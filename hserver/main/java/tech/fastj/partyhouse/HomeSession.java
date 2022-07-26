package tech.fastj.partyhouse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.fastj.network.sessions.Lobby;
import tech.fastj.network.sessions.Session;

import java.util.ArrayList;

public class HomeSession extends Session {

    private static final Logger HomeSessionLogger = LoggerFactory.getLogger(HomeSession.class);
    public static final String Name = "Home";

    protected HomeSession(Lobby lobby) {
        super(lobby, Name, new ArrayList<>());
    }

    @Override
    public Logger getLogger() {
        return HomeSessionLogger;
    }
}
