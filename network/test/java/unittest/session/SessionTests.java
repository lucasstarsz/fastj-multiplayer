package unittest.session;

import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.config.ServerConfig;
import tech.fastj.network.rpc.local.LocalClient;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;
import tech.fastj.network.rpc.server.Lobby;
import tech.fastj.network.rpc.server.Server;
import tech.fastj.network.rpc.server.ServerClient;
import tech.fastj.network.rpc.server.Session;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import mock.EmptyCommands;
import mock.SingleSessionLobby;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class SessionTests {

    private static final int Port = 19999;
    private static final InetAddress ClientTargetAddress;
    private static Server<EmptyCommands> server;
    private static final BiFunction<ServerClient<EmptyCommands>, String, Lobby<EmptyCommands>> LobbyCreator = (client, serverName) -> new SingleSessionLobby<>(server, serverName, EmptyCommands.class);

    static {
        try {
            ClientTargetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void cleanServer() {
        server.stopAllLobbies();
        server.disconnectAllClients();
    }

    @Test
    void checkClientIsAddedToSession() throws InterruptedException {
        String randomLobbyName = UUID.randomUUID().toString();
        CountDownLatch latch = new CountDownLatch(1);

        assertDoesNotThrow(() -> {
            ClientConfig clientConfig = new ClientConfig(ClientTargetAddress, Port);
            LocalClient<EmptyCommands> client = new LocalClient<>(clientConfig, EmptyCommands.class);
            client.connect();

            LobbyIdentifier newLobby = client.createLobby(randomLobbyName);

            ServerClient<EmptyCommands> serverClient = server.getClient(client.getClientId());
            assertNotNull(serverClient);

            Lobby<EmptyCommands> lobby = server.getLobby(serverClient);
            assertNotNull(lobby);
            assertEquals(newLobby, lobby.getLobbyIdentifier());

            Session<EmptyCommands> clientSession = lobby.getClientSession(serverClient);
            assertNotNull(clientSession);

            assertEquals(lobby.getCurrentSession().getSessionId(), clientSession.getSessionId());
            assertEquals(lobby.getHomeSession().getSessionId(), clientSession.getSessionId());

            latch.countDown();
        });

        boolean success = latch.await(5, TimeUnit.SECONDS);

        if (!success) {
            fail("Server failed to create lobby");
        }
    }

    @BeforeAll
    static void startServer() throws IOException {
        ServerConfig serverConfig = new ServerConfig(Port);

        server = new Server<>(serverConfig, EmptyCommands.class, LobbyCreator);
        server.start();
        server.allowClients();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }
}
