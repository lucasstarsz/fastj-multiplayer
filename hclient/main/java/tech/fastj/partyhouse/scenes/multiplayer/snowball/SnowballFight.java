package tech.fastj.partyhouse.scenes.multiplayer.snowball;

import tech.fastj.engine.FastJEngine;
import tech.fastj.logging.Log;
import tech.fastj.math.Pointf;
import tech.fastj.graphics.display.FastJCanvas;

import tech.fastj.systems.collections.Pair;
import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SceneManager;

import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import tech.fastj.gameloop.CoreLoopState;
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
import tech.fastj.partyhouse.util.ClientUtil;
import tech.fastj.partyhouse.util.Fonts;
import tech.fastj.partyhouse.util.PlayerUtil;
import tech.fastj.partyhouse.util.SceneNames;
import tech.fastj.partyhouse.util.Tags;
import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.ClientPoints;
import tech.fastj.partyhousecore.Commands;
import tech.fastj.partyhousecore.PositionState;
import tech.fastj.partyhousecore.SnowballInfo;

public class SnowballFight extends Scene {

    private final User user;

    private ContentBox pingDisplay;

    private final Map<UUID, PositionState> otherPlayerPositionStates;
    private final Map<UUID, Player> otherPlayers;
    private final Map<Pair<UUID, UUID>, Snowball> snowballs;

    private PositionState playerPositionState;
    private Player player;

    private PercentageBox<Integer> snowballStatus;
    private StatusBox snowballThrowStatus;
    private StatusBox snowballMakeStatus;

    private ResultMenu resultMenu;

    public SnowballFight() {
        super(SceneNames.SnowballFight);

        otherPlayerPositionStates = new LinkedHashMap<>();
        otherPlayers = new LinkedHashMap<>();
        user = User.getInstance();
        snowballs = new HashMap<>();
    }

    public boolean isPlayerDead() {
        return playerPositionState.isPlayerDead();
    }

    public ContentBox getPingDisplay() {
        return pingDisplay;
    }

    @Override
    public void load(FastJCanvas canvas) {
        Log.debug(SnowballFight.class, "loading {}", getSceneName());

        ClientUtil.clientNullCheck();
        var client = user.getClient();

        client.startKeepAlives(1L, TimeUnit.SECONDS);
        setupClientCommands();

        pingDisplay = ClientUtil.setupClientPingForDisplay(client, this);

        playerPositionState = PlayerUtil.createPositionState(user.getClientInfo(), canvas.getCanvasCenter());
        player = PlayerUtil.createPlayer(playerPositionState);
        player.addTag(Tags.LocalPlayer);

        HomeController homeController = new HomeController(250, 300, playerPositionState);
        player.addBehavior(homeController, this);

        SnowballController snowballController = new SnowballController(this);
        player.addBehavior(snowballController, this);

        snowballStatus = new PercentageBox<>(this, 0, SnowballController.MaxSnowballsCarried, "Snowballs: ");
        snowballStatus.getStatDisplay().setFont(Fonts.StatTextFont);
        snowballStatus.translate(Pointf.down().multiply(90f));

        snowballMakeStatus = new StatusBox(this, "Make Snowball", false);
        snowballMakeStatus.getStatDisplay().setFont(Fonts.SmallStatTextFontPlain);
        snowballMakeStatus.translate(Pointf.down().multiply(115f));

        snowballThrowStatus = new StatusBox(this, "Throw Snowball", false);
        snowballThrowStatus.getStatDisplay().setFont(Fonts.SmallStatTextFontPlain);
        snowballThrowStatus.translate(Pointf.down().multiply(140f));

        drawableManager().addGameObject(player);

        Log.debug(SnowballFight.class, "loaded {}", getSceneName());
    }

    @Override
    public void unload(FastJCanvas canvas) {
        Log.debug(SnowballFight.class, "unloading {}", getSceneName());

        user.getClient().resetCommands();

        otherPlayers.clear();
        otherPlayerPositionStates.clear();
        playerPositionState = null;

        Log.debug(SnowballFight.class, "unloaded {}", getSceneName());
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

        playerPositionState.updatePlayerPosition(player);
    }

    public void setupClientCommands() {
        var client = user.getClient();
        Pointf center = FastJEngine.getCanvas().getCanvasCenter();

        ClientUtil.addDefault2DControlCommands(client, center, otherPlayers, otherPlayerPositionStates, this);

        client.addCommand(Commands.SnowballThrow, (SnowballInfo snowballInfo) -> FastJEngine.runLater(() -> {
            Snowball snowball = new Snowball(snowballInfo, this);
            drawableManager().addGameObject(snowball);

            System.out.println(snowball.isDestroyed());
            System.out.println(snowball.shouldRender());
            System.out.println(snowball.getCenter());
            System.out.println(drawableManager().getGameObjects().get(snowball.getID()));

            Snowball replaced = snowballs.put(Pair.of(snowballInfo.clientInfo().clientId(), snowballInfo.snowballId()), snowball);

            if (replaced != null) {
                System.out.println("replacing " + replaced);
                replaced.destroy(this);
            }

            Log.info("{} threw a snowball.", snowballInfo.clientInfo().clientName());
        }, CoreLoopState.LateUpdate));

        client.addCommand(Commands.SnowballHit, (ClientInfo playerHit, SnowballInfo snowballInfo) -> FastJEngine.runLater(() -> {
            Log.info("{} was incapacitated by {}'s snowball.", playerHit.clientName(), snowballInfo.clientInfo().clientName());

            otherPlayerPositionStates.get(snowballInfo.clientInfo().clientId()).setPlayerDead(true);
            Snowball removedSnowball = removeSnowball(snowballInfo);

            if (removedSnowball != null) {
                removedSnowball.destroy(this);
            }
        }, CoreLoopState.LateUpdate));

        client.addCommand(Commands.GameFinished, (ClientInfo winnerInfo) -> FastJEngine.runLater(() -> {
            Log.info("{} won!", winnerInfo);
            playerPositionState.setPlayerDead(true);
            resultMenu = new ResultMenu(this, winnerInfo);
        }, CoreLoopState.LateUpdate));

        client.addCommand(Commands.GameResults, (ClientPoints[] clientPoints) -> {
            if (resultMenu != null) {
                System.out.println("ok but actually");
            } else {
                System.out.println("or maybe not");
            }

            FastJEngine.runLater(() -> resultMenu.addPointsResults(clientPoints, this, "Returning to lobby"), CoreLoopState.LateUpdate);
        });

        client.addCommand(Commands.SwitchScene, (String sceneName) -> {
            Log.info("Switching to scene \"{}\"", sceneName);
            FastJEngine.runLater(() -> {
                FastJEngine.<SceneManager>getLogicManager().<LobbyHome>getScene(SceneNames.HomeLobby).drawableManager().clearAllLists();
                FastJEngine.<SceneManager>getLogicManager().switchScenes(sceneName);
            }, CoreLoopState.LateUpdate);
        });
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
            drawableManager().addGameObject(snowball);
        } catch (IOException exception) {
            if (!User.getInstance().getClient().isConnected()) {
                ClientUtil.disconnectClient();
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
            playerPositionState.setPlayerDead(true);
            user.getClient().sendCommand(NetworkType.TCP, CommandTarget.Session, Commands.SnowballHit, user.getClientInfo(), snowballInfo);
        } catch (IOException exception) {
            if (!User.getInstance().getClient().isConnected()) {
                ClientUtil.disconnectClient();
            } else {
                Main.gameCrashed("Failed to be hit by a snowball.", exception);
            }
        }
    }
}
