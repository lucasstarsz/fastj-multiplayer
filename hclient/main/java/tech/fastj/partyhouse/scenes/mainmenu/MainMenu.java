package tech.fastj.partyhouse.scenes.mainmenu;

import tech.fastj.engine.FastJEngine;
import tech.fastj.logging.Log;
import tech.fastj.math.Pointf;
import tech.fastj.math.Transform2D;
import tech.fastj.graphics.display.FastJCanvas;
import tech.fastj.graphics.game.Text2D;

import tech.fastj.systems.audio.AudioEvent;
import tech.fastj.systems.audio.MemoryAudio;
import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SceneManager;

import javax.swing.SwingUtilities;
import java.io.IOException;

import tech.fastj.partyhouse.Main;
import tech.fastj.partyhouse.ui.BetterButton;
import tech.fastj.partyhouse.util.Buttons;
import tech.fastj.partyhouse.util.Colors;
import tech.fastj.partyhouse.util.Fonts;
import tech.fastj.partyhouse.util.SceneNames;
import tech.fastj.partyhouse.util.Shapes;

public class MainMenu extends Scene {

    private Text2D titleText;
    private BetterButton playButton;
    private BetterButton infoButton;
    private BetterButton songEditorButton;
    private BetterButton settingsButton;
    private BetterButton exitButton;
    private MemoryAudio mainMenuMusic;

    public MainMenu() {
        super(SceneNames.MainMenu);
    }

    @Override
    public void load(FastJCanvas canvas) {
        Log.debug(MainMenu.class, "loading {}", getSceneName());
        Pointf center = canvas.getCanvasCenter();

        titleText = Text2D.create(Main.GameName)
                .withFill(Colors.Snowy)
                .withFont(Fonts.TitleTextFont)
                .withTransform(Pointf.subtract(center, 225f, 200f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
                .build();
        drawableManager.addGameObject(titleText);

        playButton = Buttons.menu(this, canvas, -(Shapes.ButtonSize.x * 1.25f), -50f, "Play Game", SceneNames.LobbySearch, false);
        infoButton = Buttons.menu(this, canvas, (Shapes.ButtonSize.x * 0.25f), 50f, "Information", SceneNames.Information, false);
        settingsButton = Buttons.menu(this, canvas, (Shapes.ButtonSize.x * 0.25f), -50f, "Settings", SceneNames.Settings, false);

        songEditorButton = Buttons.create(this, canvas, -(Shapes.ButtonSize.x * 1.25f), 50f, "Song Editor");
        songEditorButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            FastJEngine.runAfterRender(() -> FastJEngine.<SceneManager>getLogicManager().switchScenes(SceneNames.SongEditor));
        });

        exitButton = Buttons.create(this, canvas, -Shapes.ButtonSize.x / 2f, 150f, "Quit Game");
        exitButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            SwingUtilities.invokeLater(FastJEngine::forceCloseGame);
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

        if (mainMenuMusic != null) {
            FastJEngine.getGameLoop().removeEventObserver(mainMenuMusic.getAudioEventListener(), AudioEvent.class);
            mainMenuMusic.stop();
            try {
                mainMenuMusic.getAudioInputStream().close();
            } catch (IOException exception) {
                Log.warn(MainMenu.class, "Error occurred while closing main menu music", exception);
            }
            mainMenuMusic = null;
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
}
