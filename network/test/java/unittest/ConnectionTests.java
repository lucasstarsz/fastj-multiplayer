package unittest;

import tech.fastj.math.Maths;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import mock.ChatMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.config.ServerConfig;
import tech.fastj.network.rpc.Client;
import tech.fastj.network.rpc.Server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ConnectionTests {

    private static Server server;
    private static final int Port = Maths.randomInteger(10000, 15000);

    @BeforeAll
    static void startServer() throws IOException {
        ServerConfig serverConfig = new ServerConfig(Port);
        server = new Server(serverConfig);
        server.start();
        server.allowClients();
    }

    @AfterAll
    static void stopServer() throws IOException {
        server.stop();
    }

    @AfterEach
    void cleanServer() {
    }

    @Test
    void checkConnectClientToServer() {
        assertDoesNotThrow(() -> {
            ClientConfig clientConfig = new ClientConfig(Port);
            Client client = new Client(clientConfig);

            client.connect();
        });
    }

    @Test
    void checkSendDataToServer() throws InterruptedException {
        ChatMessage tcpData = new ChatMessage(UUID.randomUUID().toString(), System.currentTimeMillis(), UUID.randomUUID().toString());
        ChatMessage udpData = new ChatMessage(UUID.randomUUID().toString(), System.currentTimeMillis(), UUID.randomUUID().toString());

        AtomicBoolean receivedTCPData = new AtomicBoolean();
        AtomicBoolean receivedUDPData = new AtomicBoolean();
        CountDownLatch latch = new CountDownLatch(2);

        assertDoesNotThrow(() -> {
            String ReceiveTCPChatMessage = "Receive TCP Chat Message";
            String ReceiveUDPChatMessage = "Receive UDP Chat Message";

            server.addCommand(ReceiveTCPChatMessage, ChatMessage.class, (client, chatMessage) -> {
                assertEquals(tcpData, chatMessage, "The TCP data should match.");
                receivedTCPData.set(true);
                latch.countDown();
            });

            server.addCommand(ReceiveUDPChatMessage, ChatMessage.class, (client, chatMessage) -> {
                assertEquals(udpData, chatMessage, "The UDP data should match.");
                receivedUDPData.set(true);
                latch.countDown();
            });

            ClientConfig clientConfig = new ClientConfig(Port);
            Client client = new Client(clientConfig);
            client.connect();
            client.getSerializer().registerSerializer(ChatMessage.class);
            client.sendTCP(ReceiveTCPChatMessage, tcpData);
            client.sendUDP(ReceiveUDPChatMessage, udpData);
        });

        boolean success = latch.await(5, TimeUnit.SECONDS);

        if (!success) {
            fail("Server did not receive both TCP and UDP properly. TCP received: " + receivedTCPData.get() + ", UDP received: " + receivedUDPData.get());
        }
    }
}
