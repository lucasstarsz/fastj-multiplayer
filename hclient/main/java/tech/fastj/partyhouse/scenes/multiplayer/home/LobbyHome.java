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

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.io.IOException;
import java.net.ConnectException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import tech.fastj.partyhouse.Main;
import tech.fastj.partyhouse.objects.Player;
import tech.fastj.partyhouse.scripts.HomeController;
import tech.fastj.partyhouse.ui.BetterButton;
import tech.fastj.partyhouse.ui.ContentBox;
import tech.fastj.partyhouse.user.User;
import tech.fastj.partyhouse.util.Buttons;
import tech.fastj.partyhouse.util.Colors;
import tech.fastj.partyhouse.util.Dialogs;
import tech.fastj.partyhouse.util.Fonts;
import tech.fastj.partyhouse.util.SceneNames;
import tech.fastj.partyhouse.util.Shapes;
import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.ClientPosition;
import tech.fastj.partyhousecore.ClientVelocity;
import tech.fastj.partyhousecore.Commands;
import tech.fastj.partyhousecore.PositionState;

public class LobbyHome extends Scene {

    private final User user;

    private ContentBox pingDisplay;

    private final Map<UUID, PositionState> otherGameStates;
    private final Map<UUID, Player> otherPlayers;

    private PositionState playerGameState;
    private Player player;
    private HomeController homeController;

    private BetterButton readyUpButton;
    private boolean isReady;

    public LobbyHome() {
        super(SceneNames.HomeLobby);

        otherGameStates = new LinkedHashMap<>();
        otherPlayers = new LinkedHashMap<>();
        user = User.getInstance();
    }

    @Override
    public void load(FastJCanvas canvas) {
        Log.debug(LobbyHome.class, "loading {}", getSceneName());

        Client client = user.getClient();

        if (client == null) {
            Main.gameCrashed("Client has not been initialized, can't proceed with the game.", new IllegalStateException());
            return;
        }

        pingDisplay = new ContentBox(this, "Ping", "???ms");
        pingDisplay.translate(new Pointf(25f));
        pingDisplay.getStatDisplay().setFill(Colors.Snowy);
        pingDisplay.getStatDisplay().setFont(Fonts.StatTextFont);

        client.onPingReceived(ping -> {
            double pingMillis = TimeUnit.MILLISECONDS.convert(ping, TimeUnit.NANOSECONDS);
            Scene scene = FastJEngine.<SceneManager>getLogicManager().getCurrentScene();

            if (scene instanceof LobbyHome lobby && lobby.pingDisplay != null) {
                lobby.pingDisplay.setContent(pingMillis + "ms");
            }
        });

        client.startPings(100L, TimeUnit.MILLISECONDS);
        client.startKeepAlives(1L, TimeUnit.SECONDS);

        Pointf center = canvas.getCanvasCenter();
        playerGameState = new PositionState();
        playerGameState.setClientInfo(user.getClientInfo());
        playerGameState.setClientPosition(new ClientPosition(center.x, center.y));
        playerGameState.setClientVelocity(new ClientVelocity());

        player = new Player(playerGameState.getClientInfo().clientName());

        homeController = new HomeController(250, 300, playerGameState);
        player.addBehavior(homeController, this);

        drawableManager.addGameObject(player);

        readyUpButton = Buttons.create(this, canvas, 640f - Shapes.ButtonSize.x - 20f, 360 - (Shapes.ButtonSize.y * 2f) - 10f, "Not Ready");
        readyUpButton.setFill(Color.red.darker());
        readyUpButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            FastJEngine.runAfterRender(() -> {
                try {
                    toggleReadyUp();
                } catch (IOException exception) {
                    isReady = !isReady;

                    if (exception instanceof ConnectException connectException) {
                        Dialogs.message(
                            DialogConfig.create()
                                .withTitle("Error while trying to notify as " + (!isReady ? "Ready" : "Not Ready"))
                                .withPrompt(connectException.getMessage())
                                .build()
                        );
                    } else {
                        if (!User.getInstance().getClient().isConnected()) {
                            disconnectClient();
                        } else {
                            Main.gameCrashed("Crashed while readying up", exception);
                        }
                    }
                }
            });
        });

        setupClientCommands();

        Log.debug(LobbyHome.class, "loaded {}", getSceneName());
    }

    private void disconnectClient() {
        SwingUtilities.invokeLater(() -> {
            Dialogs.message(DialogConfig.create()
                .withParentComponent(FastJEngine.getDisplay().getWindow())
                .withTitle("Disconnected")
                .withPrompt("You were disconnected from the server. You will now return to the main menu.")
                .build()
            );

            FastJEngine.runAfterRender(() -> {
                FastJEngine.<SceneManager>getLogicManager().switchScenes(SceneNames.MainMenu);
                User.getInstance().setClientInfo(null);
                User.getInstance().setClient(null);
            });
        });
    }

    @Override
    public void unload(FastJCanvas canvas) {
        Log.debug(LobbyHome.class, "unloading {}", getSceneName());

        disableClientCommands();

        if (player != null) {
            player.destroy(this);
            player = null;
        }

        if (otherPlayers != null) {
            for (Player otherPlayer : otherPlayers.values()) {
                if (otherPlayer != null) {
                    otherPlayer.destroy(this);
                }
            }

            otherPlayers.clear();
        }

        if (pingDisplay != null) {
            pingDisplay.destroy(this);
            pingDisplay = null;
        }

        if (homeController != null) {
            homeController.destroy();
            homeController = null;
        }

        if (readyUpButton != null) {
            readyUpButton.destroy(this);
            readyUpButton = null;
        }

        if (otherGameStates != null) {
            otherGameStates.clear();
        }

        playerGameState = null;

        setInitialized(false);
        Log.debug(LobbyHome.class, "unloaded {}", getSceneName());
    }

    @Override
    public void fixedUpdate(FastJCanvas canvas) {
        if (playerGameState.needsUpdate()) {
            try {
                playerGameState.sendUpdate(user.getClient(), CommandTarget.Session);
            } catch (IOException exception) {
                Log.error("Unable to send game update: " + exception.getMessage(), exception);
            }
        }
    }

    @Override
    public void update(FastJCanvas canvas) {
        for (PositionState positionState : otherGameStates.values()) {
            Player otherPlayer = otherPlayers.get(positionState.getClientInfo().clientId());

            if (otherPlayer == null) {
                Log.warn("Missing player reference for {}", positionState.getClientInfo().clientName());


                if (FastJEngine.isRunning()) {
                    FastJEngine.closeGame();
                }

                return;
            }

            positionState.updatePlayerPosition(otherPlayer);
        }

        playerGameState.updatePlayerPosition(player);
    }

    private void toggleReadyUp() throws IOException {
        isReady = !isReady;

        if (isReady) {
            user.getClient().sendCommand(NetworkType.TCP, CommandTarget.Session, Commands.Ready, user.getClientInfo());
            readyUpButton.setText("Ready to Play");
            readyUpButton.setFill(Color.green.darker());
        } else {
            user.getClient().sendCommand(NetworkType.TCP, CommandTarget.Session, Commands.UnReady, user.getClientInfo());
            readyUpButton.setText("Not Ready");
            readyUpButton.setFill(Color.red.darker());
        }
    }

    public void setupClientCommands() {
        Client client = user.getClient();
        Pointf center = FastJEngine.getCanvas().getCanvasCenter();

        client.addCommand(Commands.ClientJoinLobby, ClientInfo.class, (c, clientInfo) -> {
            Log.info("{} joined.", clientInfo.clientName());

            PositionState positionState = createGameState(clientInfo, center);
            Player otherPlayer = createPlayer(positionState);

            drawableManager.addGameObject(otherPlayer);
            otherPlayers.put(clientInfo.clientId(), otherPlayer);
            otherGameStates.put(clientInfo.clientId(), positionState);
        });

        client.addCommand(Commands.ClientLeaveLobby, ClientInfo.class, (c, clientInfo) -> {
            Log.info("{} left.", clientInfo.clientName());

            FastJEngine.runAfterUpdate(() -> {
                otherGameStates.remove(clientInfo.clientId());
                Player otherPlayer = otherPlayers.remove(clientInfo.clientId());

                if (otherPlayer != null) {
                    otherPlayer.destroy(this);
                }
            });
        });

        client.addCommand(Commands.UpdateClientInfo, ClientInfo.class, (c, clientInfo) -> {
            Log.info("{} updated their client info.", clientInfo.clientName());

            PositionState positionState = otherGameStates.get(clientInfo.clientId());
            Player otherPlayer = otherPlayers.get(clientInfo.clientId());

            if (positionState == null) {
                positionState = createOtherGameState(clientInfo, center);
            }

            if (otherPlayer == null) {
                otherPlayer = createOtherPlayer(positionState);
            }

            positionState.setClientInfo(clientInfo);
            otherPlayer.setPlayerName(clientInfo.clientName());
        });

        client.addCommand(Commands.UpdateClientGameState,
            ClientInfo.class, ClientPosition.class, ClientVelocity.class,
            (c, clientInfo, clientPosition, clientVelocity) -> {
                Log.info("{} moved: {}, {}", clientInfo.clientName(), clientPosition.x(), clientPosition.y());

                PositionState positionState = otherGameStates.get(clientInfo.clientId());
                Player otherPlayer = otherPlayers.get(clientInfo.clientId());

                if (positionState == null) {
                    positionState = createOtherGameState(clientInfo, center);
                }

                if (otherPlayer == null) {
                    otherPlayer = createOtherPlayer(positionState);
                }

                positionState.setClientInfo(clientInfo);
                positionState.setClientPosition(clientPosition);
                positionState.setClientVelocity(clientVelocity);

                otherPlayer.setPlayerName(clientInfo.clientName());
            }
        );

        client.addCommand(Commands.SwitchScene, String.class, (c, sceneName) -> {
            Log.info("Switching to scene \"{}\"", sceneName);
            FastJEngine.runAfterRender(() -> FastJEngine.<SceneManager>getLogicManager().switchScenes(sceneName));
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

    private Player createPlayer(PositionState positionState) {
        Log.info("creating new player instance for {}", positionState.getClientInfo().clientName());
        return new Player(positionState.getClientInfo().clientName());
    }

    private PositionState createGameState(ClientInfo clientInfo, Pointf center) {
        Log.info("creating new client info for {}", clientInfo.clientName());

        PositionState gameState = new PositionState();

        gameState.setClientInfo(clientInfo);
        gameState.setClientPosition(new ClientPosition(center.x, center.y));
        gameState.setClientVelocity(new ClientVelocity());

        return gameState;
    }

    private PositionState createOtherGameState(ClientInfo clientInfo, Pointf center) {
        PositionState positionState = createGameState(clientInfo, center);
        otherGameStates.put(clientInfo.clientId(), positionState);

        return positionState;
    }

    private Player createOtherPlayer(PositionState positionState) {
        Player otherPlayer = createPlayer(positionState);
        Player replaced = otherPlayers.put(positionState.getClientInfo().clientId(), otherPlayer);

        if (replaced != null) {
            replaced.destroy(this);
        }

        drawableManager.addGameObject(otherPlayer);

        return otherPlayer;
    }
}
