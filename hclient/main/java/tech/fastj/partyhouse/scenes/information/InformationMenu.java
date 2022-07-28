package tech.fastj.partyhouse.scenes.information;

import tech.fastj.engine.FastJEngine;
import tech.fastj.logging.Log;
import tech.fastj.math.Pointf;
import tech.fastj.math.Transform2D;
import tech.fastj.graphics.display.FastJCanvas;
import tech.fastj.graphics.game.Text2D;

import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SceneManager;

import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;

import tech.fastj.partyhouse.ui.BetterButton;
import tech.fastj.partyhouse.ui.LinkText;
import tech.fastj.partyhouse.util.Colors;
import tech.fastj.partyhouse.util.Fonts;
import tech.fastj.partyhouse.util.SceneNames;
import tech.fastj.partyhouse.util.Shapes;

public class InformationMenu extends Scene {

    private Text2D howToPlayHeader;
    private Text2D controlsText;
    private Text2D gameAimText;
    private Text2D themeText;
    private Text2D sendoffText;

    private Text2D creditsHeader;
    private Text2D creditsText;
    private LinkText githubLink;
    private LinkText spotifyLink;
    private BetterButton mainMenuButton;

    public InformationMenu() {
        super(SceneNames.Information);
    }

    @Override
    public void load(FastJCanvas canvas) {
        Log.debug(InformationMenu.class, "loading {}", getSceneName());
        Pointf center = canvas.getCanvasCenter();

        howToPlayHeader = Text2D.create("How to Play")
            .withFont(Fonts.SubtitleTextFont)
            .withFill(Colors.Snowy)
            .withTransform(Pointf.subtract(center, 425f, 150f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager.addGameObject(howToPlayHeader);

        controlsText = Text2D.create("Choose a server IP and join a lobby.")
            .withFont(Fonts.SmallStatTextFontPlain)
            .withFill(Colors.Snowy)
            .withTransform(Pointf.subtract(center, 605f, 75f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager.addGameObject(controlsText);

        gameAimText = Text2D.create("It requires two to tango -- at least two players are required in a lobby for a match to begin.")
            .withFont(Fonts.SmallStatTextFontPlain)
            .withFill(Colors.Snowy)
            .withTransform(Pointf.subtract(center, 605f, 50f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager.addGameObject(gameAimText);

        themeText = Text2D.create("Use WASD to move, R to reload snowballs, and SPACE to throw them.")
            .withFont(Fonts.SmallStatTextFontPlain)
            .withFill(Colors.Snowy)
            .withTransform(Pointf.subtract(center, 605f, 25f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager.addGameObject(themeText);

        sendoffText = Text2D.create("May the best player win!")
            .withFont(Fonts.SmallStatTextFontPlain)
            .withFill(Colors.Snowy)
            .withTransform(Pointf.subtract(center, 605f, 0f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager.addGameObject(sendoffText);

        creditsHeader = Text2D.create("Credits")
            .withFont(Fonts.SubtitleTextFont)
            .withFill(Colors.Snowy)
            .withTransform(Pointf.subtract(center, -225f, 150f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager.addGameObject(creditsHeader);

        creditsText = Text2D.create("All content was made by lucasstarsz -- even the game engine!")
            .withFont(Fonts.SmallStatTextFontPlain)
            .withFill(Colors.Snowy)
            .withTransform(Pointf.subtract(center, -80f, 75f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager.addGameObject(creditsText);

        try {
            githubLink = new LinkText(this, "lucasstarsz's GitHub", new URL("https://github.com/lucasstarsz"));
            githubLink.setFont(Fonts.SmallStatTextFontBold);
            githubLink.setFill(Colors.Snowy);
            githubLink.setTranslation(Pointf.subtract(center, -222.5f, 50f));

            spotifyLink = new LinkText(this, "Link to the Game Repo", new URL("https://github.com/lucasstarsz/java-jam-2022"));
            spotifyLink.setFont(Fonts.SmallStatTextFontBold);
            spotifyLink.setFill(Colors.Snowy);
            spotifyLink.setTranslation(Pointf.subtract(center, -222.5f, 25f));
        } catch (MalformedURLException exception) {
            throw new RuntimeException(exception);
        }

        mainMenuButton = new BetterButton(this, Pointf.subtract(center, 100f, -150f), Shapes.ButtonSize);
        mainMenuButton.setText("Back");
        mainMenuButton.setFill(Color.darkGray);
        mainMenuButton.setFont(Fonts.ButtonTextFont);
        mainMenuButton.setOutlineColor(Colors.Snowy);
        mainMenuButton.setTextColor(Colors.Snowy);
        mainMenuButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            FastJEngine.runAfterRender(() -> FastJEngine.<SceneManager>getLogicManager().switchScenes(SceneNames.MainMenu));
        });

        Log.debug(InformationMenu.class, "loaded {}", getSceneName());
    }

    @Override
    public void unload(FastJCanvas canvas) {
        Log.debug(InformationMenu.class, "unloading {}", getSceneName());

        if (howToPlayHeader != null) {
            howToPlayHeader.destroy(this);
            howToPlayHeader = null;
        }

        if (controlsText != null) {
            controlsText.destroy(this);
            controlsText = null;
        }

        if (gameAimText != null) {
            gameAimText.destroy(this);
            gameAimText = null;
        }

        if (themeText != null) {
            themeText.destroy(this);
            themeText = null;
        }

        if (sendoffText != null) {
            sendoffText.destroy(this);
            sendoffText = null;
        }

        if (creditsHeader != null) {
            creditsHeader.destroy(this);
            creditsHeader = null;
        }

        if (creditsText != null) {
            creditsText.destroy(this);
            creditsText = null;
        }

        if (githubLink != null) {
            githubLink.destroy(this);
            githubLink = null;
        }

        if (spotifyLink != null) {
            spotifyLink.destroy(this);
            spotifyLink = null;
        }

        if (mainMenuButton != null) {
            mainMenuButton.destroy(this);
            mainMenuButton = null;
        }

        setInitialized(false);

        Log.debug(InformationMenu.class, "unloaded {}", getSceneName());
    }

    @Override
    public void fixedUpdate(FastJCanvas canvas) {
    }

    @Override
    public void update(FastJCanvas canvas) {
    }
}
