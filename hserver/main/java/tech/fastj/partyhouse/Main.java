package tech.fastj.partyhouse;

import tech.fastj.network.config.ServerConfig;
import tech.fastj.network.rpc.server.Server;
import tech.fastj.network.rpc.server.ServerClient;
import tech.fastj.network.rpc.server.Lobby;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import tech.fastj.partyhousecore.Commands;
import tech.fastj.partyhousecore.Info;

public class Main {
    public static void main(String[] args) {
        AtomicReference<Server<Commands>> server = new AtomicReference<>();

        try {
            ServerConfig serverConfig = new ServerConfig(Info.DefaultPort);
            BiFunction<ServerClient<Commands>, String, Lobby<Commands>> lobbyCreator = (serverClient, lobbyName) -> new GameLobby(server.get(), lobbyName);
            server.set(new Server<>(serverConfig, Commands.class, lobbyCreator));

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
