package tech.fastj.partyhouse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.fastj.network.rpc.Server;
import tech.fastj.network.sessions.Lobby;

public class GameLobby extends Lobby {

    private static final Logger GameLobbyLogger = LoggerFactory.getLogger(GameLobby.class);

    public GameLobby(Server server, String name) {
        super(server, 8, name);
        addSession(new HomeSession(this));
    }

    @Override
    public Logger getLogger() {
        return GameLobbyLogger;
    }
}
