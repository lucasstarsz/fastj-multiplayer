package tech.fastj.partyhouse.util;

import tech.fastj.engine.FastJEngine;
import tech.fastj.graphics.display.FastJCanvas;

import tech.fastj.systems.control.GameHandler;
import tech.fastj.systems.control.SceneManager;

import java.awt.Color;

import tech.fastj.gameloop.CoreLoopState;
import tech.fastj.partyhouse.ui.BetterButton;

public class Buttons {

    public static BetterButton menu(GameHandler gameHandler, FastJCanvas canvas, float x, float y, String text, String sceneName) {
        return menu(gameHandler, canvas, x, y, text, sceneName, true);
    }

    public static BetterButton menu(GameHandler gameHandler, FastJCanvas canvas, float x, float y, String text, String sceneName, boolean unload) {
        BetterButton button = create(gameHandler, canvas, x, y, text);

        button.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();

            FastJEngine.runLater(
                () -> FastJEngine.<SceneManager>getLogicManager().switchScenes(sceneName, unload),
                CoreLoopState.LateUpdate
            );
        });

        return button;
    }

    public static BetterButton create(GameHandler gameHandler, FastJCanvas canvas, float x, float y, String text) {
        BetterButton button = new BetterButton(gameHandler, canvas.getCanvasCenter().add(x, y), Shapes.ButtonSize);

        button.setText(text);
        button.setFill(Color.darkGray);
        button.setFont(Fonts.ButtonTextFont);
        button.setOutlineColor(Colors.Snowy);
        button.setTextColor(Colors.Snowy);

        return button;
    }
}
