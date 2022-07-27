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
import tech.fastj.network.rpc.Client;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import tech.fastj.partyhouse.Main;
import tech.fastj.partyhouse.scenes.mainmenu.MainMenu;
import tech.fastj.partyhouse.scenes.multiplayer.game.HomeLobby;
import tech.fastj.partyhouse.ui.BetterButton;
import tech.fastj.partyhouse.ui.LobbyContentBox;
import tech.fastj.partyhouse.user.User;
import tech.fastj.partyhouse.util.Buttons;
import tech.fastj.partyhouse.util.Colors;
import tech.fastj.partyhouse.util.Dialogs;
import tech.fastj.partyhouse.util.Fonts;
import tech.fastj.partyhouse.util.SceneNames;
import tech.fastj.partyhouse.util.Shapes;
import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.Commands;
import tech.fastj.partyhousecore.Info;
import tech.fastj.partyhousecore.Messages;

public class LobbySearch extends Scene {

    private Text2D titleText;
    private BetterButton joinSelectedLobbyButton;
    private BetterButton refreshLobbiesButton;
    private BetterButton mainMenuButton;
    private List<LobbyContentBox> lobbyList;
    private LobbyContentBox selectedLobby;

    private User user;

    public LobbySearch() {
        super(SceneNames.LobbySearch);
    }

    @Override
    public void load(FastJCanvas canvas) {
        Log.debug(MainMenu.class, "loading {}", getSceneName());
        Pointf center = canvas.getCanvasCenter();

        user = User.getInstance();

        titleText = Text2D.create("Lobby Search")
                .withFill(Colors.Snowy)
                .withFont(Fonts.H3TextFont)
                .withTransform(new Pointf(25f, 25f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
                .build();
        drawableManager.addGameObject(titleText);

        joinSelectedLobbyButton = Buttons.create(this, canvas, (Shapes.ButtonSize.x * 0.25f), 50f, "Join Lobby");
        joinSelectedLobbyButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            FastJEngine.runAfterRender(() -> {
                try {
                    joinSelectedLobby(canvas);
                } catch (IOException | InterruptedException exception) {
                    if (exception instanceof IOException joinException) {
                        Dialogs.message(
                                DialogConfig.create()
                                        .withTitle("Error while trying to join lobby")
                                        .withPrompt(joinException.getMessage())
                                        .build()
                        );
                    } else {
                        Main.gameCrashed("Crashed while trying to join lobby", exception);
                    }
                }
            });
        });

        refreshLobbiesButton = Buttons.create(this, canvas, -(Shapes.ButtonSize.x * 1.25f), 50f, "Refresh Lobbies");
        refreshLobbiesButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            FastJEngine.runAfterRender(() -> {
                try {
                    checkForLobbies(center);
                } catch (IOException | InterruptedException exception) {
                    if (exception instanceof ConnectException connectException) {
                        Dialogs.message(
                                DialogConfig.create()
                                        .withTitle("Error while trying to connect")
                                        .withPrompt(connectException.getMessage())
                                        .build()
                        );
                    } else {
                        Main.gameCrashed("Crashed while checking for lobbies", exception);
                    }
                }
            });
        });

        mainMenuButton = Buttons.menu(this, canvas, -100f, 200f, "Back", SceneNames.MainMenu);

        Log.debug(MainMenu.class, "loaded {}", getSceneName());
    }

    @Override
    public void unload(FastJCanvas canvas) {
        Log.debug(MainMenu.class, "unloading {}", getSceneName());
        if (titleText != null) {
            titleText.destroy(this);
            titleText = null;
        }

        if (joinSelectedLobbyButton != null) {
            joinSelectedLobbyButton.destroy(this);
            joinSelectedLobbyButton = null;
        }

        if (refreshLobbiesButton != null) {
            refreshLobbiesButton.destroy(this);
            refreshLobbiesButton = null;
        }

        if (mainMenuButton != null) {
            mainMenuButton.destroy(this);
            mainMenuButton = null;
        }

        if (lobbyList != null) {
            for (LobbyContentBox contentBox : lobbyList) {
                contentBox.destroy(this);
            }

            lobbyList.clear();
        } else {
            lobbyList = new ArrayList<>();
        }

        if (selectedLobby != null) {
            selectedLobby.destroy(this);
            selectedLobby = null;
        }

        setInitialized(false);
        Log.debug(MainMenu.class, "unloaded {}", getSceneName());
    }

    @Override
    public void fixedUpdate(FastJCanvas canvas) {
    }

    @Override
    public void update(FastJCanvas canvas) {
    }

    private void checkForLobbies(Pointf center) throws IOException, InterruptedException {
        if (user.getClient() == null && !setupClient()) {
            return;
        }

        Client client = user.getClient();

        LobbyIdentifier[] lobbies = client.getAvailableLobbies();
        updateLobbyList(center, lobbies);
    }

    private void joinSelectedLobby(FastJCanvas canvas) throws IOException, InterruptedException {
        if (user.getClient() == null && !setupClient()) {
            return;
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
        Client client = user.getClient();

        SwingUtilities.invokeLater(() -> {
            String name = Dialogs.userInput(
                    DialogConfig.create()
                            .withParentComponent(FastJEngine.getDisplay().getWindow())
                            .withTitle("Set nickname")
                            .withPrompt("Please set a nickname. (must not be whitespace)")
                            .build()
            );

            user.setClientInfo(new ClientInfo(client.getClientId(), name));
            Log.info("New name: {}", name);

            try {
                LobbyIdentifier newLobbyId = client.joinLobby(lobby.id());

                if (newLobbyId == null) {
                    throw new IOException("Denied access to lobby.");
                }

                client.sendCommand(NetworkType.TCP, CommandTarget.Lobby, Commands.UpdateClientInfo, user.getClientInfo());

                FastJEngine.runAfterUpdate(() -> {
                    FastJEngine.<SceneManager>getLogicManager().getScene(SceneNames.MainMenu).unload(canvas);
                    FastJEngine.<SceneManager>getLogicManager().switchScenes(SceneNames.HomeLobby);
                });
            } catch (Exception exception) {
                Main.gameCrashed("Failed to join lobby", exception);
            }
        });
    }

    private boolean setupClient() throws IOException {
        Client client = new Client(new ClientConfig(InetAddress.getByName(Info.DefaultIp), Info.DefaultPort));

        client.connect();
        client.startKeepAlives(1L, TimeUnit.SECONDS);
        Messages.updateSerializer(client.getSerializer());

        user.setClient(client);
        user.setClientInfo(new ClientInfo(client.getClientId(), "???"));
        FastJEngine.<SceneManager>getLogicManager().<HomeLobby>getScene(SceneNames.HomeLobby).setupClientCommands();

        return true;
    }

    private void updateLobbyList(Pointf center, LobbyIdentifier[] lobbies) {
        if (lobbyList != null) {
            for (LobbyContentBox contentBox : lobbyList) {
                contentBox.destroy(this);
            }

            lobbyList.clear();
            selectedLobby = null;
        } else {
            lobbyList = new ArrayList<>();
        }

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
