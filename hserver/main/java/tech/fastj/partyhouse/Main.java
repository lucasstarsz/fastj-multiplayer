package tech.fastj.partyhouse;

import tech.fastj.network.config.ServerConfig;
import tech.fastj.network.rpc.Server;
import tech.fastj.network.rpc.ServerClient;
import tech.fastj.network.sessions.Lobby;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

public class Main {
    public static void main(String[] args) {
        AtomicReference<Server> server = new AtomicReference<>();

        try {
            int port = 19999;
            ServerConfig serverConfig = new ServerConfig(port);
            BiFunction<ServerClient, String, Lobby> lobbyCreator = (serverClient, lobbyName) -> new GameLobby(server.get(), lobbyName);
            server.set(new Server(serverConfig, lobbyCreator));

            server.get().start();
            server.get().allowClients();
            server.get().createLobby("Test");
            server.get().createLobby("Test 2");
            server.get().createLobby("Test 3");
            server.get().createLobby("Test 4");
        } catch (IOException exception) {
            server.get().stop();
        }
    }
}
