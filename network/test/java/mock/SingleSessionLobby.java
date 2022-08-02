package mock;

import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.server.Lobby;
import tech.fastj.network.rpc.server.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleSessionLobby<H extends Enum<H> & CommandAlias> extends Lobby<H> {

    private static final Logger LobbyWithSessionsLogger = LoggerFactory.getLogger(SingleSessionLobby.class);

    public SingleSessionLobby(Server<H> server, String lobbyName, Class<H> aliasClass) {
        super(server, 10, lobbyName, aliasClass);

        SimpleSession<H> session = new SimpleSession<>(this, lobbyName + "_Session1", aliasClass);

        addSession(session);
        setCurrentSession(session);
        setHomeSessionId(session.getSessionId());
    }

    @Override
    public Logger getLogger() {
        return LobbyWithSessionsLogger;
    }
}
