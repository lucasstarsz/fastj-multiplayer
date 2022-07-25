package mock;

import tech.fastj.network.rpc.Server;
import tech.fastj.network.sessions.Lobby;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleLobby extends Lobby {

    private static final Logger SimpleLobbyLogger = LoggerFactory.getLogger(SimpleLobby.class);

    public SimpleLobby(Server server, String name) {
        super(server, 1, name);
    }

    @Override
    public Logger getLogger() {
        return SimpleLobbyLogger;
    }
}
