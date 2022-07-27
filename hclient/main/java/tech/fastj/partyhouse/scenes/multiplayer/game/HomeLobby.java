package tech.fastj.partyhouse.scenes.multiplayer.game;

import tech.fastj.engine.FastJEngine;
import tech.fastj.logging.Log;
import tech.fastj.math.Pointf;
import tech.fastj.graphics.display.FastJCanvas;

import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SceneManager;

import tech.fastj.network.rpc.Client;
import tech.fastj.network.rpc.message.CommandTarget;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import tech.fastj.partyhouse.Main;
import tech.fastj.partyhouse.objects.Player;
import tech.fastj.partyhouse.scenes.mainmenu.MainMenu;
import tech.fastj.partyhouse.scripts.HomeController;
import tech.fastj.partyhouse.ui.ContentBox;
import tech.fastj.partyhouse.user.User;
import tech.fastj.partyhouse.util.Colors;
import tech.fastj.partyhouse.util.Fonts;
import tech.fastj.partyhouse.util.SceneNames;
import tech.fastj.partyhouse.util.Tags;
import tech.fastj.partyhousecore.ClientGameState;
import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.ClientPosition;
import tech.fastj.partyhousecore.ClientVelocity;
import tech.fastj.partyhousecore.Commands;

public class HomeLobby extends Scene {

    private final User user;

    private ContentBox pingDisplay;

    private final Map<UUID, ClientGameState> otherGameStates;
    private final Map<UUID, Player> otherPlayers;

    private ClientGameState playerGameState;
    private Player player;
    private HomeController homeController;

    public HomeLobby() {
        super(SceneNames.HomeLobby);

        otherGameStates = new LinkedHashMap<>();
        otherPlayers = new LinkedHashMap<>();
        user = User.getInstance();
    }

    @Override
    public void load(FastJCanvas canvas) {
        Log.debug(MainMenu.class, "loading {}", getSceneName());

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

            if (scene instanceof HomeLobby lobby) {
                lobby.pingDisplay.setContent(pingMillis + "ms");
            }
        });

        client.startPings(100L, TimeUnit.MILLISECONDS);
        client.startKeepAlives(1L, TimeUnit.SECONDS);

        Pointf center = canvas.getCanvasCenter();
        playerGameState = new ClientGameState();
        playerGameState.setClientInfo(user.getClientInfo());
        playerGameState.setClientPosition(new ClientPosition(center.x, center.y));
        playerGameState.setClientVelocity(new ClientVelocity());

        player = new Player(playerGameState.getClientInfo().clientName());
        player.addTag(Tags.LocalPlayer);

        homeController = new HomeController(250, 300, playerGameState);
        player.addBehavior(homeController, this);

        drawableManager.addGameObject(player);

        Log.debug(MainMenu.class, "loaded {}", getSceneName());
    }

    @Override
    public void unload(FastJCanvas canvas) {
        Log.debug(MainMenu.class, "unloading {}", getSceneName());

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

        setInitialized(false);
        Log.debug(MainMenu.class, "unloaded {}", getSceneName());
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
        for (ClientGameState clientState : otherGameStates.values()) {
            Player otherPlayer = otherPlayers.get(clientState.getClientInfo().clientId());

            if (otherPlayer == null) {
                Main.gameCrashed("Missing player reference for " + clientState.getClientInfo().clientName(), new IllegalStateException());

                if (FastJEngine.isRunning()) {
                    FastJEngine.closeGame();
                }

                return;
            }

            clientState.updatePlayerPosition(otherPlayer);
        }

        playerGameState.updatePlayerPosition(player);
    }

    public void setupClientCommands() {
        Client client = user.getClient();
        Pointf center = FastJEngine.getCanvas().getCanvasCenter();

        client.addCommand(Commands.ClientJoined, ClientInfo.class, (c, clientInfo) -> {
            Log.info("{} joined.", clientInfo.clientName());

            ClientGameState clientGameState = createGameState(clientInfo, center);
            Player otherPlayer = createPlayer(clientGameState);

            drawableManager.addGameObject(otherPlayer);
            otherPlayers.put(clientInfo.clientId(), otherPlayer);
            otherGameStates.put(clientInfo.clientId(), clientGameState);
        });

        client.addCommand(Commands.ClientLeft, ClientInfo.class, (c, clientInfo) -> {
            Log.info("{} left.", clientInfo.clientName());

            FastJEngine.runAfterUpdate(() -> {
                otherGameStates.remove(clientInfo.clientId());

                Player otherPlayer = otherPlayers.remove(clientInfo.clientId());
                drawableManager.removeGameObject(otherPlayer);
                otherPlayer.destroy(this);
            });
        });

        client.addCommand(Commands.UpdateClientInfo, ClientInfo.class, (c, clientInfo) -> {
            Log.info("{} updated their client info.", clientInfo.clientName());

            ClientGameState clientGameState = otherGameStates.get(clientInfo.clientId());
            Player otherPlayer = otherPlayers.get(clientInfo.clientId());

            if (clientGameState == null || otherPlayer == null) {
                return;
            }

            clientGameState.setClientInfo(clientInfo);
            otherPlayer.setPlayerName(clientInfo.clientName());
        });

        client.addCommand(Commands.UpdateClientGameState,
            ClientInfo.class, ClientPosition.class, ClientVelocity.class,
            (c, clientInfo, clientPosition, clientVelocity) -> {
                Log.info("{} moved: {}, {}", clientInfo.clientName(), clientPosition.x(), clientPosition.y());

                ClientGameState clientState = otherGameStates.get(clientInfo.clientId());
                Player otherPlayer = otherPlayers.get(clientInfo.clientId());

                if (clientState == null || otherPlayer == null) {
                    return;
                }

                clientState.setClientInfo(clientInfo);
                clientState.setClientPosition(clientPosition);
                clientState.setClientVelocity(clientVelocity);

                otherPlayer.setPlayerName(clientInfo.clientName());
            }
        );
    }

    private Player createPlayer(ClientGameState clientState) {
        Log.info("creating new player instance for {}", clientState.getClientInfo().clientName());
        return new Player(clientState.getClientInfo().clientName());
    }

    private ClientGameState createGameState(ClientInfo clientInfo, Pointf center) {
        Log.info("creating new client info for {}", clientInfo.clientName());

        ClientGameState gameState = new ClientGameState();

        gameState.setClientInfo(clientInfo);
        gameState.setClientPosition(new ClientPosition(center.x, center.y));
        gameState.setClientVelocity(new ClientVelocity());

        return gameState;
    }
}
