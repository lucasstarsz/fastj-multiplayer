package tech.fastj.partyhouse;

import tech.fastj.network.rpc.ServerClient;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.sessions.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.fastj.partyhousecore.ClientGameState;
import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.ClientPosition;
import tech.fastj.partyhousecore.ClientVelocity;
import tech.fastj.partyhousecore.Commands;

public class HomeSession extends Session {

    private static final Logger HomeSessionLogger = LoggerFactory.getLogger(HomeSession.class);
    public static final String Name = "Home";

    private final Map<UUID, ClientGameState> clientGameStates;

    protected HomeSession(GameLobby lobby) {
        super(lobby, Name, new ArrayList<>());
        clientGameStates = new HashMap<>();

        setOnReceiveNewClient(this::addClientGameState);
        setOnClientDisconnect(this::removeClientGameState);
        addCommand(Commands.UpdateClientGameState, ClientInfo.class, ClientPosition.class, ClientVelocity.class,
                (client, info, position, velocity) -> {
                    HomeSessionLogger.info("{} has moved to: {}, {}", info.clientName(), position.x(), position.y());

                    ClientGameState clientGameState = clientGameStates.get(info.clientId());
                    clientGameState.setClientInfo(info);
                    clientGameState.setClientPosition(position);
                    clientGameState.setClientVelocity(velocity);

                    for (ServerClient serverClient : getClients()) {
                        if (client.getClientId().equals(serverClient.getClientId())) {
                            HomeSessionLogger.info(
                                    "Don't tell {} that {} moved",
                                    clientGameStates.get(serverClient.getClientId()).getClientInfo().clientName(),
                                    clientGameStates.get(client.getClientId()).getClientInfo().clientName()
                            );
                            continue;
                        }

                        try {
                            HomeSessionLogger.info(
                                    "Telling {} that {} moved",
                                    clientGameStates.get(serverClient.getClientId()).getClientInfo().clientName(),
                                    clientGameStates.get(client.getClientId()).getClientInfo().clientName()
                            );
                            serverClient.sendCommand(NetworkType.UDP, CommandTarget.Client, Commands.UpdateClientGameState, info, position, velocity);
                        } catch (IOException exception) {
                            HomeSessionLogger.warn("error while trying to send {}'s game state update: {}", serverClient.getClientId(), exception.getMessage());
                        }
                    }
                }
        );
    }

    public Map<UUID, ClientGameState> getClientGameStates() {
        return clientGameStates;
    }

    private void addClientGameState(Session session, ServerClient client) {
        GameLobby lobby = (GameLobby) this.lobby;
        ClientInfo clientInfo = lobby.getClientInfo(client);

        ClientGameState newGameState = new ClientGameState();
        newGameState.setClientInfo(clientInfo);
        newGameState.setClientPosition(new ClientPosition(640f, 360f));
        newGameState.setClientVelocity(new ClientVelocity());

        clientGameStates.put(client.getClientId(), newGameState);

        System.out.println(getClients().size() + " to notify from session about client game state adding");

        for (ServerClient serverClient : getClients()) {
            try {
                System.out.println("sending new game state from " + client.getClientId() + " to " + serverClient.getClientId());

                serverClient.sendCommand(
                        NetworkType.TCP, CommandTarget.Client, Commands.UpdateClientGameState,
                        newGameState.getClientInfo(), newGameState.getClientPosition(), newGameState.getClientVelocity()
                );
            } catch (IOException exception) {
                HomeSessionLogger.warn("error while trying to send client game state to " + serverClient.getClientId(), exception);
            }

            try {
                ClientGameState existingGameState = clientGameStates.get(serverClient.getClientId());

                System.out.println("sending existing game state from " + serverClient.getClientId() + " to " + client.getClientId());

                client.sendCommand(
                        NetworkType.TCP, CommandTarget.Client, Commands.UpdateClientGameState,
                        existingGameState.getClientInfo(), existingGameState.getClientPosition(), existingGameState.getClientVelocity()
                );
            } catch (IOException exception) {
                HomeSessionLogger.warn("error while trying to send client game state to " + serverClient.getClientId(), exception);
            }
        }
    }

    private void removeClientGameState(Session session, ServerClient client) {
        clientGameStates.remove(client.getClientId());
    }

    @Override
    public Logger getLogger() {
        return HomeSessionLogger;
    }
}
