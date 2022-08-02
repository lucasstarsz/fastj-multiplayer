package tech.fastj.partyhouse;

import tech.fastj.engine.FastJEngine;
import tech.fastj.graphics.display.FastJCanvas;
import tech.fastj.graphics.display.RenderSettings;
import tech.fastj.graphics.display.SimpleDisplay;

import tech.fastj.systems.control.SceneManager;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import tech.fastj.partyhouse.scenes.information.InformationMenu;
import tech.fastj.partyhouse.scenes.mainmenu.MainMenu;
import tech.fastj.partyhouse.scenes.multiplayer.home.LobbyHome;
import tech.fastj.partyhouse.scenes.multiplayer.lobbysearch.LobbySearch;
import tech.fastj.partyhouse.scenes.multiplayer.snowball.SnowballFight;
import tech.fastj.partyhouse.scenes.settings.Settings;
import tech.fastj.partyhouse.user.User;

public class GameManager extends SceneManager {

    private final MainMenu mainMenu = new MainMenu();
    private final InformationMenu informationMenu = new InformationMenu();
    private final Settings settings = new Settings();
    private final LobbySearch lobbySearch = new LobbySearch();
    private final LobbyHome lobbyHome = new LobbyHome();
    private final SnowballFight snowballFight = new SnowballFight();

    @Override
    public void init(FastJCanvas canvas) {
        FastJEngine.getDisplay().getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                var client = User.getInstance().getClient();
                if (client != null) {
                    client.disconnect();
                }

                System.exit(0);
            }
        });

        FastJEngine.<SimpleDisplay>getDisplay().getWindow().setResizable(false);
        canvas.modifyRenderSettings(RenderSettings.Antialiasing.Enable);

        addScenes(mainMenu, settings, informationMenu, lobbySearch, lobbyHome, snowballFight);
        setCurrentScene(mainMenu);
        loadCurrentScene();
    }
}
