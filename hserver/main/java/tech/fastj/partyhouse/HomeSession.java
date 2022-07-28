package tech.fastj.partyhouse;

import tech.fastj.network.rpc.ServerClient;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.sessions.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.ClientPosition;
import tech.fastj.partyhousecore.ClientVelocity;
import tech.fastj.partyhousecore.Commands;
import tech.fastj.partyhousecore.PositionState;
import tech.fastj.partyhousecore.SessionNames;

public class HomeSession extends Session {

    private static final Logger HomeSessionLogger = LoggerFactory.getLogger(HomeSession.class);

    private final Map<UUID, PositionState> clientGameStates;

    private final Map<UUID, Boolean> clientsReady;

    protected HomeSession(GameLobby lobby) {
        super(lobby, SessionNames.Home, new ArrayList<>());
        clientGameStates = new HashMap<>();
        clientsReady = new LinkedHashMap<>();

        setOnClientJoin(this::addNewPositionState);
        setOnClientLeave(this::removePositionState);
        addCommand(Commands.UpdateClientGameState, ClientInfo.class, ClientPosition.class, ClientVelocity.class, this::updatePositionState);

        addCommand(Commands.Ready, ClientInfo.class, this::notifyClientReady);
        addCommand(Commands.UnReady, ClientInfo.class, this::notifyClientUnReady);
    }

    public Map<UUID, PositionState> getClientGameStates() {
        return clientGameStates;
    }

    private void addNewPositionState(Session session, ServerClient client) {
        GameLobby lobby = (GameLobby) this.lobby;
        ClientInfo clientInfo = lobby.getClientInfo(client);

        PositionState newGameState = new PositionState();
        newGameState.setClientInfo(clientInfo);
        newGameState.setClientPosition(new ClientPosition(640f, 360f));
        newGameState.setClientVelocity(new ClientVelocity());

        clientGameStates.put(client.getClientId(), newGameState);
        clientsReady.put(client.getClientId(), false);

        HomeSessionLogger.info("{} to notify from session about client game state adding", getClients().size());

        for (ServerClient serverClient : getClients()) {
            try {
                HomeSessionLogger.info("sending new game state from {} to {}", client.getClientId(), serverClient.getClientId());

                serverClient.sendCommand(
                    NetworkType.TCP, CommandTarget.Client, Commands.UpdateClientGameState,
                    newGameState.getClientInfo(), newGameState.getClientPosition(), newGameState.getClientVelocity()
                );
            } catch (IOException exception) {
                HomeSessionLogger.warn("Error while trying to send client game state to {}: {}", serverClient.getClientId(), exception);
            }

            try {
                HomeSessionLogger.info("sending existing game state from {} to {}", serverClient.getClientId(), client.getClientId());

                PositionState existingGameState = clientGameStates.get(serverClient.getClientId());
                client.sendCommand(
                    NetworkType.TCP, CommandTarget.Client, Commands.UpdateClientGameState,
                    existingGameState.getClientInfo(), existingGameState.getClientPosition(), existingGameState.getClientVelocity()
                );
            } catch (IOException exception) {
                HomeSessionLogger.warn("Error while trying to send client game state to {}: {}", client.getClientId(), exception);
            }
        }
    }

    private void updatePositionState(ServerClient client, ClientInfo info, ClientPosition position, ClientVelocity velocity) {
        HomeSessionLogger.info("Telling {} clients {} has moved to: {}, {}", getClients().size(), info.clientName(), position.x(), position.y());

        PositionState positionState = clientGameStates.get(info.clientId());
        positionState.setClientInfo(info);
        positionState.setClientPosition(position);
        positionState.setClientVelocity(velocity);

        for (ServerClient serverClient : getClients()) {
            if (client.getClientId().equals(serverClient.getClientId())) {
                HomeSessionLogger.debug(
                    "Don't tell {} that {} moved",
                    clientGameStates.get(serverClient.getClientId()).getClientInfo().clientName(),
                    clientGameStates.get(client.getClientId()).getClientInfo().clientName()
                );
                continue;
            }

            try {
                HomeSessionLogger.debug(
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

    private void removePositionState(Session session, ServerClient client) {
        clientGameStates.remove(client.getClientId());
        clientsReady.remove(client.getClientId());
    }

    private void notifyClientReady(ServerClient client, ClientInfo info) {
        HomeSessionLogger.info("{} is ready to play", info.clientName());

        clientsReady.put(info.clientId(), true);

        for (ServerClient serverClient : getClients()) {
            if (client.getClientId().equals(serverClient.getClientId())) {
                continue;
            }

            try {
                HomeSessionLogger.info(
                    "Telling {} that {} is ready to play",
                    clientGameStates.get(serverClient.getClientId()).getClientInfo().clientName(),
                    clientGameStates.get(client.getClientId()).getClientInfo().clientName()
                );
                serverClient.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.Ready, info);
            } catch (IOException exception) {
                HomeSessionLogger.warn("error while trying to send {}'s readiness: {}", serverClient.getClientId(), exception.getMessage());
            }
        }

        checkClientsReadyToSwitch();
    }

    private void checkClientsReadyToSwitch() {
        if (clientsReady.size() < 2) {
            HomeSessionLogger.info("Not enough clients to consider switching, only {} in lobby.", clientsReady.values().size());
            return;
        }

        for (Boolean isReady : clientsReady.values()) {
            if (isReady == null || !isReady) {
                HomeSessionLogger.info("Not all clients are ready to being playing.");
                return;
            }
        }

        HomeSessionLogger.info("All {} clients ready to play! Switching to session \"{}\"...", getClients().size(), SessionNames.SnowballFight);

        lobby.switchCurrentSession(SessionNames.SnowballFight);
        Future<Integer> exCheck = startSessionSequence(() -> {
            for (int i = getClients().size() - 1; i >= 0; i--) {
                ServerClient client = getClients().get(i);

                HomeSessionLogger.info(
                    "Telling client {}:{} to switch scenes",
                    client.getClientId(),
                    clientGameStates.get(client.getClientId()).getClientInfo().clientName()
                );

                client.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.SwitchScene, SessionNames.SnowballFight);
            }

            TimeUnit.SECONDS.sleep(1L);

            for (int i = getClients().size() - 1; i >= 0; i--) {
                ServerClient client = getClients().get(i);

                HomeSessionLogger.info(
                    "Moving client {}:{}",
                    client.getClientId(),
                    clientGameStates.get(client.getClientId()).getClientInfo().clientName()
                );

                clientLeave(client);
                lobby.getCurrentSession().clientJoin(client);
            }

            TimeUnit.SECONDS.sleep(1L);

            ((SnowballFightSession) lobby.getCurrentSession()).startGame();

            return 0;
        });

        try {
            exCheck.get();
        } catch (InterruptedException | ExecutionException exception) {
            HomeSessionLogger.error("Unable to switch scenes: " + exception.getMessage(), exception);
            lobby.stop();
        }
    }

    private void notifyClientUnReady(ServerClient client, ClientInfo info) {
        HomeSessionLogger.info("{} is no longer to play", info.clientName());

        clientsReady.put(info.clientId(), false);

        for (ServerClient serverClient : getClients()) {
            if (client.getClientId().equals(serverClient.getClientId())) {
                continue;
            }

            try {
                HomeSessionLogger.info(
                    "Telling {} that {} is no longer ready to play",
                    clientGameStates.get(serverClient.getClientId()).getClientInfo().clientName(),
                    clientGameStates.get(client.getClientId()).getClientInfo().clientName()
                );
                serverClient.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.UnReady, info);
            } catch (IOException exception) {
                HomeSessionLogger.warn("error while trying to send {}'s un-readiness: {}", serverClient.getClientId(), exception.getMessage());
            }
        }
    }

    @Override
    public Logger getLogger() {
        return HomeSessionLogger;
    }
}
