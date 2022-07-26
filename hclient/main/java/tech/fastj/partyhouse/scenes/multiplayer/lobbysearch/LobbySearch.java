package tech.fastj.partyhouse.scenes.multiplayer.lobbysearch;

import tech.fastj.engine.FastJEngine;
import tech.fastj.graphics.display.FastJCanvas;
import tech.fastj.graphics.game.Text2D;
import tech.fastj.logging.Log;
import tech.fastj.math.Pointf;
import tech.fastj.math.Transform2D;
import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;
import tech.fastj.partyhouse.Main;
import tech.fastj.partyhouse.scenes.mainmenu.MainMenu;
import tech.fastj.partyhouse.ui.BetterButton;
import tech.fastj.partyhouse.user.User;
import tech.fastj.partyhouse.util.Colors;
import tech.fastj.partyhouse.util.Fonts;
import tech.fastj.partyhouse.util.SceneNames;
import tech.fastj.partyhouse.util.Shapes;
import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SceneManager;

import java.awt.*;
import java.io.IOException;

public class LobbySearch extends Scene {

    private Text2D titleText;
    private BetterButton playButton;
    private BetterButton infoButton;
    private BetterButton songEditorButton;
    private BetterButton settingsButton;
    private BetterButton exitButton;

    private User user;

    public LobbySearch() {
        super(SceneNames.LobbySearch);
    }

    @Override
    public void load(FastJCanvas canvas) {
        Log.debug(MainMenu.class, "loading {}", getSceneName());
        Pointf center = canvas.getCanvasCenter();

        titleText = Text2D.create(Main.GameName)
                .withFill(Colors.Snowy)
                .withFont(Fonts.TitleTextFont)
                .withTransform(Pointf.subtract(center, 260f, 200f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
                .build();
        drawableManager.addGameObject(titleText);

        playButton = new BetterButton(this, Pointf.subtract(center, 225f, 50f), Shapes.ButtonSize);
        playButton.setText("Play Game");
        playButton.setFill(Color.darkGray);
        playButton.setFont(Fonts.ButtonTextFont);
        playButton.setOutlineColor(Colors.Snowy);
        playButton.setTextColor(Colors.Snowy);
        playButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            FastJEngine.runAfterRender(() -> FastJEngine.<SceneManager>getLogicManager().switchScenes(SceneNames.SongPicker, false));
        });

        infoButton = new BetterButton(this, Pointf.subtract(center, -25f, 50f), Shapes.ButtonSize);
        infoButton.setText("Information");
        infoButton.setFill(Color.darkGray);
        infoButton.setFont(Fonts.ButtonTextFont);
        infoButton.setOutlineColor(Colors.Snowy);
        infoButton.setTextColor(Colors.Snowy);
        infoButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            FastJEngine.runAfterRender(() -> FastJEngine.<SceneManager>getLogicManager().switchScenes(SceneNames.Information, false));
        });

        songEditorButton = new BetterButton(this, Pointf.subtract(center, 225f, -50f), Shapes.ButtonSize);
        songEditorButton.setText("Song Editor");
        songEditorButton.setFill(Color.darkGray);
        songEditorButton.setFont(Fonts.ButtonTextFont);
        songEditorButton.setOutlineColor(Colors.Snowy);
        songEditorButton.setTextColor(Colors.Snowy);
        songEditorButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            FastJEngine.runAfterRender(() -> FastJEngine.<SceneManager>getLogicManager().switchScenes(SceneNames.SongEditor));
        });

        settingsButton = new BetterButton(this, Pointf.subtract(center, -25f, -50f), Shapes.ButtonSize);
        settingsButton.setText("Settings");
        settingsButton.setFill(Color.darkGray);
        settingsButton.setFont(Fonts.ButtonTextFont);
        settingsButton.setOutlineColor(Colors.Snowy);
        settingsButton.setTextColor(Colors.Snowy);
        settingsButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            FastJEngine.runAfterRender(() -> FastJEngine.<SceneManager>getLogicManager().switchScenes(SceneNames.Settings, false));
        });

        exitButton = new BetterButton(this, Pointf.subtract(center, 100f, -150f), Shapes.ButtonSize);
        exitButton.setText("Quit Game");
        exitButton.setFill(Color.darkGray);
        exitButton.setFont(Fonts.ButtonTextFont);
        exitButton.setOutlineColor(Colors.Snowy);
        exitButton.setTextColor(Colors.Snowy);
        exitButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            FastJEngine.runAfterRender(FastJEngine.getDisplay()::close);
        });

        Log.debug(MainMenu.class, "loaded {}", getSceneName());
    }

    @Override
    public void unload(FastJCanvas canvas) {
        Log.debug(MainMenu.class, "unloading {}", getSceneName());
        if (titleText != null) {
            titleText.destroy(this);
            titleText = null;
        }

        if (playButton != null) {
            playButton.destroy(this);
            playButton = null;
        }

        if (infoButton != null) {
            infoButton.destroy(this);
            infoButton = null;
        }

        if (settingsButton != null) {
            settingsButton.destroy(this);
            settingsButton = null;
        }

        if (songEditorButton != null) {
            songEditorButton.destroy(this);
            songEditorButton = null;
        }

        if (exitButton != null) {
            exitButton.destroy(this);
            exitButton = null;
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

    private void checkForLobbies() throws IOException, InterruptedException {
        LobbyIdentifier[] lobbies = user.getClient().getAvailableLobbies();
        updateLobbyList(lobbies);
    }

    private void updateLobbyList(LobbyIdentifier[] lobbies) {

    }
}
