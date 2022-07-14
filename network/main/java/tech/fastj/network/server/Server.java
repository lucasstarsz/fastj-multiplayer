package tech.fastj.network.server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.fastj.network.client.Client;
import tech.fastj.network.client.ClientListener;

public class Server implements ClientListener {

    private final List<Client> clients;

    private final ServerSocket tcpServer;
    private final DatagramSocket udpServer;

    private final Map<Integer, BiConsumer<byte[], Client>> tcpActions;
    private final Map<Integer, BiConsumer<byte[], Client>> udpActions;

    private ExecutorService clientAccepter;

    private volatile boolean isRunning;
    private volatile boolean isAcceptingClients;

    private final Logger serverLogger = LoggerFactory.getLogger(Server.class);

    public Server(ServerConfig serverConfig) throws IOException {
        this.clients = new ArrayList<>(serverConfig.maxClients());

        tcpServer = new ServerSocket(serverConfig.port(), serverConfig.clientBacklog(), serverConfig.address());
        udpServer = new DatagramSocket(serverConfig.port(), serverConfig.address());

        tcpActions = new HashMap<>();
        udpActions = new HashMap<>();
    }

    public List<Client> getClients() {
        return Collections.unmodifiableList(clients);
    }

    public Map<Integer, BiConsumer<byte[], Client>> getTcpActions() {
        return tcpActions;
    }

    public Map<Integer, BiConsumer<byte[], Client>> getUdpActions() {
        return udpActions;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isAcceptingClients() {
        return isAcceptingClients;
    }

    public void stop() throws IOException {
        serverLogger.debug("stopping server");

        disallowClients();

        tcpServer.close();
        udpServer.close();

        isRunning = false;
    }

    public void allowClients() {
        if (!isRunning) {
            serverLogger.debug("Server not running.");
            return;
        }

        if (isAcceptingClients) {
            return;
        }

        if (clientAccepter == null || clientAccepter.isShutdown()) {
            clientAccepter = Executors.newSingleThreadExecutor();
        }

        isAcceptingClients = true;
        clientAccepter.submit(this::acceptClients);
    }

    public void disallowClients() {
        if (!isAcceptingClients) {
            return;
        }

        if (clientAccepter != null && !clientAccepter.isShutdown()) {
            clientAccepter.shutdownNow();
        }

        isAcceptingClients = false;
        serverLogger.debug("Server no longer accepting clients.");
    }

    private void acceptClients() {
        serverLogger.debug("Now accepting clients...");

        while (isAcceptingClients) {
            try {
                acceptClient();
            } catch (IOException exception) {
                if (!isRunning || !isAcceptingClients) {
                    break;
                }

                serverLogger.error("Failed to accept new client", exception);
            }
        }

        disallowClients();
    }

    private synchronized void acceptClient() throws IOException {
        serverLogger.debug("Accepting new client...");

        Socket clientSocket = tcpServer.accept();

        serverLogger.debug("Received new client, creating connection...");

        Client client = new Client(clientSocket, this, udpServer);
        client.connect();

        serverLogger.debug(
                "Successful connection on {} to {}:{}. Sending success...",
                client.getClientId(),
                client.getClientConfig().address(),
                client.getClientConfig().port()
        );

        clients.add(client);
        client.sendTCP(Client.Join);

        serverLogger.debug("client {} connected.", client.getClientId());

        client.run();
    }

    @Override
    public void receiveTCP(byte[] data, Client client) {
        int identifier = ByteBuffer.wrap(data).getInt();
        serverLogger.debug("Received TCP identifier {} from client {}", identifier, client.getClientId());
        tcpActions.getOrDefault(identifier, (i, c) -> {}).accept(data, client);
    }

    @Override
    public void receiveUDP(byte[] data, Client client) {
        int identifier = ByteBuffer.wrap(data).getInt();
        serverLogger.debug("Received UDP identifier {} from client {}", identifier, client.getClientId());
        udpActions.getOrDefault(identifier, (p, c) -> {}).accept(data, client);
    }

    public void start() {
        isRunning = true;
    }
}
