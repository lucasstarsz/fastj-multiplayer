package unittest.session;

import mock.SingleSessionLobby;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.config.ServerConfig;
import tech.fastj.network.rpc.Client;
import tech.fastj.network.rpc.Server;
import tech.fastj.network.rpc.ServerClient;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;
import tech.fastj.network.sessions.Lobby;
import tech.fastj.network.sessions.Session;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class SessionTests {

    private static Server server;
    private static final int Port = 19999;
    private static final InetAddress ClientTargetAddress;

    static {
        try {
            ClientTargetAddress = InetAddress.getByName("partyhouse.lucasz.tech");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private static final BiFunction<ServerClient, String, Lobby> LobbyCreator = (client, serverName) -> new SingleSessionLobby(server, serverName);

    @BeforeAll
    static void startServer() throws IOException {
        ServerConfig serverConfig = new ServerConfig(Port);

        server = new Server(serverConfig, LobbyCreator);
        server.start();
        server.allowClients();
    }

    @AfterEach
    void cleanServer() {
        server.stopAllLobbies();
        server.disconnectAllClients();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @Test
    void checkClientIsAddedToSession() throws InterruptedException {
        String randomLobbyName = UUID.randomUUID().toString();
        CountDownLatch latch = new CountDownLatch(1);

        assertDoesNotThrow(() -> {
            ClientConfig clientConfig = new ClientConfig(ClientTargetAddress, Port);
            Client client = new Client(clientConfig);
            client.connect();

            LobbyIdentifier newLobby = client.createLobby(randomLobbyName);

            ServerClient serverClient = server.getClient(client.getClientId());
            assertNotNull(serverClient);

            Lobby lobby = server.getLobby(serverClient);
            assertNotNull(lobby);
            assertEquals(newLobby, lobby.getLobbyIdentifier());

            Session clientSession = lobby.getClientSession(serverClient);
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
}
