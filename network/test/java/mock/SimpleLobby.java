package mock;

import tech.fastj.network.rpc.Server;
import tech.fastj.network.rpc.ServerClient;
import tech.fastj.network.sessions.Lobby;

public class SimpleLobby extends Lobby {
    public SimpleLobby(Server server, String name) {
        super(server, 1, name);
    }

    public void addClient(ServerClient serverClient) {
        clients.add(serverClient);
    }
}
