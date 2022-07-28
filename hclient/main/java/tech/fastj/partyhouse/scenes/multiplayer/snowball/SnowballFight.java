package tech.fastj.partyhouse.scenes.multiplayer.snowball;

import tech.fastj.engine.FastJEngine;
import tech.fastj.logging.Log;
import tech.fastj.math.Pointf;
import tech.fastj.graphics.dialog.DialogConfig;
import tech.fastj.graphics.display.FastJCanvas;

import tech.fastj.systems.collections.Pair;
import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SceneManager;

import tech.fastj.network.rpc.Client;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import tech.fastj.partyhouse.Main;
import tech.fastj.partyhouse.objects.Player;
import tech.fastj.partyhouse.objects.Snowball;
import tech.fastj.partyhouse.scenes.multiplayer.home.LobbyHome;
import tech.fastj.partyhouse.scripts.HomeController;
import tech.fastj.partyhouse.scripts.SnowballController;
import tech.fastj.partyhouse.ui.ContentBox;
import tech.fastj.partyhouse.ui.PercentageBox;
import tech.fastj.partyhouse.ui.ResultMenu;
import tech.fastj.partyhouse.ui.StatusBox;
import tech.fastj.partyhouse.user.User;
import tech.fastj.partyhouse.util.Colors;
import tech.fastj.partyhouse.util.Dialogs;
import tech.fastj.partyhouse.util.Fonts;
import tech.fastj.partyhouse.util.SceneNames;
import tech.fastj.partyhouse.util.Tags;
import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.ClientPoints;
import tech.fastj.partyhousecore.ClientPosition;
import tech.fastj.partyhousecore.ClientVelocity;
import tech.fastj.partyhousecore.Commands;
import tech.fastj.partyhousecore.PositionState;
import tech.fastj.partyhousecore.SnowballInfo;

public class SnowballFight extends Scene {

    private final User user;

    private ContentBox pingDisplay;

    private final Map<UUID, PositionState> otherGameStates;
    private final Map<UUID, Player> otherPlayers;
    private final Map<Pair<UUID, UUID>, Snowball> snowballs;

    private PositionState playerGameState;
    private Player player;
    private HomeController homeController;
    private SnowballController snowballController;

    private PercentageBox<Integer> snowballStatus;
    private StatusBox snowballThrowStatus;
    private StatusBox snowballMakeStatus;

    private ResultMenu resultMenu;

    public SnowballFight() {
        super(SceneNames.SnowballFight);

        otherGameStates = new LinkedHashMap<>();
        otherPlayers = new LinkedHashMap<>();
        user = User.getInstance();
        snowballs = new HashMap<>();
    }

    public boolean isPlayerDead() {
        return playerGameState.isPlayerDead();
    }

    @Override
    public void load(FastJCanvas canvas) {
        Log.debug(SnowballFight.class, "loading {}", getSceneName());

        drawableManager.clearAllLists();

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

            if (scene instanceof SnowballFight lobby && lobby.pingDisplay != null) {
                lobby.pingDisplay.setContent(pingMillis + "ms");
            }
        });

        client.startPings(100L, TimeUnit.MILLISECONDS);

        setupClientCommands();

        Pointf center = canvas.getCanvasCenter();
        playerGameState = new PositionState();
        playerGameState.setClientInfo(user.getClientInfo());
        playerGameState.setClientPosition(new ClientPosition(center.x, center.y));
        playerGameState.setClientVelocity(new ClientVelocity());

        player = new Player(playerGameState.getClientInfo().clientName());
        player.addTag(Tags.LocalPlayer);

        homeController = new HomeController(250, 300, playerGameState);
        player.addBehavior(homeController, this);

        snowballController = new SnowballController(this);
        player.addBehavior(snowballController, this);

        snowballStatus = new PercentageBox<>(this, 0, SnowballController.MaxSnowballsCarried, "Snowballs: ");
        snowballStatus.getStatDisplay().setFont(Fonts.StatTextFont);
        snowballStatus.translate(Pointf.down().multiply(90f));
        drawableManager.addUIElement(snowballStatus);

        snowballMakeStatus = new StatusBox(this, "Make Snowball", false);
        snowballMakeStatus.getStatDisplay().setFont(Fonts.SmallStatTextFontPlain);
        snowballMakeStatus.translate(Pointf.down().multiply(115f));
        drawableManager.addUIElement(snowballMakeStatus);

        snowballThrowStatus = new StatusBox(this, "Throw Snowball", false);
        snowballThrowStatus.getStatDisplay().setFont(Fonts.SmallStatTextFontPlain);
        snowballThrowStatus.translate(Pointf.down().multiply(140f));
        drawableManager.addUIElement(snowballThrowStatus);

        drawableManager.addGameObject(player);

        Log.debug(SnowballFight.class, "loaded {}", getSceneName());
    }

    @Override
    public void unload(FastJCanvas canvas) {
        Log.debug(SnowballFight.class, "unloading {}", getSceneName());

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

        if (snowballController != null) {
            snowballController.destroy();
            snowballController = null;
        }

        if (snowballStatus != null) {
            snowballStatus.destroy(this);
            snowballStatus = null;
        }

        if (snowballMakeStatus != null) {
            snowballMakeStatus.destroy(this);
            snowballMakeStatus = null;
        }

        if (snowballThrowStatus != null) {
            snowballThrowStatus.destroy(this);
            snowballThrowStatus = null;
        }

        if (resultMenu != null) {
            resultMenu.destroy(this);
            resultMenu = null;
        }

        setInitialized(false);
        Log.debug(SnowballFight.class, "unloaded {}", getSceneName());
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
            if (positionState.isPlayerDead()) {
                return;
            }

            Player otherPlayer = otherPlayers.get(positionState.getClientInfo().clientId());

            if (otherPlayer == null) {
                Log.warn("Null player {}" + positionState.getClientInfo());
                return;
            }

            positionState.updatePlayerPosition(otherPlayer);
        }

        playerGameState.updatePlayerPosition(player);
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
                    drawableManager.removeGameObject(otherPlayer);
                    otherPlayer.destroy(this);
                }
            });
        });

        client.addCommand(Commands.UpdateClientInfo, ClientInfo.class, (c, clientInfo) -> {
            Log.info("{} updated their client info.", clientInfo.clientName());

            PositionState positionState = otherGameStates.get(clientInfo.clientId());
            Player otherPlayer = otherPlayers.get(clientInfo.clientId());

            if (positionState == null) {
                Log.info("Constructing new game state for {}", clientInfo.clientName());
                positionState = createGameState(clientInfo, center);
                otherGameStates.put(clientInfo.clientId(), positionState);
            }

            if (otherPlayer == null) {
                Log.info("Constructing new player model for {}", clientInfo.clientName());
                otherPlayer = createPlayer(positionState);
                drawableManager.addGameObject(otherPlayer);
                otherPlayers.put(clientInfo.clientId(), otherPlayer);
            }

            positionState.setClientInfo(clientInfo);
            otherPlayer.setPlayerName(clientInfo.clientName());
        });

        client.addCommand(Commands.UpdateClientGameState,
            ClientInfo.class, ClientPosition.class, ClientVelocity.class,
            (c, clientInfo, clientPosition, clientVelocity) -> {
                Log.debug("{} moved: {}, {}", clientInfo.clientName(), clientPosition.x(), clientPosition.y());

                PositionState positionState = otherGameStates.get(clientInfo.clientId());
                Player otherPlayer = otherPlayers.get(clientInfo.clientId());

                if (positionState == null) {
                    Log.info("Constructing new game state for {}", clientInfo.clientName());
                    positionState = createGameState(clientInfo, center);
                    otherGameStates.put(clientInfo.clientId(), positionState);
                }

                if (otherPlayer == null) {
                    Log.info("Constructing new player model for {}", clientInfo.clientName());
                    otherPlayer = createPlayer(positionState);
                    drawableManager.addGameObject(otherPlayer);
                    otherPlayers.put(clientInfo.clientId(), otherPlayer);
                }

                positionState.setClientInfo(clientInfo);
                positionState.setClientPosition(clientPosition);
                positionState.setClientVelocity(clientVelocity);

                otherPlayer.setPlayerName(clientInfo.clientName());
            }
        );

        client.addCommand(Commands.SnowballThrow, SnowballInfo.class, (c, snowballInfo) -> FastJEngine.runAfterRender(() -> {
            Snowball snowball = new Snowball(snowballInfo, this);
            drawableManager.addGameObject(snowball);
            System.out.println(snowball.isDestroyed());
            System.out.println(snowball.shouldRender());
            System.out.println(snowball.getCenter());
            System.out.println(drawableManager.getGameObjects().get(snowball.getID()));

            Snowball replaced = snowballs.put(Pair.of(snowballInfo.clientInfo().clientId(), snowballInfo.snowballId()), snowball);

            if (replaced != null) {
                System.out.println("replacing " + replaced);
                replaced.destroy(this);
            }

            Log.info("{} threw a snowball.", snowballInfo.clientInfo().clientName());
        }));

        client.addCommand(Commands.SnowballHit, ClientInfo.class, SnowballInfo.class, (c, playerHit, snowballInfo) -> FastJEngine.runAfterRender(() -> {
            Log.info("{} was incapacitated by {}'s snowball.", playerHit.clientName(), snowballInfo.clientInfo().clientName());

            otherGameStates.get(snowballInfo.clientInfo().clientId()).setPlayerDead(true);
            Snowball removedSnowball = removeSnowball(snowballInfo);

            if (removedSnowball != null) {
                removedSnowball.destroy(this);
            }
        }));

        client.addCommand(Commands.GameFinished, ClientInfo.class, (c, winnerInfo) -> FastJEngine.runAfterRender(() -> {
            Log.info("{} won!", winnerInfo);
            playerGameState.setPlayerDead(true);
            resultMenu = new ResultMenu(this, winnerInfo);
        }));

        client.addCommand(Commands.GameResults, ClientPoints.class, (c, clientPoints) -> {
            if (resultMenu != null) {
                System.out.println("ok but actually");
            } else {
                System.out.println("or maybe not");
            }

            FastJEngine.runAfterRender(() -> resultMenu.addPointsResults(clientPoints, this, "Returning to lobby"));
        });

        client.addCommand(Commands.SwitchScene, String.class, (c, sceneName) -> {
            Log.info("Switching to scene \"{}\"", sceneName);
            FastJEngine.runAfterUpdate(() -> {
                FastJEngine.<SceneManager>getLogicManager().<LobbyHome>getScene(SceneNames.HomeLobby).drawableManager.clearAllLists();
                FastJEngine.<SceneManager>getLogicManager().switchScenes(sceneName);
            });
        });
    }

    public void disableClientCommands() {
        Client client = user.getClient();

        client.addCommand(Commands.ClientJoinLobby, ClientInfo.class, (c, clientInfo) -> {});
        client.addCommand(Commands.ClientLeaveLobby, ClientInfo.class, (c, clientInfo) -> {});
        client.addCommand(Commands.UpdateClientInfo, ClientInfo.class, (c, clientInfo) -> {});
        client.addCommand(Commands.SnowballThrow, SnowballInfo.class, (c, snowballInfo) -> {});
        client.addCommand(Commands.SnowballHit, ClientInfo.class, SnowballInfo.class, (c, playerHit, snowballInfo) -> {});

        client.addCommand(Commands.GameFinished, ClientInfo.class, (c, winnerInfo) -> {});
        client.addCommand(Commands.GameResults, ClientPoints.class, (c, clientPoints) -> {});

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

    public void updateSnowballsCarried(int snowballCount, boolean canMakeSnowball, boolean canThrowSnowball) {
        snowballStatus.setCurrentValue(snowballCount);
        snowballThrowStatus.setCurrentStatus(canMakeSnowball);
        snowballMakeStatus.setCurrentStatus(canThrowSnowball);
    }

    public void spawnSnowball(Player player, Pointf trajectory, float playerRotation) {
        Snowball snowball = new Snowball(
            User.getInstance().getClientInfo(),
            trajectory,
            playerRotation,
            player,
            this,
            Snowball.StartingLife
        );

        try {
            user.getClient().sendCommand(NetworkType.TCP, CommandTarget.Session, Commands.SnowballThrow, snowball.getSnowballInfo());
            drawableManager.addGameObject(snowball);
        } catch (IOException exception) {
            if (!User.getInstance().getClient().isConnected()) {
                disconnectClient();
            } else {
                Main.gameCrashed("Failed to throw a snowball.", exception);
            }
        }

        Log.debug("Created snowball moving at a trajectory of {} with life starting at {}", trajectory, Snowball.StartingLife);
    }

    public Snowball removeSnowball(SnowballInfo snowballInfo) {
        return snowballs.remove(Pair.of(snowballInfo.clientInfo().clientId(), snowballInfo.snowballId()));
    }

    public void deathBySnowball(SnowballInfo snowballInfo) {
        ClientInfo snowballOrigin = snowballInfo.clientInfo();

        Log.info(SnowballFight.class, "You were hit with snowball from {}", snowballOrigin.clientName());

        try {
            playerGameState.setPlayerDead(true);
            user.getClient().sendCommand(NetworkType.TCP, CommandTarget.Session, Commands.SnowballHit, user.getClientInfo(), snowballInfo);
        } catch (IOException exception) {
            if (!User.getInstance().getClient().isConnected()) {
                disconnectClient();
            } else {
                Main.gameCrashed("Failed to be hit by a snowball.", exception);
            }
        }
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
}
