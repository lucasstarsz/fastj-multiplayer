package mock;

import tech.fastj.network.rpc.Server;
import tech.fastj.network.sessions.Lobby;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleSessionLobby extends Lobby {

    private static final Logger LobbyWithSessionsLogger = LoggerFactory.getLogger(SingleSessionLobby.class);

    public SingleSessionLobby(Server server, String lobbyName) {
        super(server, 10, lobbyName);

        SimpleSession session = new SimpleSession(this, lobbyName + "_Session1");
        addSession(session);
    }

    @Override
    public Logger getLogger() {
        return LobbyWithSessionsLogger;
    }
}
