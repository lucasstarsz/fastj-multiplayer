package mock;

import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.sessions.Lobby;
import tech.fastj.network.sessions.Session;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleSession<H extends Enum<H> & CommandAlias> extends Session<H> {

    private static final Logger SimpleSessionLogger = LoggerFactory.getLogger(SimpleSession.class);

    protected SimpleSession(Lobby<H> lobby, String sessionName, Class<H> aliasClass) {
        super(lobby, sessionName, new ArrayList<>(), aliasClass);
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
