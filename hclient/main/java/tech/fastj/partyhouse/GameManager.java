package tech.fastj.partyhouse;

import tech.fastj.engine.FastJEngine;
import tech.fastj.graphics.display.FastJCanvas;
import tech.fastj.graphics.display.RenderSettings;
import tech.fastj.graphics.display.SimpleDisplay;
import tech.fastj.partyhouse.scenes.information.InformationMenu;
import tech.fastj.partyhouse.scenes.mainmenu.MainMenu;
import tech.fastj.partyhouse.scenes.settings.Settings;
import tech.fastj.systems.control.SceneManager;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GameManager extends SceneManager {

    private final MainMenu mainMenu = new MainMenu();
    private final InformationMenu informationMenu = new InformationMenu();
    private final Settings settings = new Settings();

    @Override
    public void init(FastJCanvas canvas) {
        FastJEngine.getDisplay().getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        FastJEngine.<SimpleDisplay>getDisplay().getWindow().setResizable(false);
        canvas.modifyRenderSettings(RenderSettings.Antialiasing.Enable);

        addScenes(mainMenu, settings, informationMenu);
        setCurrentScene(mainMenu);
        loadCurrentScene();
    }
}
