package tech.fastj.partyhouse.scenes.settings;

import tech.fastj.logging.Log;
import tech.fastj.math.Pointf;
import tech.fastj.math.Transform2D;
import tech.fastj.graphics.display.FastJCanvas;
import tech.fastj.graphics.game.Text2D;

import tech.fastj.systems.control.Scene;

import tech.fastj.partyhouse.ui.BetterButton;
import tech.fastj.partyhouse.util.Buttons;
import tech.fastj.partyhouse.util.Colors;
import tech.fastj.partyhouse.util.Fonts;
import tech.fastj.partyhouse.util.SceneNames;

public class Settings extends Scene {

    public Settings() {
        super(SceneNames.Settings);
    }

    @Override
    public void load(FastJCanvas canvas) {
        Log.debug(Settings.class, "loading {}", getSceneName());
        Pointf center = canvas.getCanvasCenter();

        Text2D titleText = Text2D.create("Settings")
            .withFont(Fonts.TitleTextFont)
            .withFill(Colors.Snowy)
            .withTransform(Pointf.subtract(center, 100f, 200f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager().addGameObject(titleText);

        // keep -- menu button's functionality is fully declared in Buttons.menu(...);
        BetterButton mainMenuButton = Buttons.menu(this, canvas, -100f, 150f, "Back", SceneNames.MainMenu);

        Log.debug(Settings.class, "loaded {}", getSceneName());
    }

    @Override
    public void unload(FastJCanvas canvas) {
        Log.debug(Settings.class, "unloading {}", getSceneName());
        Log.debug(Settings.class, "unloaded {}", getSceneName());
    }

    @Override
    public void fixedUpdate(FastJCanvas canvas) {
    }

    @Override
    public void update(FastJCanvas canvas) {
    }
}
