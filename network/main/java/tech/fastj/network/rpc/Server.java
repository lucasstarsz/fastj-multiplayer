package tech.fastj.network.rpc;

import tech.fastj.network.config.ServerConfig;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.RequestType;
import tech.fastj.network.rpc.message.SentMessageType;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;
import tech.fastj.network.serial.read.MessageInputStream;
import tech.fastj.network.sessions.Lobby;
import tech.fastj.network.sessions.Session;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
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
        this.lobbies = new LinkedHashMap<>();
        this.lobbyCreator = lobbyCreator;

        tcpServer = new ServerSocket(serverConfig.port(), serverConfig.clientBacklog(), serverConfig.address());
        udpServer = new DatagramSocket(serverConfig.port(), serverConfig.address());
    }

    public List<ServerClient> getClients() {
        return Collections.unmodifiableList(allClients);
    }

    public Map<UUID, Lobby> getLobbies() {
        return Collections.unmodifiableMap(lobbies);
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

    @Override
    public Logger getLogger() {
        return serverLogger;
    }

    public Lobby getLobby(ServerClient client) {
        for (Lobby lobby : lobbies.values()) {
            if (lobby.hasClient(client)) {
                return lobby;
            }
        }

        return null;
    }

    public void disconnectAllClients() {
        serverLogger.debug("disconnecting {} clients", allClients.size());

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

    public void stop() {
        serverLogger.debug("stopping server");

        disallowClients();
        stopAllLobbies();
        disconnectAllClients();

        try {
            tcpServer.close();
            udpServer.close();
        } catch (IOException exception) {
            serverLogger.error("Issue while closing server sockets", exception);
        }

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

    public ServerClient getClient(UUID senderId) {
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

    public void receiveCommand(CommandTarget commandTarget, long dataLength, UUID commandId, UUID senderId, MessageInputStream stream)
            throws IOException {
        ServerClient client = getClient(senderId);

        if (client == null) {
            return;
        }

        switch (commandTarget) {
            case Client -> client.readCommand(dataLength, commandId, stream, client);
            case Server -> readCommand(dataLength, commandId, stream, client);
            case Lobby -> {
                Lobby lobby = getLobby(client);

                if (lobby == null) {
                    serverLogger.warn("Couldn't find {}'s lobby to send command {}", senderId, commandId);
                    stream.skipNBytes(dataLength);

                    return;
                }

                lobby.readCommand(dataLength, commandId, stream, client);
            }
            case Session -> {
                Lobby lobby = getLobby(client);

                if (lobby == null) {
                    serverLogger.warn("Couldn't find {}'s lobby to send command {}", senderId, commandId);
                    stream.skipNBytes(dataLength);

                    return;
                }

                Session session = lobby.getClientSession(client);

                if (session == null) {
                    serverLogger.warn("Couldn't find {}'s session to send command {}", senderId, commandId);
                    stream.skipNBytes(dataLength);

                    return;
                }

                session.readCommand(dataLength, commandId, stream, client);
            }
        }
    }

    public void returnAvailableLobbies(ServerClient client) throws IOException {
        LobbyIdentifier[] lobbyIdentifiers = getLobbies()
                .values()
                .stream()
                .map(Lobby::getLobbyIdentifier)
                .toArray(LobbyIdentifier[]::new);

        client.getTcpOut().writeObject(SentMessageType.AvailableLobbiesUpdate, SentMessageType.class);
        client.getTcpOut().writeArray(lobbyIdentifiers);
        client.getTcpOut().flush();
    }

    public void createLobby(ServerClient client, String lobbyName) throws IOException {
        serverLogger.info("Creating lobby for client {}, named {}", client.getClientId(), lobbyName);

        Lobby lobby = lobbyCreator.apply(client, lobbyName);
        lobbies.put(lobby.getLobbyIdentifier().id(), lobby);

        lobby.receiveNewClient(client);
        client.setOnDisconnect(lobby::clientDisconnect);
    }

    public void createLobby(String lobbyName) {
        serverLogger.info("Creating lobby named {}", lobbyName);

        Lobby lobby = lobbyCreator.apply(null, lobbyName);
        lobbies.put(lobby.getLobbyIdentifier().id(), lobby);
    }

    public void joinLobby(ServerClient client, UUID lobbyId) throws IOException {
        Lobby lobby = lobbies.get(lobbyId);

        if (lobby == null) {
            serverLogger.warn("Couldn't find {}'s chosen lobby to join.", client.clientId);
            return;
        }

        serverLogger.info("Client {} joining lobby {}", client.getClientId(), lobby.getLobbyIdentifier().name());

        lobby.receiveNewClient(client);
        client.setOnDisconnect(lobby::clientDisconnect);
    }

    public void receiveRequest(RequestType requestType, long dataLength, UUID senderId, MessageInputStream inputStream)
            throws IOException {
        ServerClient client = getClient(senderId);

        if (client == null) {
            serverLogger.warn("Couldn't find client {} to receive request.", senderId);

            inputStream.skipNBytes(dataLength);
            return;
        }

        switch (requestType) {
            case GetAvailableLobbies -> returnAvailableLobbies(client);
            case CreateLobby -> {
                String lobbyName = (String) inputStream.readObject(String.class);
                createLobby(client, lobbyName);
            }
            case JoinLobby -> {
                if (inputStream.available() < dataLength) {
                    serverLogger.warn(
                            "Unable to read {}'s lobby \"{}\" to join",
                            senderId,
                            Arrays.toString(inputStream.readAllBytes())
                    );

                    return;
                }

                joinLobby(client, (UUID) inputStream.readObject(UUID.class));
            }
        }
    }

    public void sendPingResponse(UUID senderId, long timestamp, MessageInputStream inputStream) throws IOException {
        ServerClient client = getClient(senderId);

        if (client == null) {
            serverLogger.warn("Couldn't find client {} to send ping response.", senderId);

            inputStream.skipNBytes(inputStream.available());
            return;
        }

        client.sendPingResponse(timestamp);
    }

    public void disconnectClient(ServerClient client) {
        Lobby lobby = getLobby(client);
        if (lobby != null) {
            lobby.clientDisconnect(client);
        }

        allClients.remove(client);
        client.disconnect(NetworkType.TCP);
    }
}
