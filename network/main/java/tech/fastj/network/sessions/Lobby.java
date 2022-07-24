package tech.fastj.network.sessions;

import tech.fastj.network.rpc.CommandHandler;
import tech.fastj.network.rpc.Server;
import tech.fastj.network.rpc.ServerClient;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public abstract class Lobby extends CommandHandler<ServerClient> {

    protected final LobbyIdentifier lobbyIdentifier;

    protected final Server server;
    protected final List<ServerClient> clients;
    protected final Map<UUID, Session> sessions;
    protected Session currentSession;
    protected UUID homeSessionId;
    protected UUID newClientSessionId;

    private BiConsumer<Session, Session> onSwitchSession;
    private BiConsumer<Lobby, ServerClient> onReceiveNewClient;
    private BiConsumer<Lobby, ServerClient> onClientDisconnect;

    protected Lobby(Server server, int expectedLobbySize, String name) {
        this.server = server;
        clients = new ArrayList<>(expectedLobbySize);
        sessions = new HashMap<>();
        lobbyIdentifier = new LobbyIdentifier(UUID.randomUUID(), name);

        onSwitchSession = (oldSession, newSession) -> {};
        onReceiveNewClient = (lobby, client) -> {};
        onClientDisconnect = (lobby, client) -> {};
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

    public Session getNewClientSession() {
        return sessions.get(newClientSessionId);
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

    public void receiveNewClient(ServerClient client) {
        clients.add(client);

        onReceiveNewClient.accept(this, client);

        Session newClientSession = sessions.get(newClientSessionId);
        if (newClientSession != null) {
            newClientSession.receiveNewClient(client);
        }
    }

    public void stop() {
        for (int i = clients.size() - 1; i >= 0; i--) {
            clients.get(i).disconnect();
        }

        clients.clear();
    }

    public void clientDisconnect(ServerClient client) {
        clients.remove(client);

        Session session = getClientSession(client);

        if (session != null) {
            session.clientDisconnect(client);
        }

        onClientDisconnect.accept(this, client);
    }

    protected void addSession(Session session) {
        sessions.put(session.getSessionId(), session);

        if (sessions.size() == 1) {
            setCurrentSession(session);
            setHomeSessionId(session.getSessionId());
            setNewClientSessionId(session.getSessionId());
        }
    }

    protected void setCurrentSession(Session currentSession) {
        this.currentSession = currentSession;
    }

    protected void setHomeSessionId(UUID homeSessionId) {
        this.homeSessionId = homeSessionId;
    }

    protected void setNewClientSessionId(UUID newClientSessionId) {
        this.newClientSessionId = newClientSessionId;
    }

    protected void switchCurrentSession(UUID nextSession) {
        Session previousSession = currentSession;
        currentSession = sessions.get(nextSession);

        onSwitchSession.accept(previousSession, currentSession);
    }
}
