package unittest;

import tech.fastj.math.Maths;

import java.awt.Point;
import java.io.IOException;
import java.net.DatagramPacket;
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
                public void receiveTCP(int identifier, Client client) {
                }

                @Override
                public void receiveUDP(DatagramPacket packet, Client client) {
                }
            };

            ClientConfig clientConfig = new ClientConfig(Port, clientListener);
            Client client = new Client(clientConfig);
            client.connect();
            client.sendTCP(4);
            client.sendUDP(ByteBuffer.allocate(4).put(3, (byte) 4).array());
        });
    }

    @Test
    void checkSendDataToServer() throws InterruptedException {
        int tcpIdentifier = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        Object tcpData = new Point();

        int udpIdentifier = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        byte[] randomUdpData = new byte[Client.PacketBufferLength - 4];
        ThreadLocalRandom.current().nextBytes(randomUdpData);

        byte[] udpData = ByteBuffer.allocate(Client.PacketBufferLength)
                .putInt(udpIdentifier)
                .put(randomUdpData)
                .array();

        AtomicBoolean receivedTCPData = new AtomicBoolean();
        AtomicBoolean receivedUDPData = new AtomicBoolean();
        CountDownLatch latch = new CountDownLatch(2);

        assertDoesNotThrow(() -> {

            BiConsumer<Integer, Client> serverTCPListener = (identifier, client) -> {
                assertEquals(tcpIdentifier, identifier, "The TCP identifier should match.");

                try {
                    Object data = client.getTcpIn().readObject();
                    assertEquals(tcpData, data, "The TCP data should match.");
                } catch (IOException | ClassNotFoundException exception) {
                    fail(exception);
                } finally {
                    receivedTCPData.set(true);
                    latch.countDown();
                }
            };

            BiConsumer<DatagramPacket, Client> serverUDPListener = (packet, client) -> {
                try {
                    byte[] data = packet.getData();
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
                public void receiveTCP(int identifier, Client client) {
                }

                @Override
                public void receiveUDP(DatagramPacket packet, Client client) {
                }
            };

            ClientConfig clientConfig = new ClientConfig(Port, clientListener);
            Client client = new Client(clientConfig);
            client.connect();
            client.sendTCP(tcpIdentifier, tcpData);
            client.sendUDP(udpData);
        });

        boolean success = latch.await(5, TimeUnit.SECONDS);

        if (!success) {
            fail("Server did not receive both TCP and UDP properly. TCP received: " + receivedTCPData.get() + ", UDP received: " + receivedUDPData.get());
        }
    }
}
