package tech.fastj.network.sessions;

import tech.fastj.network.rpc.CommandAlias;
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

public abstract class Lobby<H extends Enum<H> & CommandAlias> extends CommandHandler<H, ServerClient<H>> {

    protected LobbyIdentifier lobbyIdentifier;

    protected final Server<H> server;
    protected final List<ServerClient<H>> clients;
    protected final Map<UUID, Session<H>> sessions;
    protected Session<H> currentSession;
    protected UUID homeSessionId;

    private BiConsumer<Session<H>, Session<H>> onSwitchSession;
    private BiConsumer<Lobby<H>, ServerClient<H>> onReceiveNewClient;
    private BiConsumer<Lobby<H>, ServerClient<H>> onClientDisconnect;

    protected Lobby(Server<H> server, int expectedLobbySize, String name, Class<H> aliasClass) {
        super(aliasClass);
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

    public boolean hasClient(ServerClient<H> serverClient) {
        return clients.contains(serverClient);
    }

    public boolean hasClient(UUID clientId) {
        for (ServerClient<H> client : clients) {
            if (clientId.equals(client.getClientId())) {
                return true;
            }
        }

        return false;
    }

    public List<ServerClient<H>> getClients() {
        return Collections.unmodifiableList(clients);
    }

    public Map<UUID, Session<H>> getSessions() {
        return Collections.unmodifiableMap(sessions);
    }

    public Session<H> getCurrentSession() {
        return currentSession;
    }

    public Session<H> getHomeSession() {
        return sessions.get(homeSessionId);
    }

    public Server<H> getServer() {
        return server;
    }

    public void setOnSwitchSession(BiConsumer<Session<H>, Session<H>> onSwitchSession) {
        this.onSwitchSession = onSwitchSession;
    }

    public void setOnReceiveNewClient(BiConsumer<Lobby<H>, ServerClient<H>> onReceiveNewClient) {
        this.onReceiveNewClient = onReceiveNewClient;
    }

    public void setOnClientDisconnect(BiConsumer<Lobby<H>, ServerClient<H>> onClientDisconnect) {
        this.onClientDisconnect = onClientDisconnect;
    }

    public Session<H> getClientSession(ServerClient<H> client) {
        for (Session<H> session : sessions.values()) {
            if (session.getClients().contains(client)) {
                return session;
            }
        }

        return null;
    }

    public void receiveNewClient(ServerClient<H> client) throws IOException {
        getLogger().info("Lobby {} received new client {}", lobbyIdentifier.name(), client.getClientId());

        onReceiveNewClient.accept(this, client);

        clients.add(client);
        lobbyIdentifier = new LobbyIdentifier(lobbyIdentifier.id(), lobbyIdentifier.name(), clients.size(), lobbyIdentifier.maxPlayers());

        client.sendLobbyUpdate(lobbyIdentifier);

        Session<H> newClientSession = sessions.get(homeSessionId);

        if (newClientSession != null) {
            newClientSession.clientJoin(client);
        }
    }

    public void stop() {
        for (Session<H> session : sessions.values()) {
            session.stop();
        }

        sessions.clear();

        for (int i = clients.size() - 1; i >= 0; i--) {
            clients.get(i).disconnect();
        }

        clients.clear();
    }

    public void clientDisconnect(ServerClient<H> client) {
        clients.remove(client);
        lobbyIdentifier = new LobbyIdentifier(lobbyIdentifier.id(), lobbyIdentifier.name(), clients.size(), lobbyIdentifier.maxPlayers());

        Session<H> session = getClientSession(client);

        if (session != null) {
            session.clientLeave(client);
        }

        onClientDisconnect.accept(this, client);
    }

    protected void addSession(Session<H> session) {
        sessions.put(session.getSessionId(), session);

        if (sessions.size() == 1) {
            setCurrentSession(session);
            setHomeSessionId(session.getSessionId());
        }
    }

    protected void setCurrentSession(Session<H> currentSession) {
        this.currentSession = currentSession;
    }

    protected void setHomeSessionId(UUID homeSessionId) {
        this.homeSessionId = homeSessionId;
    }

    public void switchCurrentSession(UUID nextSession) {
        Session<H> previousSession = currentSession;
        currentSession = sessions.get(nextSession);

        onSwitchSession.accept(previousSession, currentSession);
    }

    public void switchCurrentSession(String sessionName) {
        Session<H> previousSession = currentSession;
        System.out.println("previous session: " + previousSession.getSessionIdentifier().sessionName());
        currentSession = findSession(sessionName);
        System.out.println("current session: " + currentSession.getSessionIdentifier().sessionName());
        onSwitchSession.accept(previousSession, currentSession);
    }

    private Session<H> findSession(String sessionName) {
        System.out.println("sessions: " + sessions.values());

        for (Session<H> session : sessions.values()) {
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
