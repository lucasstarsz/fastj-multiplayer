package unittest;

import tech.fastj.math.Maths;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tech.fastj.network.client.Client;
import tech.fastj.network.client.ClientConfig;
import tech.fastj.network.client.ClientListener;
import tech.fastj.network.server.Server;
import tech.fastj.network.server.ServerConfig;

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
    public void cleanServer() {
        server.getTcpActions().clear();
        server.getUdpActions().clear();
    }

    @Test
    void checkConnectClientToServer() {
        assertDoesNotThrow(() -> {
            ClientListener clientListener = new ClientListener() {
                @Override
                public void receiveTCP(byte[] data, Client client) {
                }

                @Override
                public void receiveUDP(byte[] data, Client client) {
                }
            };

            ClientConfig clientConfig = new ClientConfig(Port, clientListener);
            Client client = new Client(clientConfig);
            client.connect();
            client.sendTCP(4);
            client.sendUDP(4);
        });
    }

    @Test
    void checkSendDataToServer() throws InterruptedException {
        int tcpIdentifier = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        byte[] tcpData = new byte[Client.PacketBufferLength - Integer.BYTES];
        ThreadLocalRandom.current().nextBytes(tcpData);

        int udpIdentifier = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        byte[] udpData = new byte[Client.PacketBufferLength - Integer.BYTES];
        ThreadLocalRandom.current().nextBytes(udpData);

        AtomicBoolean receivedTCPData = new AtomicBoolean();
        AtomicBoolean receivedUDPData = new AtomicBoolean();
        CountDownLatch latch = new CountDownLatch(2);

        assertDoesNotThrow(() -> {

            BiConsumer<byte[], Client> serverTCPListener = (data, client) -> {
                try {
                    assertEquals(udpIdentifier, ByteBuffer.wrap(data).getInt(), "The TCP identifier should match.");
                    assertEquals(udpData, data, "The UDP data should match.");
                } finally {
                    receivedTCPData.set(true);
                    latch.countDown();
                }
            };

            BiConsumer<byte[], Client> serverUDPListener = (data, client) -> {
                try {
                    assertEquals(udpIdentifier, ByteBuffer.wrap(data).getInt(), "The UDP identifier should match.");
                    assertEquals(udpData, data, "The UDP data should match.");
                } finally {
                    receivedUDPData.set(true);
                    latch.countDown();
                }
            };

            server.getTcpActions().put(tcpIdentifier, serverTCPListener);
            server.getUdpActions().put(udpIdentifier, serverUDPListener);

            ClientListener clientListener = new ClientListener() {
                @Override
                public void receiveTCP(byte[] data, Client client) {
                }

                @Override
                public void receiveUDP(byte[] data, Client client) {
                }
            };

            ClientConfig clientConfig = new ClientConfig(Port, clientListener);
            Client client = new Client(clientConfig);
            client.connect();
            client.sendTCP(tcpIdentifier, tcpData);
            client.sendUDP(udpIdentifier, udpData);
        });

        boolean success = latch.await(5, TimeUnit.SECONDS);

        if (!success) {
            fail("Server did not receive both TCP and UDP properly. TCP received: " + receivedTCPData.get() + ", UDP received: " + receivedUDPData.get());
        }
    }
}
