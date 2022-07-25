package unittest.session;

import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.config.ServerConfig;
import tech.fastj.network.rpc.Client;
import tech.fastj.network.rpc.Server;
import tech.fastj.network.rpc.ServerClient;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;
import tech.fastj.network.sessions.Lobby;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import mock.SimpleLobby;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LobbyTests {

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

    private static final BiFunction<ServerClient, String, Lobby> LobbyCreator = (client, serverName) -> {
        SimpleLobby lobby = new SimpleLobby(server, serverName);
        lobby.addClient(client);

        return lobby;
    };

    @BeforeAll
    static void startServer() throws IOException {
        ServerConfig serverConfig = new ServerConfig(Port);

        server = new Server(serverConfig, LobbyCreator);
        server.start();
        server.allowClients();
    }

    @AfterEach
    void cleanServer() {
        System.out.println("stop lobbies");
        server.stopAllLobbies();
        System.out.println("disconnect");
        server.disconnectAllClients();
    }

    @AfterAll
    static void stopServer() throws IOException {
        server.stop();
    }

    @Test
    void checkCreateLobby() throws InterruptedException {
        String randomLobbyName = UUID.randomUUID().toString();
        CountDownLatch latch = new CountDownLatch(1);

        assertDoesNotThrow(() -> {
            ClientConfig clientConfig = new ClientConfig(ClientTargetAddress, Port);
            Client client = new Client(clientConfig);
            client.connect();

            LobbyIdentifier newLobby = client.createLobby(randomLobbyName);
            LobbyIdentifier[] allLobbies = client.getAvailableLobbies();

            assertEquals(randomLobbyName, newLobby.name());
            assertEquals(server.getLobbies().get(newLobby.id()).getLobbyIdentifier(), newLobby);
            assertEquals(server.getLobbies().size(), allLobbies.length);

            assertEquals(1, server.getLobbies().get(newLobby.id()).getClients().size());
            assertTrue(server.getLobbies().get(newLobby.id()).hasClient(client.getClientId()));

            latch.countDown();
        });

        boolean success = latch.await(5, TimeUnit.SECONDS);

        if (!success) {
            fail("Server failed to create lobby");
        }
    }

    @Test
    void checkJoinLobby() throws InterruptedException {
        String randomLobbyName = UUID.randomUUID().toString();
        CountDownLatch latch = new CountDownLatch(1);

        assertDoesNotThrow(() -> {
            ClientConfig clientConfig = new ClientConfig(ClientTargetAddress, Port);
            Client client1 = new Client(clientConfig);
            client1.connect();

            LobbyIdentifier newLobby = client1.createLobby(randomLobbyName);

            Client client2 = new Client(clientConfig);
            client2.connect();

            LobbyIdentifier joinedLobby = client2.joinLobby(newLobby.id());

            LobbyIdentifier[] allLobbies1 = client1.getAvailableLobbies();
            LobbyIdentifier[] allLobbies2 = client1.getAvailableLobbies();

            assertEquals(newLobby, joinedLobby);
            assertArrayEquals(allLobbies1, allLobbies2);

            assertEquals(2, server.getLobbies().get(newLobby.id()).getClients().size());
            assertTrue(server.getLobbies().get(newLobby.id()).hasClient(client1.getClientId()));
            assertTrue(server.getLobbies().get(newLobby.id()).hasClient(client2.getClientId()));

            latch.countDown();
        });

        boolean success = latch.await(5, TimeUnit.SECONDS);

        if (!success) {
            fail("Server failed to create lobby");
        }
    }
}
