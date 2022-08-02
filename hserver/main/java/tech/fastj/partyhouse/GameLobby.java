package tech.fastj.partyhouse;

import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.server.Lobby;
import tech.fastj.network.rpc.server.Server;
import tech.fastj.network.rpc.server.ServerClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.Commands;
import tech.fastj.partyhousecore.PointsState;

public class GameLobby extends Lobby<Commands> {

    private static final Logger GameLobbyLogger = LoggerFactory.getLogger(GameLobby.class);

    HomeSession homeSession;
    SnowballFightSession snowballFightSession;

    private final Map<UUID, ClientInfo> clientInfoMap;
    private final Map<UUID, PointsState> totalPoints;

    public GameLobby(Server<Commands> server, String name) {
        super(server, 8, name, Commands.class);

        clientInfoMap = new HashMap<>();
        totalPoints = new HashMap<>();

        homeSession = new HomeSession(this);
        snowballFightSession = new SnowballFightSession(this);

        addSession(homeSession);
        addSession(snowballFightSession);

        setCurrentSession(homeSession);
        setHomeSessionId(homeSession.getSessionId());

        setOnReceiveNewClient(this::notifyClientJoined);
        setOnClientDisconnect(this::notifyClientLeft);
        addCommand(Commands.UpdateClientInfo, this::notifyUpdateClientInfo);
    }

    private void notifyUpdateClientInfo(ServerClient<Commands> client, ClientInfo clientInfo) {
        GameLobbyLogger.info("Updating client info of {} to {}:{}", clientInfo.clientId(), clientInfo.clientId(), clientInfo.clientName());

        replaceClientInfo(clientInfo);
        replaceClientPoints(clientInfo);

        for (var serverClient : clients) {
            if (clientInfo.clientId().equals(serverClient.getClientId())) {
                return;
            }

            try {
                GameLobbyLogger.info(
                    "Telling {}:{} that {}:{} changed its name",
                    serverClient.getClientId(), clientInfoMap.get(serverClient.getClientId()).clientName(),
                    clientInfo.clientId(), clientInfoMap.get(clientInfo.clientId()).clientName()
                );
                serverClient.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.UpdateClientInfo, clientInfo);
            } catch (IOException exception) {
                GameLobbyLogger.warn("error while trying to send lobby name to " + serverClient.getClientId(), exception);
            }
        }
    }

    private void notifyClientJoined(Lobby<Commands> lobby, ServerClient<Commands> client) {
        ClientInfo clientInfo = new ClientInfo(client.getClientId(), "Player " + (clients.size() + 1));

        replaceClientInfo(clientInfo);

        GameLobbyLogger.info("new client {}", clientInfo);
        GameLobbyLogger.info("{} to notify from lobby", clients.size());

        for (var serverClient : getClients()) {
            try {
                GameLobbyLogger.info("Notifying {} on new client {}", getClientInfo(serverClient).clientName(), clientInfo.clientName());
                serverClient.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.ClientJoinLobby, clientInfo);
            } catch (IOException exception) {
                GameLobbyLogger.warn("tried to send notification to all clients, but {}", exception.getMessage());
            }

            try {
                GameLobbyLogger.info("Notifying new client {} on existing client {}", clientInfo.clientName(), getClientInfo(serverClient).clientName());
                client.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.ClientJoinLobby, getClientInfo(serverClient));
            } catch (IOException exception) {
                GameLobbyLogger.warn("tried to send notification to newly joined client, but {}", exception.getMessage());
            }
        }
    }

    private void notifyClientLeft(Lobby<Commands> lobby, ServerClient<Commands> client) {
        ClientInfo clientInfo = getClientInfo(client);

        if (clientInfo == null) {
            clientInfo = new ClientInfo(client.getClientId(), client.getClientId().toString());
        }

        GameLobbyLogger.info("client {} leaving", clientInfo.clientName());
        GameLobbyLogger.info("{} to notify from lobby", clients.size());

        for (var serverClient : getClients()) {
            GameLobbyLogger.info("telling {} that {} is leaving", getClientInfo(serverClient).clientName(), clientInfo.clientName());

            try {
                serverClient.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.ClientLeaveLobby, clientInfo);
            } catch (IOException exception) {
                GameLobbyLogger.warn("tried to send notification to all clients, but {}", exception.getMessage());
            }
        }

        clientInfoMap.remove(client.getClientId());
        totalPoints.remove(client.getClientId());
    }

    @Override
    public Logger getLogger() {
        return GameLobbyLogger;
    }

    public ClientInfo getClientInfo(ServerClient<Commands> client) {
        return clientInfoMap.get(client.getClientId());
    }

    public void replaceClientInfo(ClientInfo clientInfo) {
        ClientInfo replacedInfo = clientInfoMap.put(clientInfo.clientId(), clientInfo);
        GameLobbyLogger.info("updated client info: set up {} replacing {}", clientInfo, replacedInfo);
    }

    private void replaceClientPoints(ClientInfo clientInfo) {
        PointsState pointsState = new PointsState();
        pointsState.setClientInfo(clientInfo);

        PointsState replacedPoints = totalPoints.put(clientInfo.clientId(), pointsState);
        GameLobbyLogger.info("updated client points: set up {} replacing {}", pointsState, replacedPoints);
    }

    public void replaceTotalPoints(Map<UUID, PointsState> clientPoints) {
        for (Map.Entry<UUID, PointsState> pointsEntry : clientPoints.entrySet()) {
            totalPoints.get(pointsEntry.getKey()).modifyPoints(pointsEntry.getValue().getPoints());
            pointsEntry.getValue().resetPoints();
        }
    }

    public Map<UUID, PointsState> getTotalPoints() {
        return totalPoints;
    }
}
