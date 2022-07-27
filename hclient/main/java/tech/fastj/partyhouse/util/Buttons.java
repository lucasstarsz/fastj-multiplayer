package tech.fastj.partyhouse.util;

import tech.fastj.engine.FastJEngine;
import tech.fastj.graphics.display.FastJCanvas;

import tech.fastj.systems.control.Scene;
import tech.fastj.systems.control.SceneManager;

import java.awt.Color;

import tech.fastj.partyhouse.ui.BetterButton;

public class Buttons {

    public static BetterButton menu(Scene scene, FastJCanvas canvas, float x, float y, String text, String sceneName) {
        return menu(scene, canvas, x, y, text, sceneName, true);
    }

    public static BetterButton menu(Scene scene, FastJCanvas canvas, float x, float y, String text, String sceneName, boolean unload) {
        BetterButton button = create(scene, canvas, x, y, text);

        button.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            FastJEngine.runAfterRender(() -> FastJEngine.<SceneManager>getLogicManager().switchScenes(sceneName, unload));
        });

        return button;
    }

    public static BetterButton create(Scene scene, FastJCanvas canvas, float x, float y, String text) {
        BetterButton button = new BetterButton(scene, canvas.getCanvasCenter().add(x, y), Shapes.ButtonSize);

        button.setText(text);
        button.setFill(Color.darkGray);
        button.setFont(Fonts.ButtonTextFont);
        button.setOutlineColor(Colors.Snowy);
        button.setTextColor(Colors.Snowy);

        return button;
    }
}
