package tech.fastj.network.rpc;

import tech.fastj.network.config.ServerConfig;
import tech.fastj.network.serial.read.NetworkableInputStream;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server extends CommandHandler {

    private final List<Client> clients;

    private final ServerSocket tcpServer;
    private final DatagramSocket udpServer;

    private ExecutorService clientAccepter;

    private volatile boolean isRunning;
    private volatile boolean isAcceptingClients;

    private final Logger serverLogger = LoggerFactory.getLogger(Server.class);

    public Server(ServerConfig serverConfig) throws IOException {
        this.clients = new ArrayList<>(serverConfig.maxClients());

        tcpServer = new ServerSocket(serverConfig.port(), serverConfig.clientBacklog(), serverConfig.address());
        udpServer = new DatagramSocket(serverConfig.port(), serverConfig.address());
    }

    public List<Client> getClients() {
        return Collections.unmodifiableList(clients);
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
        Client client = null;

        try {
            serverLogger.debug("Accepting new client...");

            Socket clientSocket = tcpServer.accept();

            serverLogger.debug("Received new client, creating connection...");

            client = new Client(clientSocket, this, udpServer);
            client.connect();

            serverLogger.debug(
                    "Successful connection on {} to {}:{}. Sending success...",
                    client.getClientId(),
                    client.getClientConfig().address(),
                    client.getClientConfig().port()
            );

            client.getTcpOut().writeInt(Client.Join);
            client.getTcpOut().flush();

            serverLogger.debug("Client {} connected.", client.getClientId());

            client.run();
            clients.add(client);
        } catch (IOException exception) {
            if (client != null && client.isConnected()) {
                client.getTcpOut().writeInt(Client.Leave);
                client.getTcpOut().flush();
                client.disconnect();
            }

            throw new IOException(
                    "Unable to connect to client"
                            + (client != null ? " " + client.getClientId() : ""),
                    exception
            );
        }
    }

    public void start() {
        isRunning = true;
    }

    public void receiveCommand(UUID commandId, Client client, NetworkableInputStream stream) throws IOException {
        readCommand(commandId, stream, client);
    }
}
