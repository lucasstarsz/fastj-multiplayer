package tech.fastj.network.sessions;

import tech.fastj.network.rpc.CommandHandler;
import tech.fastj.network.rpc.Server;
import tech.fastj.network.rpc.ServerClient;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public abstract class Lobby extends CommandHandler<ServerClient> {

    protected LobbyIdentifier lobbyIdentifier;

    protected final Server server;
    protected final List<ServerClient> clients;
    protected final Map<UUID, Session> sessions;
    protected Session currentSession;
    protected UUID homeSessionId;

    private BiConsumer<Session, Session> onSwitchSession;
    private BiConsumer<Lobby, ServerClient> onReceiveNewClient;
    private BiConsumer<Lobby, ServerClient> onClientDisconnect;

    protected Lobby(Server server, int expectedLobbySize, String name) {
        this.server = server;
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

    public boolean hasClient(ServerClient serverClient) {
        return clients.contains(serverClient);
    }

    public boolean hasClient(UUID clientId) {
        for (ServerClient client : clients) {
            if (clientId.equals(client.getClientId())) {
                return true;
            }
        }

        return false;
    }

    public List<ServerClient> getClients() {
        return Collections.unmodifiableList(clients);
    }

    public Map<UUID, Session> getSessions() {
        return Collections.unmodifiableMap(sessions);
    }

    public Session getCurrentSession() {
        return currentSession;
    }

    public Session getHomeSession() {
        return sessions.get(homeSessionId);
    }

    public Server getServer() {
        return server;
    }

    public void setOnSwitchSession(BiConsumer<Session, Session> onSwitchSession) {
        this.onSwitchSession = onSwitchSession;
    }

    public void setOnReceiveNewClient(BiConsumer<Lobby, ServerClient> onReceiveNewClient) {
        this.onReceiveNewClient = onReceiveNewClient;
    }

    public void setOnClientDisconnect(BiConsumer<Lobby, ServerClient> onClientDisconnect) {
        this.onClientDisconnect = onClientDisconnect;
    }

    public Session getClientSession(ServerClient client) {
        for (Session session : sessions.values()) {
            if (session.getClients().contains(client)) {
                return session;
            }
        }

        return null;
    }

    public void receiveNewClient(ServerClient client) throws IOException {
        getLogger().info("Lobby {} received new client {}", lobbyIdentifier.name(), client.getClientId());

        onReceiveNewClient.accept(this, client);

        clients.add(client);
        lobbyIdentifier = new LobbyIdentifier(lobbyIdentifier.id(), lobbyIdentifier.name(), clients.size(), lobbyIdentifier.maxPlayers());

        client.sendLobbyUpdate(lobbyIdentifier);

        Session newClientSession = sessions.get(homeSessionId);

        if (newClientSession != null) {
            newClientSession.clientJoin(client);
        }
    }

    public void stop() {
        for (Session session : sessions.values()) {
            session.stop();
        }

        sessions.clear();

        for (int i = clients.size() - 1; i >= 0; i--) {
            clients.get(i).disconnect();
        }

        clients.clear();
    }

    public void clientDisconnect(ServerClient client) {
        clients.remove(client);
        lobbyIdentifier = new LobbyIdentifier(lobbyIdentifier.id(), lobbyIdentifier.name(), clients.size(), lobbyIdentifier.maxPlayers());

        Session session = getClientSession(client);

        if (session != null) {
            session.clientLeave(client);
        }

        onClientDisconnect.accept(this, client);
    }

    protected void addSession(Session session) {
        sessions.put(session.getSessionId(), session);

        if (sessions.size() == 1) {
            setCurrentSession(session);
            setHomeSessionId(session.getSessionId());
        }
    }

    protected void setCurrentSession(Session currentSession) {
        this.currentSession = currentSession;
    }

    protected void setHomeSessionId(UUID homeSessionId) {
        this.homeSessionId = homeSessionId;
    }

    public void switchCurrentSession(UUID nextSession) {
        Session previousSession = currentSession;
        currentSession = sessions.get(nextSession);

        onSwitchSession.accept(previousSession, currentSession);
    }

    public void switchCurrentSession(String sessionName) {
        Session previousSession = currentSession;
        System.out.println("previous session: " + previousSession.getSessionIdentifier().sessionName());
        currentSession = findSession(sessionName);
        System.out.println("current session: " + currentSession.getSessionIdentifier().sessionName());
        onSwitchSession.accept(previousSession, currentSession);
    }

    private Session findSession(String sessionName) {
        System.out.println("sessions: " + sessions.values());

        for (Session session : sessions.values()) {
            if (session == null) {
                continue;
            }

            System.out.println(session.getSessionIdentifier().sessionName());
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
