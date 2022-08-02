package tech.fastj.partyhouse.util;

import tech.fastj.engine.FastJEngine;
import tech.fastj.logging.Log;
import tech.fastj.math.Pointf;
import tech.fastj.graphics.dialog.DialogConfig;

import tech.fastj.systems.control.GameHandler;
import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SceneManager;

import tech.fastj.network.rpc.local.LocalClient;

import javax.swing.SwingUtilities;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import tech.fastj.gameloop.CoreLoopState;
import tech.fastj.partyhouse.Main;
import tech.fastj.partyhouse.objects.Player;
import tech.fastj.partyhouse.scenes.multiplayer.home.LobbyHome;
import tech.fastj.partyhouse.scenes.multiplayer.snowball.SnowballFight;
import tech.fastj.partyhouse.ui.ContentBox;
import tech.fastj.partyhouse.user.User;
import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.ClientPosition;
import tech.fastj.partyhousecore.ClientVelocity;
import tech.fastj.partyhousecore.Commands;
import tech.fastj.partyhousecore.PositionState;

public class ClientUtil {

    public static boolean clientNullCheck() {
        if (User.getInstance().getClient() == null) {
            Main.gameCrashed("Client has not been initialized, can't proceed with the game.", new IllegalStateException());
            return true;
        }

        return false;
    }

    public static void disconnectClient() {
        SwingUtilities.invokeLater(() -> {
            Dialogs.message(DialogConfig.create()
                .withParentComponent(FastJEngine.getDisplay().getWindow())
                .withTitle("Disconnected")
                .withPrompt("You were disconnected from the server. You will now return to the main menu.")
                .build()
            );

            FastJEngine.runLater(() -> {
                FastJEngine.<SceneManager>getLogicManager().switchScenes(SceneNames.MainMenu);

                User.getInstance().setClientInfo(null);

                if (User.getInstance().getClient() != null) {
                    User.getInstance().setClient(null);
                }
            }, CoreLoopState.LateUpdate);
        });
    }

    public static void addDefault2DControlCommands(LocalClient<Commands> client, Pointf center, Map<UUID, Player> otherPlayers,
                                                   Map<UUID, PositionState> otherPlayerPositionStates, GameHandler gameHandler) {
        client.addCommand(Commands.ClientJoinLobby, (ClientInfo clientInfo) -> {
            Log.info("{} joined.", clientInfo.clientName());

            PositionState positionState = PlayerUtil.createOtherPositionState(clientInfo, center, otherPlayerPositionStates);
            Player otherPlayer = PlayerUtil.createOtherPlayer(positionState, otherPlayers, gameHandler);

            otherPlayers.put(clientInfo.clientId(), otherPlayer);
            otherPlayerPositionStates.put(clientInfo.clientId(), positionState);
        });

        client.addCommand(Commands.ClientLeaveLobby, (ClientInfo clientInfo) -> {
            Log.info("{} left.", clientInfo.clientName());

            FastJEngine.runLater(() -> {
                otherPlayerPositionStates.remove(clientInfo.clientId());
                Player otherPlayer = otherPlayers.remove(clientInfo.clientId());

                if (otherPlayer != null) {
                    otherPlayer.destroy(gameHandler);
                }
            }, CoreLoopState.LateUpdate);
        });

        client.addCommand(Commands.UpdateClientInfo, (ClientInfo clientInfo) -> {
            Log.info("{} updated their client info.", clientInfo.clientName());

            PositionState positionState = otherPlayerPositionStates.get(clientInfo.clientId());
            Player otherPlayer = otherPlayers.get(clientInfo.clientId());

            if (positionState == null) {
                positionState = PlayerUtil.createOtherPositionState(clientInfo, center, otherPlayerPositionStates);
            }

            if (otherPlayer == null) {
                otherPlayer = PlayerUtil.createOtherPlayer(positionState, otherPlayers, gameHandler);
            }

            positionState.setClientInfo(clientInfo);
            otherPlayer.setPlayerName(clientInfo.clientName());
        });

        client.addCommand(Commands.UpdateClientGameState,
            (ClientInfo clientInfo, ClientPosition clientPosition, ClientVelocity clientVelocity) -> {
                PositionState positionState = otherPlayerPositionStates.get(clientInfo.clientId());
                Player otherPlayer = otherPlayers.get(clientInfo.clientId());

                if (positionState == null) {
                    positionState = PlayerUtil.createOtherPositionState(clientInfo, center, otherPlayerPositionStates);
                }

                if (otherPlayer == null) {
                    otherPlayer = PlayerUtil.createOtherPlayer(positionState, otherPlayers, gameHandler);
                }

                positionState.setClientInfo(clientInfo);
                positionState.setClientPosition(clientPosition);
                positionState.setClientVelocity(clientVelocity);

                otherPlayer.setPlayerName(clientInfo.clientName());
            }
        );
    }

    public static ContentBox setupClientPingForDisplay(LocalClient<Commands> client, GameHandler gameHandler) {
        ContentBox pingDisplay = new ContentBox(gameHandler, "Ping", "???ms");

        pingDisplay.translate(new Pointf(25f));
        pingDisplay.getStatDisplay().setFill(Colors.Snowy);
        pingDisplay.getStatDisplay().setFont(Fonts.StatTextFont);

        client.onPingReceived(ping -> {
            double pingMillis = TimeUnit.MILLISECONDS.convert(ping, TimeUnit.NANOSECONDS);
            Scene scene = FastJEngine.<SceneManager>getLogicManager().getCurrentScene();
            ContentBox display = null;

            if (scene instanceof SnowballFight lobby) {
                display = lobby.getPingDisplay();
            } else if (scene instanceof LobbyHome lobby) {
                display = lobby.getPingDisplay();
            }

            if (display == null || display.isDestroyed()) {
                return;
            }

            display.setContent(pingMillis + "ms");
        });

        client.startPings(100L, TimeUnit.MILLISECONDS);

        return pingDisplay;
    }
}
