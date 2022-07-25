package mock;

import tech.fastj.network.serial.Serializer;
import tech.fastj.network.sessions.Lobby;
import tech.fastj.network.sessions.Session;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleSession extends Session {

    private static final Logger SimpleSessionLogger = LoggerFactory.getLogger(SimpleSession.class);

    protected SimpleSession(Lobby lobby, String sessionName) {
        super(lobby, sessionName, new ArrayList<>());
    }

    @Override
    public Logger getLogger() {
        return SimpleSessionLogger;
    }

    @Override
    public Serializer getSerializer() {
        return serializer;
    }
}
