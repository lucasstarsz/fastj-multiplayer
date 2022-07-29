package tech.fastj.partyhouse.scenes.multiplayer.home;

import tech.fastj.engine.FastJEngine;
import tech.fastj.logging.Log;
import tech.fastj.math.Pointf;
import tech.fastj.graphics.dialog.DialogConfig;
import tech.fastj.graphics.display.FastJCanvas;

import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SceneManager;

import tech.fastj.network.rpc.Client;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;

import java.awt.Color;
import java.io.IOException;
import java.net.ConnectException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import tech.fastj.gameloop.CoreLoopState;
import tech.fastj.partyhouse.Main;
import tech.fastj.partyhouse.objects.Player;
import tech.fastj.partyhouse.scripts.HomeController;
import tech.fastj.partyhouse.ui.BetterButton;
import tech.fastj.partyhouse.ui.ContentBox;
import tech.fastj.partyhouse.user.User;
import tech.fastj.partyhouse.util.Buttons;
import tech.fastj.partyhouse.util.ClientUtil;
import tech.fastj.partyhouse.util.Dialogs;
import tech.fastj.partyhouse.util.PlayerUtil;
import tech.fastj.partyhouse.util.SceneNames;
import tech.fastj.partyhouse.util.Shapes;
import tech.fastj.partyhouse.util.Tags;
import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.ClientPosition;
import tech.fastj.partyhousecore.ClientVelocity;
import tech.fastj.partyhousecore.Commands;
import tech.fastj.partyhousecore.PositionState;

public class LobbyHome extends Scene {

    private final User user;

    private ContentBox pingDisplay;

    private final Map<UUID, PositionState> otherPlayerPositionStates;
    private final Map<UUID, Player> otherPlayers;

    private PositionState playerPositionState;
    private Player player;

    private BetterButton readyUpButton;
    private boolean isReady;

    public LobbyHome() {
        super(SceneNames.HomeLobby);

        otherPlayerPositionStates = new LinkedHashMap<>();
        otherPlayers = new LinkedHashMap<>();
        user = User.getInstance();
    }

    public ContentBox getPingDisplay() {
        return pingDisplay;
    }

    @Override
    public void load(FastJCanvas canvas) {
        Log.debug(LobbyHome.class, "loading {}", getSceneName());

        ClientUtil.clientNullCheck();
        Client client = user.getClient();

        client.startKeepAlives(1L, TimeUnit.SECONDS);
        setupClientCommands();

        pingDisplay = ClientUtil.setupClientPingForDisplay(client, this);

        playerPositionState = PlayerUtil.createPositionState(user.getClientInfo(), canvas.getCanvasCenter());
        player = PlayerUtil.createPlayer(playerPositionState);
        player.addTag(Tags.LocalPlayer);

        HomeController homeController = new HomeController(250, 300, playerPositionState);
        player.addBehavior(homeController, this);

        readyUpButton = Buttons.create(this, canvas, 640f - Shapes.ButtonSize.x - 20f, 360 - (Shapes.ButtonSize.y * 2f) - 10f, "Not Ready");
        readyUpButton.setFill(Color.red.darker());
        readyUpButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            FastJEngine.runLater(() -> {
                try {
                    toggleReadyUp();
                } catch (IOException exception) {
                    if (exception instanceof ConnectException connectException) {
                        Dialogs.message(
                            DialogConfig.create()
                                .withTitle("Error while trying to notify as " + (!isReady ? "Ready" : "Not Ready"))
                                .withPrompt(connectException.getMessage())
                                .build()
                        );
                    } else {
                        if (!User.getInstance().getClient().isConnected()) {
                            ClientUtil.disconnectClient();
                        } else {
                            Main.gameCrashed("Crashed while readying up", exception);
                        }
                    }
                }
            }, CoreLoopState.LateUpdate);
        });

        drawableManager().addGameObject(player);

        Log.debug(LobbyHome.class, "loaded {}", getSceneName());
    }

    @Override
    public void unload(FastJCanvas canvas) {
        Log.debug(LobbyHome.class, "unloading {}", getSceneName());

        disableClientCommands();

        otherPlayers.clear();
        otherPlayerPositionStates.clear();
        playerPositionState = null;

        Log.debug(LobbyHome.class, "unloaded {}", getSceneName());
    }

    @Override
    public void fixedUpdate(FastJCanvas canvas) {
        if (playerPositionState.needsUpdate()) {
            try {
                playerPositionState.sendUpdate(user.getClient(), CommandTarget.Session);
            } catch (IOException exception) {
                Log.error("Unable to send game update: " + exception.getMessage(), exception);
            }
        }
    }

    @Override
    public void update(FastJCanvas canvas) {
        for (PositionState positionState : otherPlayerPositionStates.values()) {
            Player otherPlayer = otherPlayers.get(positionState.getClientInfo().clientId());

            if (otherPlayer == null) {
                Log.warn("Missing player reference for {}", positionState.getClientInfo().clientName());

                ClientUtil.disconnectClient();

                return;
            }

            positionState.updatePlayerPosition(otherPlayer);
        }

        playerPositionState.updatePlayerPosition(player);
    }

    private void toggleReadyUp() throws IOException {
        if (!isReady) {
            user.getClient().sendCommand(NetworkType.TCP, CommandTarget.Session, Commands.Ready, user.getClientInfo());
            readyUpButton.setText("Ready to Play");
            readyUpButton.setFill(Color.green.darker());
        } else {
            user.getClient().sendCommand(NetworkType.TCP, CommandTarget.Session, Commands.UnReady, user.getClientInfo());
            readyUpButton.setText("Not Ready");
            readyUpButton.setFill(Color.red.darker());
        }

        isReady = !isReady;
    }

    public void setupClientCommands() {
        Client client = user.getClient();
        Pointf center = FastJEngine.getCanvas().getCanvasCenter();

        ClientUtil.addDefault2DControlCommands(client, center, otherPlayers, otherPlayerPositionStates, this);

        client.addCommand(Commands.SwitchScene, String.class, (c, sceneName) -> {
            Log.info("Switching to scene \"{}\"", sceneName);
            FastJEngine.runLater(() -> FastJEngine.<SceneManager>getLogicManager().switchScenes(sceneName), CoreLoopState.LateUpdate);
        });
    }

    public void disableClientCommands() {
        Client client = user.getClient();

        client.addCommand(Commands.ClientJoinLobby, ClientInfo.class, (c, clientInfo) -> {});
        client.addCommand(Commands.ClientLeaveLobby, ClientInfo.class, (c, clientInfo) -> {});
        client.addCommand(Commands.UpdateClientInfo, ClientInfo.class, (c, clientInfo) -> {});
        client.addCommand(Commands.SwitchScene, String.class, (c, sceneName) -> {});

        client.addCommand(Commands.UpdateClientGameState,
            ClientInfo.class, ClientPosition.class, ClientVelocity.class,
            (c, clientInfo, clientPosition, clientVelocity) -> {}
        );
    }
}
