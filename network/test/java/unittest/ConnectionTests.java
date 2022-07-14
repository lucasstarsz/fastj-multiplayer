package unittest;

import tech.fastj.math.Maths;

import java.awt.Point;
import java.io.IOException;
import java.net.DatagramPacket;
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
                public void receiveTCP(byte identifier, Client client) {
                }

                @Override
                public void receiveUDP(DatagramPacket packet, Client client) {
                }
            };

            ClientConfig clientConfig = new ClientConfig(Port, clientListener);
            Client client = new Client(clientConfig);
            client.connect();
            client.sendTCP((byte) 4);
            client.sendUDP(new byte[]{(byte) 4});
        });
    }

    @Test
    void checkSendDataToServer() throws InterruptedException {
        byte tcpIdentifier = (byte) ThreadLocalRandom.current().nextInt(1, Byte.MAX_VALUE);
        Object tcpData = new Point();

        byte udpIdentifier = (byte) ThreadLocalRandom.current().nextInt(1, Byte.MAX_VALUE);
        byte[] udpData = new byte[Client.PacketBufferLength];
        ThreadLocalRandom.current().nextBytes(udpData);
        udpData[0] = udpIdentifier;

        AtomicBoolean receivedTCPData = new AtomicBoolean();
        AtomicBoolean receivedUDPData = new AtomicBoolean();
        CountDownLatch latch = new CountDownLatch(2);

        assertDoesNotThrow(() -> {

            BiConsumer<Byte, Client> serverTCPListener = (identifier, client) -> {
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
                    assertEquals(udpIdentifier, data[0], "The UDP identifier should match.");
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
                public void receiveTCP(byte identifier, Client client) {
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
