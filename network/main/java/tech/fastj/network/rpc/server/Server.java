package tech.fastj.network.rpc.server;

import tech.fastj.network.config.ServerConfig;
import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.local.LocalClient;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.RequestType;
import tech.fastj.network.rpc.message.SentMessageType;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;
import tech.fastj.network.rpc.message.prebuilt.SessionIdentifier;
import tech.fastj.network.rpc.server.command.ServerCommand;
import tech.fastj.network.rpc.server.command.ServerCommandReader;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.read.MessageInputStream;
import tech.fastj.network.serial.util.MessageUtils;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server<E extends Enum<E> & CommandAlias> implements ServerCommandReader<E> {

    protected final Serializer serializer;

    private final List<ServerClient<E>> allClients;
    private final Map<UUID, Lobby<E>> lobbies;
    private final BiFunction<ServerClient<E>, String, Lobby<E>> lobbyCreator;

    private final Class<E> aliasClass;
    private final Map<E, ServerCommand> commands;

    private final ServerSocket tcpServer;
    private final DatagramSocket udpServer;
    private final Logger serverLogger = LoggerFactory.getLogger(Server.class);

    private ExecutorService clientAccepter;
    private volatile boolean isRunning;
    private volatile boolean isAcceptingClients;

    public Server(ServerConfig serverConfig, Class<E> aliasClass, BiFunction<ServerClient<E>, String, Lobby<E>> lobbyCreator)
        throws IOException {
        this.aliasClass = aliasClass;
        commands = new EnumMap<>(aliasClass);
        serializer = new Serializer();

        this.allClients = new ArrayList<>(serverConfig.maxClients());
        this.lobbies = new LinkedHashMap<>();
        this.lobbyCreator = lobbyCreator;

        tcpServer = new ServerSocket(serverConfig.port(), serverConfig.clientBacklog(), serverConfig.address());
        udpServer = new DatagramSocket(serverConfig.port(), serverConfig.address());

        serializer.registerSerializer(SessionIdentifier.class);
        serializer.registerSerializer(LobbyIdentifier.class);

        for (E commandAlias : aliasClass.getEnumConstants()) {
            commandAlias.registerMessages(serializer);
        }

        resetCommands();
    }

    public List<ServerClient<E>> getClients() {
        return Collections.unmodifiableList(allClients);
    }

    public Map<UUID, Lobby<E>> getLobbies() {
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

    @Override
    public Class<E> getAliasClass() {
        return aliasClass;
    }

    @Override
    public Serializer getSerializer() {
        return serializer;
    }

    @Override
    public Map<E, ServerCommand> getCommands() {
        return commands;
    }

    public Lobby<E> getLobby(ServerClient<E> client) {
        for (Lobby<E> lobby : lobbies.values()) {
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
        for (Lobby<E> lobby : lobbies.values()) {
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

    public ServerClient<E> getClient(UUID senderId) {
        for (ServerClient<E> client : allClients) {
            if (senderId.equals(client.getClientId())) {
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
        ServerClient<E> client = null;

        try {
            serverLogger.debug("Accepting new client...");

            Socket clientSocket = tcpServer.accept();

            serverLogger.debug("Received new client, creating connection...");

            client = new ServerClient<>(clientSocket, this, udpServer, aliasClass);
            client.connect();

            serverLogger.debug("Client {} connected.", client.getClientId());

            client.startListening();
            allClients.add(client);
        } catch (IOException exception) {
            if (client != null && client.isConnected()) {
                client.getTcpOut().writeInt(LocalClient.Leave);
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

    public void receiveCommand(CommandTarget commandTarget, E commandId, UUID senderId, MessageInputStream stream)
        throws IOException {
        ServerClient<E> client = getClient(senderId);

        if (client == null) {
            return;
        }

        switch (commandTarget) {
            case Client -> client.readCommand(commandId, stream, client);
            case Server -> readCommand(commandId, stream, client);
            case Lobby -> {
                Lobby<E> lobby = getLobby(client);

                if (lobby == null) {
                    serverLogger.warn("Couldn't find {}'s lobby to send command {}", senderId, commandId);
                    stream.skipNBytes(stream.available());

                    return;
                }

                lobby.readCommand(commandId, stream, client);
            }
            case Session -> {
                Lobby<E> lobby = getLobby(client);

                if (lobby == null) {
                    serverLogger.warn("Couldn't find {}'s lobby to send command {}", senderId, commandId);
                    stream.skipNBytes(stream.available());

                    return;
                }

                Session<E> session = lobby.getClientSession(client);

                if (session == null) {
                    serverLogger.warn("Couldn't find {}'s session to send command {}", senderId, commandId);
                    stream.skipNBytes(stream.available());

                    return;
                }

                session.readCommand(commandId, stream, client);
            }
        }
    }

    public void returnAvailableLobbies(ServerClient<E> client) throws IOException {
        LobbyIdentifier[] lobbyIdentifiers = getLobbies()
            .values()
            .stream()
            .map(Lobby::getLobbyIdentifier)
            .toArray(LobbyIdentifier[]::new);

        client.getTcpOut().writeObject(SentMessageType.AvailableLobbiesUpdate, SentMessageType.class);
        client.getTcpOut().writeArray(lobbyIdentifiers);
        client.getTcpOut().flush();
    }

    public void createLobby(ServerClient<E> client, String lobbyName) throws IOException {
        serverLogger.info("Creating lobby for client {}, named {}", client.getClientId(), lobbyName);

        Lobby<E> lobby = lobbyCreator.apply(client, lobbyName);
        lobbies.put(lobby.getLobbyIdentifier().id(), lobby);

        lobby.receiveNewClient(client);
        client.setOnDisconnect(() -> disconnectClient(client));
    }

    public void createLobby(String lobbyName) {
        serverLogger.info("Creating lobby named {}", lobbyName);

        Lobby<E> lobby = lobbyCreator.apply(null, lobbyName);
        lobbies.put(lobby.getLobbyIdentifier().id(), lobby);
    }

    public void joinLobby(ServerClient<E> client, UUID lobbyId) throws IOException {
        Lobby<E> lobby = lobbies.get(lobbyId);

        if (lobby == null) {
            serverLogger.warn("Couldn't find {}'s chosen lobby to join.", client.getClientId());
            return;
        }

        serverLogger.info("Client {} joining lobby {}", client.getClientId(), lobby.getLobbyIdentifier().name());

        lobby.receiveNewClient(client);
        client.setOnDisconnect(() -> disconnectClient(client));
    }

    public void receiveRequest(RequestType requestType, UUID senderId, MessageInputStream inputStream)
        throws IOException {
        ServerClient<E> client = getClient(senderId);

        if (client == null) {
            serverLogger.warn("Couldn't find client {} to receive request.", senderId);
            inputStream.skipNBytes(inputStream.available());
            return;
        }

        switch (requestType) {
            case GetAvailableLobbies -> returnAvailableLobbies(client);
            case CreateLobby -> {
                String lobbyName = (String) inputStream.readObject(String.class);
                createLobby(client, lobbyName);
            }
            case JoinLobby -> {
                if (inputStream.available() < MessageUtils.UuidBytes) {
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
        ServerClient<E> client = getClient(senderId);

        if (client == null) {
            serverLogger.warn("Couldn't find client {} to send ping response.", senderId);

            inputStream.skipNBytes(inputStream.available());
            return;
        }

        client.sendPingResponse(timestamp);
    }

    public void disconnectClient(ServerClient<E> client) {
        if (client.isConnected()) {
            client.disconnect();
        }

        Lobby<E> lobby = getLobby(client);

        if (lobby != null) {
            lobby.clientDisconnect(client);
        }

        allClients.remove(client);
    }
}
