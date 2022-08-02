package mock;

import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.server.Lobby;
import tech.fastj.network.rpc.server.Session;
import tech.fastj.network.serial.Serializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleSession<H extends Enum<H> & CommandAlias> extends Session<H> {

    private static final Logger SimpleSessionLogger = LoggerFactory.getLogger(SimpleSession.class);

    protected SimpleSession(Lobby<H> lobby, String sessionName, Class<H> aliasClass) {
        super(lobby, sessionName, aliasClass);
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
