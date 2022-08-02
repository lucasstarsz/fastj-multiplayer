package tech.fastj.network.rpc.server;

import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;
import tech.fastj.network.rpc.message.prebuilt.SessionIdentifier;
import tech.fastj.network.rpc.server.command.ServerCommand;
import tech.fastj.network.rpc.server.command.ServerCommandReader;
import tech.fastj.network.serial.Serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public abstract class Lobby<E extends Enum<E> & CommandAlias> implements ServerCommandReader<E> {

    protected final Serializer serializer;
    protected final Class<E> aliasClass;

    protected final Server<E> server;
    protected final List<ServerClient<E>> clients;
    protected final Map<UUID, Session<E>> sessions;

    private final Map<E, ServerCommand> commands;

    protected LobbyIdentifier lobbyIdentifier;
    protected Session<E> currentSession;
    protected UUID homeSessionId;

    private BiConsumer<Session<E>, Session<E>> onSwitchSession;
    private BiConsumer<Lobby<E>, ServerClient<E>> onReceiveNewClient;
    private BiConsumer<Lobby<E>, ServerClient<E>> onClientDisconnect;

    protected Lobby(Server<E> server, int expectedLobbySize, String name, Class<E> aliasClass) {
        this.aliasClass = aliasClass;
        this.server = server;

        commands = new EnumMap<>(aliasClass);
        serializer = new Serializer();

        serializer.registerSerializer(SessionIdentifier.class);
        serializer.registerSerializer(LobbyIdentifier.class);

        for (E commandAlias : aliasClass.getEnumConstants()) {
            commandAlias.registerMessages(serializer);
        }

        resetCommands();

        clients = new ArrayList<>(expectedLobbySize);
        sessions = new HashMap<>();
        lobbyIdentifier = new LobbyIdentifier(UUID.randomUUID(), name, 0, expectedLobbySize);

        onSwitchSession = (oldSession, newSession) -> {
        };
        onReceiveNewClient = (lobby, client) -> {
        };
        onClientDisconnect = (lobby, client) -> {
        };
    }

    public LobbyIdentifier getLobbyIdentifier() {
        return lobbyIdentifier;
    }

    public boolean hasClient(ServerClient<E> serverClient) {
        return clients.contains(serverClient);
    }

    public boolean hasClient(UUID clientId) {
        for (ServerClient<E> client : clients) {
            if (clientId.equals(client.getClientId())) {
                return true;
            }
        }

        return false;
    }

    public List<ServerClient<E>> getClients() {
        return Collections.unmodifiableList(clients);
    }

    public Map<UUID, Session<E>> getSessions() {
        return Collections.unmodifiableMap(sessions);
    }

    public Session<E> getCurrentSession() {
        return currentSession;
    }

    public Session<E> getHomeSession() {
        return sessions.get(homeSessionId);
    }

    public Server<E> getServer() {
        return server;
    }

    public void setOnSwitchSession(BiConsumer<Session<E>, Session<E>> onSwitchSession) {
        this.onSwitchSession = onSwitchSession;
    }

    public void setOnReceiveNewClient(BiConsumer<Lobby<E>, ServerClient<E>> onReceiveNewClient) {
        this.onReceiveNewClient = onReceiveNewClient;
    }

    public void setOnClientDisconnect(BiConsumer<Lobby<E>, ServerClient<E>> onClientDisconnect) {
        this.onClientDisconnect = onClientDisconnect;
    }

    public Session<E> getClientSession(ServerClient<E> client) {
        for (Session<E> session : sessions.values()) {
            if (session.getClients().contains(client)) {
                return session;
            }
        }

        return null;
    }

    public void receiveNewClient(ServerClient<E> client) throws IOException {
        getLogger().info("Lobby {} received new client {}", lobbyIdentifier.name(), client.getClientId());

        onReceiveNewClient.accept(this, client);

        clients.add(client);
        lobbyIdentifier = new LobbyIdentifier(lobbyIdentifier.id(), lobbyIdentifier.name(), clients.size(), lobbyIdentifier.maxPlayers());

        client.sendLobbyUpdate(lobbyIdentifier);

        Session<E> newClientSession = sessions.get(homeSessionId);

        if (newClientSession != null) {
            newClientSession.clientJoin(client);
        }
    }

    public void stop() {
        for (Session<E> session : sessions.values()) {
            session.stop();
        }

        sessions.clear();

        for (int i = clients.size() - 1; i >= 0; i--) {
            clients.get(i).disconnect();
        }

        clients.clear();
    }

    public void clientDisconnect(ServerClient<E> client) {
        clients.remove(client);
        lobbyIdentifier = new LobbyIdentifier(lobbyIdentifier.id(), lobbyIdentifier.name(), clients.size(), lobbyIdentifier.maxPlayers());

        Session<E> session = getClientSession(client);

        if (session != null) {
            session.clientLeave(client);
        }

        onClientDisconnect.accept(this, client);
    }

    public void switchCurrentSession(UUID nextSession) {
        Session<E> previousSession = currentSession;
        currentSession = sessions.get(nextSession);

        onSwitchSession.accept(previousSession, currentSession);
    }

    public void switchCurrentSession(String sessionName) {
        Session<E> previousSession = currentSession;
        currentSession = findSession(sessionName);
        onSwitchSession.accept(previousSession, currentSession);
    }

    protected void addSession(Session<E> session) {
        sessions.put(session.getSessionId(), session);
    }

    protected void setCurrentSession(Session<E> currentSession) {
        this.currentSession = currentSession;
    }

    protected void setHomeSessionId(UUID homeSessionId) {
        this.homeSessionId = homeSessionId;
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

    private Session<E> findSession(String sessionName) {
        for (Session<E> session : sessions.values()) {
            if (session == null) {
                continue;
            }

            if (sessionName.equals(session.getSessionIdentifier().sessionName())) {
                return session;
            }
        }

        throw new IllegalStateException(
            "Could not find session named " + sessionName + "." + System.lineSeparator()
                + "Sessions: " + sessions.values()
        );
    }
}
