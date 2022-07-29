package tech.fastj.partyhouse.scenes.information;

import tech.fastj.logging.Log;
import tech.fastj.math.Pointf;
import tech.fastj.math.Transform2D;
import tech.fastj.graphics.display.FastJCanvas;
import tech.fastj.graphics.game.Text2D;

import tech.fastj.systems.control.Scene;

import java.net.MalformedURLException;
import java.net.URL;

import tech.fastj.partyhouse.ui.BetterButton;
import tech.fastj.partyhouse.ui.LinkText;
import tech.fastj.partyhouse.util.Buttons;
import tech.fastj.partyhouse.util.Colors;
import tech.fastj.partyhouse.util.Fonts;
import tech.fastj.partyhouse.util.SceneNames;

public class InformationMenu extends Scene {

    public InformationMenu() {
        super(SceneNames.Information);
    }

    @Override
    public void load(FastJCanvas canvas) {
        Log.debug(InformationMenu.class, "loading {}", getSceneName());
        Pointf center = canvas.getCanvasCenter();

        Text2D howToPlayHeader = Text2D.create("How to Play")
            .withFont(Fonts.SubtitleTextFont)
            .withFill(Colors.Snowy)
            .withTransform(Pointf.subtract(center, 425f, 150f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager().addGameObject(howToPlayHeader);

        Text2D controlsText = Text2D.create("Choose a server IP and join a lobby.")
            .withFont(Fonts.SmallStatTextFontPlain)
            .withFill(Colors.Snowy)
            .withTransform(Pointf.subtract(center, 605f, 75f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager().addGameObject(controlsText);

        Text2D gameAimText = Text2D.create("It requires two to tango -- at least two players are required in a lobby for a match to begin.")
            .withFont(Fonts.SmallStatTextFontPlain)
            .withFill(Colors.Snowy)
            .withTransform(Pointf.subtract(center, 605f, 50f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager().addGameObject(gameAimText);

        Text2D themeText = Text2D.create("Use WASD to move, R to reload snowballs, and SPACE to throw them.")
            .withFont(Fonts.SmallStatTextFontPlain)
            .withFill(Colors.Snowy)
            .withTransform(Pointf.subtract(center, 605f, 25f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager().addGameObject(themeText);

        Text2D sendoffText = Text2D.create("May the best player win!")
            .withFont(Fonts.SmallStatTextFontPlain)
            .withFill(Colors.Snowy)
            .withTransform(Pointf.subtract(center, 605f, 0f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager().addGameObject(sendoffText);

        Text2D creditsHeader = Text2D.create("Credits")
            .withFont(Fonts.SubtitleTextFont)
            .withFill(Colors.Snowy)
            .withTransform(Pointf.subtract(center, -235f, 150f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager().addGameObject(creditsHeader);

        Text2D creditsText = Text2D.create("All content was made by lucasstarsz -- even the game engine!")
            .withFont(Fonts.SmallStatTextFontPlain)
            .withFill(Colors.Snowy)
            .withTransform(Pointf.subtract(center, -90f, 75f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager().addGameObject(creditsText);

        try {
            LinkText githubLink = new LinkText(this, "lucasstarsz's GitHub", new URL("https://github.com/lucasstarsz"));
            githubLink.setFont(Fonts.SmallStatTextFontBold);
            githubLink.setFill(Colors.Snowy);
            githubLink.setTranslation(Pointf.subtract(center, -230f, 50f));

            LinkText partyHouseLink = new LinkText(this, "Party House on GitHub", new URL("https://github.com/lucasstarsz/java-jam-2022"));
            partyHouseLink.setFont(Fonts.SmallStatTextFontBold);
            partyHouseLink.setFill(Colors.Snowy);
            partyHouseLink.setTranslation(Pointf.subtract(center, -220f, 25f));

            LinkText fastjLink = new LinkText(this, "The FastJ Game Engine", new URL("https://github.com/fastjengine/FastJ"));
            fastjLink.setFont(Fonts.SmallStatTextFontBold);
            fastjLink.setFill(Colors.Snowy);
            fastjLink.setTranslation(Pointf.subtract(center, -220f, 0f));
        } catch (MalformedURLException exception) {
            throw new RuntimeException(exception);
        }

        // keep -- menu button's functionality is fully declared in Buttons.menu(...);
        BetterButton mainMenuButton = Buttons.menu(this, canvas, -100f, 150f, "Back", SceneNames.MainMenu);

        Log.debug(InformationMenu.class, "loaded {}", getSceneName());
    }

    @Override
    public void unload(FastJCanvas canvas) {
        Log.debug(InformationMenu.class, "unloading {}", getSceneName());
        Log.debug(InformationMenu.class, "unloaded {}", getSceneName());
    }

    @Override
    public void fixedUpdate(FastJCanvas canvas) {
    }

    @Override
    public void update(FastJCanvas canvas) {
    }
}
