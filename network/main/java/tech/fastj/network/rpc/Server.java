package tech.fastj.network.rpc;

import tech.fastj.network.config.ServerConfig;
import tech.fastj.network.rpc.message.RequestType;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;
import tech.fastj.network.serial.read.MessageInputStream;
import tech.fastj.network.sessions.Lobby;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server extends CommandHandler<ServerClient> {

    private final List<ServerClient> allClients;
    private final Map<UUID, Lobby> lobbies;
    private final BiFunction<ServerClient, String, Lobby> lobbyCreator;

    private final ServerSocket tcpServer;
    private final DatagramSocket udpServer;

    private ExecutorService clientAccepter;

    private volatile boolean isRunning;
    private volatile boolean isAcceptingClients;

    private final Logger serverLogger = LoggerFactory.getLogger(Server.class);

    public Server(ServerConfig serverConfig, BiFunction<ServerClient, String, Lobby> lobbyCreator) throws IOException {
        this.allClients = new ArrayList<>(serverConfig.maxClients());
        this.lobbies = new HashMap<>();
        this.lobbyCreator = lobbyCreator;

        tcpServer = new ServerSocket(serverConfig.port(), serverConfig.clientBacklog(), serverConfig.address());
        udpServer = new DatagramSocket(serverConfig.port(), serverConfig.address());
    }

    public List<ServerClient> getClients() {
        return Collections.unmodifiableList(allClients);
    }

    public Map<UUID, Lobby> getLobbies() {
        return lobbies;
    }

    public ServerSocket getTcpServer() {
        return tcpServer;
    }

    public DatagramSocket getUdpServer() {
        return udpServer;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isAcceptingClients() {
        return isAcceptingClients;
    }

    public void disconnectAllClients() {
        for (int i = allClients.size() - 1; i >= 0; i--) {
            allClients.get(i).disconnect();
        }

        allClients.clear();
    }

    public void stopAllLobbies() {
        for (Lobby lobby : lobbies.values()) {
            lobby.stop();
        }

        lobbies.clear();
    }

    public void stop() throws IOException {
        serverLogger.debug("stopping server");

        disallowClients();
        stopAllLobbies();
        disconnectAllClients();

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

    private ServerClient getClient(UUID senderId) {
        for (ServerClient client : allClients) {
            if (senderId.equals(client.clientId)) {
                return client;
            }
        }

        return null;
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
        ServerClient client = null;

        try {
            serverLogger.debug("Accepting new client...");

            Socket clientSocket = tcpServer.accept();

            serverLogger.debug("Received new client, creating connection...");

            client = new ServerClient(clientSocket, this, udpServer);
            client.connect();

            serverLogger.debug("Client {} connected.", client.getClientId());

            client.run();
            allClients.add(client);
        } catch (IOException exception) {
            if (client != null && client.isConnected()) {
                client.getTcpOut().writeInt(Client.Leave);
                client.getTcpOut().flush();
                client.disconnect();
            }

            throw new IOException(
                    "Unable to connect to client "
                            + (client != null ? client.getClientId() : "(null)"),
                    exception
            );
        }
    }

    public void start() {
        isRunning = true;
    }

    public void receiveCommand(UUID commandId, UUID senderId, MessageInputStream stream) throws IOException {
        ServerClient client = getClient(senderId);
        readCommand(commandId, stream, client);
    }

    public void returnAvailableLobbies(ServerClient client) throws IOException {
        LobbyIdentifier[] lobbyIdentifiers = getLobbies()
                .values()
                .stream()
                .map(Lobby::getLobbyIdentifier)
                .toArray(LobbyIdentifier[]::new);

        client.getSerializer().registerSerializer(LobbyIdentifier.class);

        client.getTcpOut().writeArray(lobbyIdentifiers);
        client.getTcpOut().flush();
    }

    public void createLobby(ServerClient client, String lobbyName) throws IOException {
        serverLogger.info("Creating lobby for client {}, named {}", client.getClientId(), lobbyName);
        Lobby lobby = lobbyCreator.apply(client, lobbyName);
        lobbies.put(lobby.getLobbyIdentifier().id(), lobby);

        client.getSerializer().registerSerializer(LobbyIdentifier.class);

        client.getTcpOut().writeObject(lobby.getLobbyIdentifier(), LobbyIdentifier.class);
        client.getTcpOut().flush();
    }

    public void joinLobby(ServerClient client, UUID lobbyId) throws IOException {
        Lobby lobby = lobbies.get(lobbyId);

        if (lobby == null) {
            return;
        }

        lobby.receiveNewClient(client);
        client.setOnDisconnect(lobby::clientDisconnect);

        serverLogger.info("Client {} joining lobby {}", client.getClientId(), lobby.getLobbyIdentifier().name());

        client.getSerializer().registerSerializer(LobbyIdentifier.class);

        client.getTcpOut().writeObject(lobby.getLobbyIdentifier(), LobbyIdentifier.class);
        client.getTcpOut().flush();
    }

    public void receiveRequest(RequestType requestType, UUID senderId, MessageInputStream inputStream)
            throws IOException {
        ServerClient client = getClient(senderId);

        if (client == null) {
            return;
        }

        switch (requestType) {
            case GetAvailableLobbies -> returnAvailableLobbies(client);
            case CreateLobby -> createLobby(client, (String) inputStream.readObject(String.class));
            case JoinLobby -> joinLobby(client, (UUID) inputStream.readObject(UUID.class));
        }
    }
}
