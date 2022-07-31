package tech.fastj.network.sessions;

import tech.fastj.network.rpc.Client;
import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.NetworkSender;
import tech.fastj.network.rpc.SendUtils;
import tech.fastj.network.rpc.ServerClient;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.RequestType;
import tech.fastj.network.rpc.message.prebuilt.SessionIdentifier;
import tech.fastj.network.serial.Serializer;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Session<H extends Enum<H> & CommandAlias> extends SessionHandler<H, ServerClient<H>> implements NetworkSender {
    private static final Logger SessionLogger = LoggerFactory.getLogger(Client.class);

    protected final Lobby<H> lobby;
    private final List<ServerClient<H>> clients;
    private final SessionIdentifier sessionIdentifier;
    private BiConsumer<Session<H>, ServerClient<H>> onClientJoin;
    private BiConsumer<Session<H>, ServerClient<H>> onClientLeave;
    private ExecutorService sequenceRunner;

    protected Session(Lobby<H> lobby, String name, List<ServerClient<H>> clients, Class<H> aliacClass) {
        super(aliacClass);
        this.lobby = lobby;
        this.clients = clients;
        sessionIdentifier = new SessionIdentifier(UUID.randomUUID(), name);

        onClientJoin = (session, client) -> {
        };
        onClientLeave = (session, client) -> {
        };
    }

    public UUID getSessionId() {
        return sessionIdentifier.sessionId();
    }

    public SessionIdentifier getSessionIdentifier() {
        return sessionIdentifier;
    }

    public List<ServerClient<H>> getClients() {
        return Collections.unmodifiableList(clients);
    }

    public void setOnClientJoin(BiConsumer<Session<H>, ServerClient<H>> onClientJoin) {
        this.onClientJoin = onClientJoin;
    }

    public void setOnClientLeave(BiConsumer<Session<H>, ServerClient<H>> onClientLeave) {
        this.onClientLeave = onClientLeave;
    }

    public <T> Future<T> startSessionSequence(Sequence<T, H> sessionSequence) {
        if (sequenceRunner == null) {
            sequenceRunner = Executors.newWorkStealingPool();
        }

        return sequenceRunner.submit(sessionSequence::start);
    }

    @Override
    public Serializer getSerializer() {
        return serializer;
    }

    @Override
    public synchronized void sendCommand(NetworkType networkType, CommandTarget commandTarget, Enum<? extends CommandAlias> commandId, byte[] rawData)
        throws IOException {
        assert aliasClass.isAssignableFrom(commandId.getClass());

        SessionLogger.trace(
            "Session {} sending {} \"{}\" to {} client(s)",
            sessionIdentifier.sessionId(),
            networkType.name(),
            commandId.name(),
            clients.size()
        );

        switch (networkType) {
            case TCP -> {
                byte[] data = SendUtils.buildTCPCommandData(commandTarget, commandId, rawData);
                sendTCP(data);
            }
            case UDP -> {
                DatagramSocket udpServer = lobby.getServer().getUdpServer();
                byte[] data = SendUtils.buildUDPCommandData(commandTarget, sessionIdentifier.sessionId(), commandId, rawData);
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

        for (int clientsSize = clients.size() - 1, i = clientsSize; i > 0; i--) {
            clients.get(i).disconnect();
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

        for (ServerClient<H> client : clients) {
            client.sendKeepAlive(networkType);
        }
    }

    private void sendTCP(byte[] data) throws IOException {
        for (ServerClient<H> client : clients) {
            client.getTcpOut().write(data);
            client.getTcpOut().flush();
        }
    }

    private void sendUDP(DatagramSocket udpServer, byte[] data) throws IOException {
        for (ServerClient<H> client : clients) {
            DatagramPacket packet = SendUtils.buildPacket(client.getClientConfig(), data);
            udpServer.send(packet);
        }
    }

    public void clientJoin(ServerClient<H> client) throws IOException {
        client.sendSessionUpdate(sessionIdentifier);
        onClientJoin.accept(this, client);
        clients.add(client);
    }

    public void clientLeave(ServerClient<H> client) {
        clients.remove(client);
        onClientLeave.accept(this, client);
    }

    public void stop() {
        if (sequenceRunner != null) {
            sequenceRunner.shutdownNow();
            sequenceRunner = null;
        }

        sendDisconnect(NetworkType.TCP, null);
    }

    public interface Sequence<T, H extends Enum<H> & CommandAlias> {

        T start() throws Exception;

        default boolean waitForCompletion(BooleanSupplier task, long timeout, long timeBetweenChecks, TimeUnit timeoutUnit)
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

        default Future<Boolean> waitForCompletionAsync(BooleanSupplier task, long timeout, long timeBetweenChecks, TimeUnit timeoutUnit) {
            ExecutorService completionExecutor = Executors.newWorkStealingPool();

            try {
                return completionExecutor.submit(() -> waitForCompletion(task, timeout, timeBetweenChecks, timeoutUnit));
            } finally {
                completionExecutor.shutdown();
            }
        }

        default Map<ResponseId<H>, Object[]> waitForResponses(Session<H> session, H responseId, long timeout, long timeBetweenChecks,
                                                              TimeUnit timeoutUnit) throws InterruptedException {
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

        default Future<Map<ResponseId<H>, Object[]>> waitForResponsesAsync(Session<H> session, H responseId, long timeout,
                                                                           long timeBetweenChecks, TimeUnit timeoutUnit) {
            ExecutorService completionExecutor = Executors.newWorkStealingPool();

            try {
                return completionExecutor.submit(() -> waitForResponses(session, responseId, timeout, timeBetweenChecks, timeoutUnit));
            } finally {
                completionExecutor.shutdown();
            }
        }
    }
}
