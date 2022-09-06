package tech.fastj.partyhouse.scenes.multiplayer.lobbysearch;

import tech.fastj.engine.FastJEngine;
import tech.fastj.logging.Log;
import tech.fastj.math.Pointf;
import tech.fastj.math.Transform2D;
import tech.fastj.graphics.dialog.DialogConfig;
import tech.fastj.graphics.display.FastJCanvas;
import tech.fastj.graphics.game.Text2D;

import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SceneManager;

import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.rpc.local.LocalClient;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import tech.fastj.gameloop.CoreLoopState;
import tech.fastj.partyhouse.Main;
import tech.fastj.partyhouse.scenes.mainmenu.MainMenu;
import tech.fastj.partyhouse.scenes.multiplayer.home.LobbyHome;
import tech.fastj.partyhouse.ui.BetterButton;
import tech.fastj.partyhouse.ui.LobbyContentBox;
import tech.fastj.partyhouse.user.User;
import tech.fastj.partyhouse.util.Buttons;
import tech.fastj.partyhouse.util.ClientUtil;
import tech.fastj.partyhouse.util.Colors;
import tech.fastj.partyhouse.util.Dialogs;
import tech.fastj.partyhouse.util.Fonts;
import tech.fastj.partyhouse.util.SceneNames;
import tech.fastj.partyhouse.util.Shapes;
import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.Commands;
import tech.fastj.partyhousecore.Info;

public class LobbySearch extends Scene {

    private final List<LobbyContentBox> lobbyList;
    private LobbyContentBox selectedLobby;

    private User user;

    public LobbySearch() {
        super(SceneNames.LobbySearch);
        lobbyList = new ArrayList<>();
    }

    @Override
    public void load(FastJCanvas canvas) {
        Log.debug(MainMenu.class, "loading {}", getSceneName());
        Pointf center = canvas.getCanvasCenter();

        user = User.getInstance();

        Text2D titleText = Text2D.create("Lobby Search")
            .withFill(Colors.Snowy)
            .withFont(Fonts.H3TextFont)
            .withTransform(new Pointf(25f, 25f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager().addGameObject(titleText);

        BetterButton joinSelectedLobbyButton = Buttons.create(this, canvas, (Shapes.ButtonSize.x * 0.25f), 50f, "Join Lobby");
        joinSelectedLobbyButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            FastJEngine.runLater(() -> {
                try {
                    joinSelectedLobby(canvas);
                } catch (IOException | InterruptedException exception) {
                    Log.error("Error while trying to join lobby", exception);

                    if (exception instanceof IOException joinException) {
                        Dialogs.message(
                            DialogConfig.create()
                                .withTitle("Error while trying to join lobby")
                                .withPrompt(joinException.getMessage())
                                .build()
                        );
                    } else {
                        if (!User.getInstance().getClient().isConnected()) {
                            ClientUtil.disconnectClient();
                        } else {
                            Main.gameCrashed("Crashed while trying to join lobby", exception);
                        }
                    }
                }
            }, CoreLoopState.LateUpdate);
        });

        BetterButton refreshLobbiesButton = Buttons.create(this, canvas, -(Shapes.ButtonSize.x * 1.25f), 50f, "Refresh Lobbies");
        refreshLobbiesButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            FastJEngine.runLater(() -> {
                try {
                    checkForLobbies(center);
                } catch (IOException | InterruptedException exception) {
                    Log.error("Error while trying to refresh lobbies", exception);

                    if (exception instanceof ConnectException connectException) {
                        Dialogs.message(
                            DialogConfig.create()
                                .withTitle("Error while trying to refresh lobbies")
                                .withPrompt(connectException.getMessage())
                                .build()
                        );
                    } else {
                        if (!User.getInstance().getClient().isConnected()) {
                            ClientUtil.disconnectClient();
                        } else {
                            Main.gameCrashed("Crashed while trying to refresh lobbies", exception);
                        }
                    }
                }
            }, CoreLoopState.LateUpdate);
        });

        // keep -- main menu button's functionality is fully declared in Buttons.menu(...);
        BetterButton mainMenuButton = Buttons.menu(this, canvas, -100f, 200f, "Back", SceneNames.MainMenu);

        Log.debug(MainMenu.class, "loaded {}", getSceneName());
    }

    @Override
    public void unload(FastJCanvas canvas) {
        Log.debug(MainMenu.class, "unloading {}", getSceneName());

        lobbyList.clear();

        Log.debug(MainMenu.class, "unloaded {}", getSceneName());
    }

    @Override
    public void fixedUpdate(FastJCanvas canvas) {
    }

    @Override
    public void update(FastJCanvas canvas) {
    }

    private void checkForLobbies(Pointf center) throws IOException, InterruptedException {
        if (user.getClient() == null) {
            trySetupClient();

            if (user.getClient() == null) {
                return;
            }
        }

        var client = user.getClient();

        LobbyIdentifier[] lobbies = client.getAvailableLobbies();
        updateLobbyList(center, lobbies);
    }

    private void joinSelectedLobby(FastJCanvas canvas) throws IOException, InterruptedException {
        if (user.getClient() == null) {
            trySetupClient();

            if (user.getClient() == null) {
                return;
            }
        }

        if (selectedLobby == null) {
            Dialogs.message(
                DialogConfig.create()
                    .withTitle("No selected lobby")
                    .withPrompt("You must select an available lobby before trying to connect.")
                    .build()
            );

            return;
        }

        LobbyIdentifier lobby = selectedLobby.getLobby();
        var client = user.getClient();

        SwingUtilities.invokeLater(() -> {
            String name = Dialogs.userInput(
                DialogConfig.create()
                    .withParentComponent(FastJEngine.getDisplay().getWindow())
                    .withTitle("Set nickname")
                    .withPrompt("Please set a nickname. (must not be whitespace)")
                    .build()
            );

            if (name == null) {
                return;
            }

            user.setClientInfo(new ClientInfo(client.getClientId(), name));
            Log.info("New name: {}", name);

            try {
                LobbyIdentifier newLobbyId = client.joinLobby(lobby.id());

                if (newLobbyId == null) {
                    throw new IOException("Denied access to lobby.");
                }

                client.sendCommand(NetworkType.TCP, CommandTarget.Lobby, Commands.UpdateClientInfo, user.getClientInfo());

                FastJEngine.runLater(() -> {
                    FastJEngine.<SceneManager>getLogicManager().getScene(SceneNames.MainMenu).unload(canvas);
                    FastJEngine.<SceneManager>getLogicManager().switchScenes(SceneNames.HomeLobby);
                }, CoreLoopState.LateUpdate);
            } catch (Exception exception) {
                if (!User.getInstance().getClient().isConnected()) {
                    ClientUtil.disconnectClient();
                } else {
                    Main.gameCrashed("Failed to join lobby", exception);
                }
            }
        });
    }

    private void trySetupClient() throws IOException {
        var client = new LocalClient<>(new ClientConfig(user.getCustomIp(), Info.DefaultPort), Commands.class);

        client.setOnDisconnect(() -> {
            if (!FastJEngine.isRunning()) {
                return;
            }

            String sceneName = FastJEngine.<SceneManager>getLogicManager().getCurrentScene().getSceneName();

            if (!sceneName.equals(SceneNames.HomeLobby) && !sceneName.equals(SceneNames.SnowballFight)) {
                return;
            }

            ClientUtil.disconnectClient();
        });

        client.connect();
        client.startKeepAlives(1L, TimeUnit.SECONDS);

        user.setClient(client);
        user.setClientInfo(new ClientInfo(client.getClientId(), "???"));
        FastJEngine.<SceneManager>getLogicManager().<LobbyHome>getScene(SceneNames.HomeLobby).setupClientCommands();
    }

    private void updateLobbyList(Pointf center, LobbyIdentifier[] lobbies) {
        for (LobbyContentBox contentBox : lobbyList) {
            contentBox.destroy(this);
        }

        lobbyList.clear();

        for (int i = 0; i < lobbies.length; i++) {
            LobbyIdentifier lobby = lobbies[i];
            LobbyContentBox contentBox = new LobbyContentBox(this, lobby);

            contentBox.setTranslation(Pointf.subtract(center, 100f, 100f - (25f * i)));
            contentBox.getStatDisplay().setFill(Colors.Snowy);
            contentBox.getStatDisplay().setFont(Fonts.StatTextFont);

            contentBox.addOnAction((mouseButtonEvent) -> {
                if (contentBox.isFocused()) {
                    for (LobbyContentBox lobbyContentBox : lobbyList) {
                        lobbyContentBox.setFocused(false);
                    }

                    contentBox.setFocused(true);
                    selectedLobby = contentBox;
                }
            });

            lobbyList.add(contentBox);
        }
    }
}
