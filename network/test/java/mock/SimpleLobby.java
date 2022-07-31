package mock;

import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.Server;
import tech.fastj.network.sessions.Lobby;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleLobby<H extends Enum<H> & CommandAlias> extends Lobby<H> {

    private static final Logger SimpleLobbyLogger = LoggerFactory.getLogger(SimpleLobby.class);

    public SimpleLobby(Server<H> server, String name, Class<H> aliasClass) {
        super(server, 1, name, aliasClass);
    }

    @Override
    public Logger getLogger() {
        return SimpleLobbyLogger;
    }
}
