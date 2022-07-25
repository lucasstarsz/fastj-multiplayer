package tech.fastj.network.sessions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.fastj.network.rpc.Client;
import tech.fastj.network.rpc.NetworkSender;
import tech.fastj.network.rpc.SendUtils;
import tech.fastj.network.rpc.ServerClient;
import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.RequestType;
import tech.fastj.network.rpc.message.prebuilt.SessionIdentifier;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public abstract class Session extends SessionHandler<ServerClient> implements NetworkSender {
    private static final Logger SessionLogger = LoggerFactory.getLogger(Client.class);

    private final Lobby lobby;
    private final List<ServerClient> clients;
    private final SessionIdentifier sessionIdentifier;
    private BiConsumer<Session, ServerClient> onReceiveNewClient;
    private BiConsumer<Session, ServerClient> onClientDisconnect;
    private ExecutorService sequenceRunner;

    protected Session(Lobby lobby, String name, List<ServerClient> clients) {
        this.lobby = lobby;
        this.clients = clients;
        sessionIdentifier = new SessionIdentifier(UUID.randomUUID(), name);

        onReceiveNewClient = (session, client) -> {
        };
        onClientDisconnect = (session, client) -> {
        };
    }

    public UUID getSessionId() {
        return sessionIdentifier.sessionId();
    }

    public SessionIdentifier getSessionIdentifier() {
        return sessionIdentifier;
    }

    public List<ServerClient> getClients() {
        return Collections.unmodifiableList(clients);
    }

    public void setOnReceiveNewClient(BiConsumer<Session, ServerClient> onReceiveNewClient) {
        this.onReceiveNewClient = onReceiveNewClient;
    }

    public void setOnClientDisconnect(BiConsumer<Session, ServerClient> onClientDisconnect) {
        this.onClientDisconnect = onClientDisconnect;
    }

    public <T> Future<T> startSessionSequence(Sequence<T> sessionSequence) {
        if (sequenceRunner == null) {
            sequenceRunner = Executors.newWorkStealingPool();
        }

        return sequenceRunner.submit(sessionSequence::start);
    }

    @Override
    public synchronized void sendCommand(NetworkType networkType, CommandTarget commandTarget, Command.Id commandId, byte[] rawData)
            throws IOException {
        SessionLogger.trace(
                "Session {} sending {} \"{}\" to {} client(s)",
                sessionIdentifier.sessionId(),
                networkType.name(),
                commandId.name(),
                clients.size()
        );

        switch (networkType) {
            case TCP -> {
                byte[] data = SendUtils.buildTCPCommandData(commandTarget, commandId.uuid(), rawData);
                sendTCP(data);
            }
            case UDP -> {
                DatagramSocket udpServer = lobby.getServer().getUdpServer();
                byte[] data = SendUtils.buildUDPCommandData(commandTarget, sessionIdentifier.sessionId(), commandId.uuid(), rawData);
                sendUDP(udpServer, data);
            }
        }
    }

    @Override
    public void sendRequest(NetworkType networkType, RequestType requestType, byte[] rawData) throws IOException {
        SessionLogger.trace(
                "Session {} sending {} \"{}\" to {} client(s)",
                sessionIdentifier.sessionId(),
                networkType.name(),
                requestType.name(),
                clients.size()
        );

        switch (networkType) {
            case TCP -> {
                byte[] data = SendUtils.buildTCPRequestData(requestType, rawData);
                sendTCP(data);
            }
            case UDP -> {
                DatagramSocket udpServer = lobby.getServer().getUdpServer();
                byte[] data = SendUtils.buildUDPRequestData(sessionIdentifier.sessionId(), requestType, rawData);
                sendUDP(udpServer, data);
            }
        }
    }

    @Override
    public void sendDisconnect(NetworkType networkType, byte[] rawData) {
        SessionLogger.trace(
                "Session {} sending {}  \"disconnect\" to {} client(s)",
                sessionIdentifier.sessionId(),
                networkType.name(),
                clients.size()
        );

        for (ServerClient client : clients) {
            client.disconnect();
        }
    }

    @Override
    public void sendKeepAlive(NetworkType networkType) throws IOException {
        SessionLogger.trace(
                "Session {} sending {}  \"keep-alive\" to {} client(s)",
                sessionIdentifier.sessionId(),
                networkType.name(),
                clients.size()
        );

        for (ServerClient client : clients) {
            client.sendKeepAlive(networkType);
        }
    }

    private void sendTCP(byte[] data) throws IOException {
        for (ServerClient client : clients) {
            client.getTcpOut().write(data);
            client.getTcpOut().flush();
        }
    }

    private void sendUDP(DatagramSocket udpServer, byte[] data) throws IOException {
        for (ServerClient client : clients) {
            DatagramPacket packet = SendUtils.buildPacket(client.getClientConfig(), data);
            udpServer.send(packet);
        }
    }

    public void receiveNewClient(ServerClient client) {
        clients.add(client);
        onReceiveNewClient.accept(this, client);
    }

    public void clientDisconnect(ServerClient client) {
        clients.remove(client);
        onClientDisconnect.accept(this, client);
    }

    public void stop() {
        if (sequenceRunner != null) {
            sequenceRunner.shutdownNow();
            sequenceRunner = null;
        }

        sendDisconnect(NetworkType.TCP, null);
    }

    public abstract static class Sequence<T> {

        private final ExecutorService completionExecutor;

        protected final Session session;

        public Sequence(Session session) {
            this.session = session;
            completionExecutor = Executors.newWorkStealingPool();
        }

        public abstract T start();

        public boolean waitForCompletion(BooleanSupplier task, long timeout, long timeBetweenChecks, TimeUnit timeoutUnit)
                throws InterruptedException {

            long currentTime = System.nanoTime();
            long timeoutNanos = TimeUnit.NANOSECONDS.convert(timeout, timeoutUnit);
            long endTime = currentTime + timeoutNanos;

            while (endTime > currentTime) {
                currentTime = System.nanoTime();

                if (task.getAsBoolean()) {
                    return true;
                }

                timeoutUnit.sleep(timeBetweenChecks);
            }

            return false;
        }

        public Future<Boolean> waitForCompletionAsync(BooleanSupplier task, long timeout, long timeBetweenChecks, TimeUnit timeoutUnit) {
            return completionExecutor.submit(() -> waitForCompletion(task, timeout, timeBetweenChecks, timeoutUnit));
        }

        public Map<ResponseId, Object[]> waitForResponses(Command.Id responseId, long timeout, long timeBetweenChecks, TimeUnit timeoutUnit)
                throws InterruptedException {
            session.trackResponses(responseId);

            long timeoutNanos = TimeUnit.NANOSECONDS.convert(timeout, timeoutUnit);
            long lastTime = System.nanoTime();
            long timePassed = 0L;

            while (timeoutNanos > timePassed) {
                long thisTime = System.nanoTime();
                timePassed += (thisTime - lastTime);

                if (session.hasAllResponses(responseId, session.clients)) {
                    return session.drainResponses(responseId);
                }

                timeoutUnit.sleep(timeBetweenChecks);
            }

            return Map.of();
        }

        public Future<Map<ResponseId, Object[]>> waitForResponsesAsync(Command.Id responseId, long timeout, long timeBetweenChecks,
                                                                       TimeUnit timeoutUnit) {
            return completionExecutor.submit(() -> waitForResponses(responseId, timeout, timeBetweenChecks, timeoutUnit));
        }
    }
}
