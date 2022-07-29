package tech.fastj.partyhouse.scenes.mainmenu;

import tech.fastj.engine.FastJEngine;
import tech.fastj.logging.Log;
import tech.fastj.math.Pointf;
import tech.fastj.math.Transform2D;
import tech.fastj.graphics.dialog.DialogConfig;
import tech.fastj.graphics.display.FastJCanvas;
import tech.fastj.graphics.game.Text2D;

import tech.fastj.systems.control.Scene;

import javax.swing.SwingUtilities;
import java.net.InetAddress;
import java.net.UnknownHostException;

import tech.fastj.gameloop.CoreLoopState;
import tech.fastj.partyhouse.Main;
import tech.fastj.partyhouse.ui.BetterButton;
import tech.fastj.partyhouse.user.User;
import tech.fastj.partyhouse.util.Buttons;
import tech.fastj.partyhouse.util.Colors;
import tech.fastj.partyhouse.util.Dialogs;
import tech.fastj.partyhouse.util.Fonts;
import tech.fastj.partyhouse.util.SceneNames;
import tech.fastj.partyhouse.util.Shapes;

public class MainMenu extends Scene {

    public MainMenu() {
        super(SceneNames.MainMenu);
    }

    @Override
    public void load(FastJCanvas canvas) {
        Log.debug(MainMenu.class, "loading {}", getSceneName());
        Pointf center = canvas.getCanvasCenter();

        Text2D titleText = Text2D.create(Main.GameName)
            .withFill(Colors.Snowy)
            .withFont(Fonts.TitleTextFont)
            .withTransform(Pointf.subtract(center, 225f, 200f), Transform2D.DefaultRotation, Transform2D.DefaultScale)
            .build();
        drawableManager().addGameObject(titleText);

        // keep -- menu button's functionality is fully declared in Buttons.menu(...);
        BetterButton playButton = Buttons.menu(this, canvas, -(Shapes.ButtonSize.x * 1.25f), -50f, "Play Game", SceneNames.LobbySearch, false);
        BetterButton infoButton = Buttons.menu(this, canvas, (Shapes.ButtonSize.x * 0.25f), -50f, "Information", SceneNames.Information, false);
//        settingsButton = Buttons.menu(this, canvas, (Shapes.ButtonSize.x * 0.25f), -50f, "Settings", SceneNames.Settings, false);

        BetterButton setServerIPButton = Buttons.create(this, canvas, -(Shapes.ButtonSize.x * 1.25f), 50f, "Set Server IP");
        setServerIPButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            FastJEngine.runLater(() -> {
                String newIp = Dialogs.userInput(
                    DialogConfig.create()
                        .withParentComponent(FastJEngine.getDisplay().getWindow())
                        .withTitle("Set Custom Server IP")
                        .withPrompt("Please set a custom server IP Address.")
                        .build()
                );

                if (newIp == null) {
                    return;
                }

                try {
                    InetAddress newIpAddress = InetAddress.getByName(newIp);
                    User.getInstance().setCustomIp(newIpAddress);
                } catch (UnknownHostException exception) {
                    Dialogs.message(
                        DialogConfig.create()
                            .withParentComponent(FastJEngine.getDisplay().getWindow())
                            .withTitle("Invalid Custom Server IP")
                            .withPrompt("COuld not set the custom server IP to this value: " + exception.getMessage())
                            .build()
                    );
                }
            }, CoreLoopState.LateUpdate);
        });

        BetterButton exitButton = Buttons.create(this, canvas, (Shapes.ButtonSize.x * 0.25f), 50f, "Quit Game");
        exitButton.setOnAction(mouseButtonEvent -> {
            mouseButtonEvent.consume();
            SwingUtilities.invokeLater(FastJEngine::forceCloseGame);
        });

        Log.debug(MainMenu.class, "loaded {}", getSceneName());
    }

    @Override
    public void unload(FastJCanvas canvas) {
        Log.debug(MainMenu.class, "unloading {}", getSceneName());
        Log.debug(MainMenu.class, "unloaded {}", getSceneName());
    }

    @Override
    public void fixedUpdate(FastJCanvas canvas) {
    }

    @Override
    public void update(FastJCanvas canvas) {
    }
}
