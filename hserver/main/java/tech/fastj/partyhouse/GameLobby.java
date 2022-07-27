package tech.fastj.partyhouse;

import tech.fastj.network.rpc.Server;
import tech.fastj.network.rpc.ServerClient;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.sessions.Lobby;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.Commands;
import tech.fastj.partyhousecore.Messages;

public class GameLobby extends Lobby {

    private static final Logger GameLobbyLogger = LoggerFactory.getLogger(GameLobby.class);

    HomeSession homeSession;

    private final Map<UUID, ClientInfo> clientInfoMap;

    public GameLobby(Server server, String name) {
        super(server, 8, name);

        clientInfoMap = new HashMap<>();
        Messages.updateSerializer(serializer);
        setOnReceiveNewClient(this::notifyClientJoined);
        setOnClientDisconnect(this::notifyClientLeft);

        homeSession = new HomeSession(this);
        addSession(homeSession);
        addCommand(Commands.UpdateClientInfo, ClientInfo.class, (client, clientInfo) -> {
            GameLobbyLogger.info("Updating client info of {} to {}:{}", client.getClientId(), clientInfo.clientId(), clientInfo.clientName());
            updateClientInfo(clientInfo);

            for (ServerClient serverClient : clients) {
                if (client.getClientId().equals(serverClient.getClientId())) {
                    return;
                }

                try {
                    GameLobbyLogger.info(
                        "Telling {}:{} that {}:{} changed its name",
                        serverClient.getClientId(), clientInfoMap.get(serverClient.getClientId()).clientName(),
                        client.getClientId(), clientInfoMap.get(client.getClientId()).clientName()
                    );
                    serverClient.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.UpdateClientInfo, clientInfo);
                } catch (IOException exception) {
                    GameLobbyLogger.warn("error while trying to send lobby name to " + serverClient.getClientId(), exception);
                }
            }
        });
    }

    private void notifyClientJoined(Lobby lobby, ServerClient client) {
        Messages.updateSerializer(client.getSerializer());

        ClientInfo clientInfo = new ClientInfo(client.getClientId(), "Player " + (clients.size() + 1));
        updateClientInfo(clientInfo);

        GameLobbyLogger.info("new client {}", clientInfo);
        GameLobbyLogger.info("{} to notify from lobby", clients.size());

        for (ServerClient serverClient : getClients()) {
            try {
                GameLobbyLogger.info("Notifying {} on new client {}", getClientInfo(serverClient).clientName(), clientInfo.clientName());
                serverClient.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.ClientJoined, clientInfo);
            } catch (IOException exception) {
                GameLobbyLogger.warn("tried to send notification to all clients, but {}", exception.getMessage());
            }

            try {
                GameLobbyLogger.info("Notifying new client {} on existing client {}", clientInfo.clientName(), getClientInfo(serverClient).clientName());
                client.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.ClientJoined, getClientInfo(serverClient));
            } catch (IOException exception) {
                GameLobbyLogger.warn("tried to send notification to newly joined client, but {}", exception.getMessage());
            }
        }
    }

    private void notifyClientLeft(Lobby lobby, ServerClient client) {
        GameLobbyLogger.info("client {} leaving", getClientInfo(client).clientName());
        GameLobbyLogger.info("{} to notify from lobby", clients.size());

        for (ServerClient serverClient : getClients()) {
            GameLobbyLogger.info("telling {} that {} is leaving", getClientInfo(serverClient).clientName(), getClientInfo(client).clientName());
            try {
                serverClient.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.ClientLeft, getClientInfo(client));
            } catch (IOException exception) {
                GameLobbyLogger.warn("tried to send notification to all clients, but {}", exception.getMessage());
            }
        }

        removeClientInfo(client);
    }

    @Override
    public Logger getLogger() {
        return GameLobbyLogger;
    }

    public ClientInfo getClientInfo(ServerClient client) {
        return clientInfoMap.get(client.getClientId());
    }

    public void updateClientInfo(ClientInfo clientInfo) {
        ClientInfo replacedInfo = clientInfoMap.put(clientInfo.clientId(), clientInfo);
        GameLobbyLogger.info("updated client info: set up {} replacing {}", clientInfo, replacedInfo);
    }

    public void removeClientInfo(ServerClient client) {
        clientInfoMap.remove(client.getClientId());
    }
}
