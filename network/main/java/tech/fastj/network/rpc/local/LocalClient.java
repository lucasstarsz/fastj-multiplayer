package tech.fastj.network.rpc.local;

import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.rpc.ClientBase;
import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.ConnectionStatus;
import tech.fastj.network.rpc.SendUtils;
import tech.fastj.network.rpc.local.command.LocalCommand;
import tech.fastj.network.rpc.local.command.LocalCommandReader;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.RequestType;
import tech.fastj.network.rpc.message.SentMessageType;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;
import tech.fastj.network.rpc.message.prebuilt.SessionIdentifier;
import tech.fastj.network.serial.read.MessageInputStream;
import tech.fastj.network.serial.util.MessageUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalClient<E extends Enum<E> & CommandAlias> extends ClientBase<E> implements LocalCommandReader<E> {

    private static final Logger ClientLogger = LoggerFactory.getLogger(LocalClient.class);
    public static final int Leave = 1;
    public static final int Join = 0;

    private final ExecutorService updateFreshener;
    private final Class<E> aliasClass;
    private final Map<E, LocalCommand> commands;

    private ScheduledExecutorService pingSender;
    private boolean isSendingPings;

    private ScheduledExecutorService keepAliveSender;
    private boolean isSendingKeepAlives;

    private LobbyIdentifier[] tempAvailableLobbies;
    private LobbyIdentifier currentLobby;
    private SessionIdentifier currentSession;

    private LongConsumer onPingReceived;
    private BiConsumer<LobbyIdentifier, LobbyIdentifier> onLobbyUpdate;
    private BiConsumer<SessionIdentifier, SessionIdentifier> onSessionUpdate;
    private volatile boolean recentLobbyUpdate;

    public LocalClient(ClientConfig clientConfig, Class<E> aliasClass) throws IOException {
        super(clientConfig);

        this.aliasClass = aliasClass;
        this.commands = new EnumMap<>(aliasClass);

        serializer.registerSerializer(SessionIdentifier.class);
        serializer.registerSerializer(LobbyIdentifier.class);

        for (E commandAlias : aliasClass.getEnumConstants()) {
            commandAlias.registerMessages(serializer);
        }

        resetCommands();

        onPingReceived = (ping) -> {
        };
        onLobbyUpdate = (oldLobby, newLobby) -> {
        };
        onSessionUpdate = (oldSession, newSession) -> {
        };

        updateFreshener = Executors.newWorkStealingPool();
    }

    @Override
    public void connect() throws IOException {
        super.connect();

        ClientLogger.debug("{} connecting UDP to {}:{}...", clientId, clientConfig.address(), clientConfig.port());

        udpSocket.connect(clientConfig.address(), clientConfig.port());

        ClientLogger.debug("UDP satisfactory.");

        ClientLogger.debug("{} checking server connection status...", clientId);

        int verification = tcpIn.readInt();
        if (verification != LocalClient.Join) {
            disconnect();
            throw new IOException("Failed to join server " + clientConfig.address() + ":" + clientConfig.port() + ", connection status was " + verification + ".");
        }

        clientId = (UUID) tcpIn.readObject(UUID.class);

        ClientLogger.debug("Client id synced to server, now {}.", clientId);
        ClientLogger.debug("{} connection status to {}:{} satisfactory.", clientId, clientConfig.address(), clientConfig.port());
        ClientLogger.debug("Sending UDP port {}", udpSocket.getLocalPort());

        tcpOut.writeInt(udpSocket.getLocalPort());
        tcpOut.flush();

        startListening();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void readMessageType(NetworkType networkType, UUID senderId, MessageInputStream inputStream, SentMessageType sentMessageType)
        throws IOException {
        switch (sentMessageType) {
            case KeepAlive -> {
            }
            case Disconnect -> disconnect();
            case PingResponse -> {
                long currentTimestamp = System.nanoTime();
                long otherTimestamp = inputStream.readLong();
                long pingNanos = currentTimestamp - otherTimestamp;
                onPingReceived.accept(pingNanos);
            }
            case LobbyUpdate -> {
                LobbyIdentifier newLobby = (LobbyIdentifier) inputStream.readObject(LobbyIdentifier.class);
                LobbyIdentifier oldLobby = currentLobby;

                currentLobby = newLobby;
                recentLobbyUpdate = true;

                updateFreshener.submit(() -> {
                    TimeUnit.MILLISECONDS.sleep(20L);
                    recentLobbyUpdate = false;
                    return 0;
                });

                onLobbyUpdate.accept(oldLobby, newLobby);
            }
            case SessionUpdate -> {
                SessionIdentifier newSession = (SessionIdentifier) inputStream.readObject(SessionIdentifier.class);
                SessionIdentifier oldSession = currentSession;
                currentSession = newSession;

                onSessionUpdate.accept(oldSession, newSession);
            }
            case AvailableLobbiesUpdate -> tempAvailableLobbies = (LobbyIdentifier[]) inputStream.readObject(LobbyIdentifier[].class);
            case RPCCommand -> {
                CommandTarget commandTarget = (CommandTarget) inputStream.readObject(CommandTarget.class);
                E commandId = (E) inputStream.readObject(aliasClass);

                ClientLogger.trace("{} received RPC command \"{}\" on {} targeting {}", senderId, commandId, networkType, commandTarget);

                if (commandTarget != CommandTarget.Client) {
                    ClientLogger.warn(
                        "Received command \"{}\" targeted at {} instead of client, discarding {}.",
                        commandId.name(),
                        commandTarget.name(),
                        Arrays.toString(inputStream.readAllBytes())
                    );

                    return;
                }

                readCommand(commandId, inputStream);
            }
            default -> ClientLogger.warn(
                "{} Received unused message type {}, discarding {}",
                senderId,
                sentMessageType.name(),
                Arrays.toString(inputStream.readAllBytes())
            );
        }
    }

    @Override
    protected void shutdown() throws IOException {
        getLogger().debug("{} shutting down", clientId);

        stopKeepAlives();
        stopPings();
        stopListening();
        updateFreshener.shutdownNow();

        udpSocket.close();
        tcpSocket.close();

        connectionStatus = ConnectionStatus.Disconnected;
    }

    public void onLobbyUpdate(BiConsumer<LobbyIdentifier, LobbyIdentifier> onLobbyUpdate) {
        this.onLobbyUpdate = onLobbyUpdate;
    }

    public void onSessionUpdate(BiConsumer<SessionIdentifier, SessionIdentifier> onSessionUpdate) {
        this.onSessionUpdate = onSessionUpdate;
    }

    public void onPingReceived(LongConsumer onPingReceived) {
        this.onPingReceived = onPingReceived;
    }

    public boolean startPings(long delay, TimeUnit delayUnit) {
        if (isSendingPings) {
            return false;
        }

        isSendingPings = true;

        pingSender = Executors.newSingleThreadScheduledExecutor();
        pingSender.scheduleAtFixedRate(this::sendPing, 0L, delay, delayUnit);

        return true;
    }

    private void sendPing() {
        byte[] packetData = ByteBuffer.allocate(MessageUtils.EnumBytes + Long.BYTES)
            .putInt(SentMessageType.PingRequest.ordinal())
            .putLong(System.nanoTime())
            .array();

        ClientLogger.trace("sending ping to {}:{}", clientConfig.address(), clientConfig.port());

        try {
            tcpOut.write(packetData);
            tcpOut.flush();
        } catch (IOException exception) {
            ClientLogger.error("Unable to send UDP ping packet. Stopping pings", exception);
            stopPings();
        }
    }

    public boolean stopPings() {
        if (!isSendingPings) {
            return false;
        }

        isSendingPings = false;

        pingSender.shutdownNow();
        pingSender = null;

        return true;
    }

    public boolean isSendingPings() {
        return isSendingPings;
    }

    public boolean startKeepAlives(long delay, TimeUnit delayUnit) {
        if (isSendingKeepAlives) {
            return false;
        }

        isSendingKeepAlives = true;

        keepAliveSender = Executors.newSingleThreadScheduledExecutor();
        keepAliveSender.scheduleAtFixedRate(this::sendKeepAlives, 0L, delay, delayUnit);

        return true;
    }

    private void sendKeepAlives() {
        try {
            sendKeepAlive(NetworkType.TCP);
        } catch (IOException exception) {
            ClientLogger.error("Unable to send keep-alive(s). Stopping sending keep-alives.", exception);
            stopKeepAlives();
        }
    }

    public boolean stopKeepAlives() {
        if (!isSendingKeepAlives) {
            return false;
        }

        isSendingKeepAlives = false;

        keepAliveSender.shutdownNow();
        keepAliveSender = null;

        return true;
    }

    public boolean isSendingKeepAlives() {
        return isSendingKeepAlives;
    }

    public LobbyIdentifier[] getAvailableLobbies() throws IOException, InterruptedException {
        if (!isConnected()) {
            throw new IOException("Cannot ask for available lobbies while client is not connected.");
        }

        sendRequest(NetworkType.TCP, RequestType.GetAvailableLobbies);

        while (tempAvailableLobbies == null && isListening) {
            TimeUnit.MILLISECONDS.sleep(1L);
        }
        LobbyIdentifier[] results = tempAvailableLobbies;
        tempAvailableLobbies = null;

        return results;
    }

    public LobbyIdentifier getCurrentLobby() {
        return currentLobby;
    }

    public SessionIdentifier getCurrentSession() {
        return currentSession;
    }

    public LobbyIdentifier createLobby(String lobbyName) throws IOException, InterruptedException {
        if (connectionStatus.ordinal() < ConnectionStatus.InServer.ordinal()) {
            throw new IOException("Cannot create lobby while client is not connected.");
        }

        sendRequest(NetworkType.TCP, RequestType.CreateLobby, serializer.writeObject(lobbyName, String.class));

        while (!recentLobbyUpdate && isListening) {
            TimeUnit.MILLISECONDS.sleep(1L);
        }

        return currentLobby;
    }

    public LobbyIdentifier joinLobby(UUID lobbyId) throws IOException, InterruptedException {
        if (connectionStatus.ordinal() < ConnectionStatus.InServer.ordinal()) {
            throw new IOException("Cannot join lobby while client is not connected.");
        }

        sendRequest(NetworkType.TCP, RequestType.JoinLobby, serializer.writeObject(lobbyId, UUID.class));

        while (!recentLobbyUpdate && isListening) {
            TimeUnit.MILLISECONDS.sleep(1L);
        }

        return currentLobby;
    }

    @Override
    public Logger getLogger() {
        return ClientLogger;
    }

    @Override
    public Class<E> getAliasClass() {
        return aliasClass;
    }

    @Override
    public Map<E, LocalCommand> getCommands() {
        return commands;
    }

    @Override
    public synchronized void sendCommand(NetworkType networkType, CommandTarget commandTarget, Enum<? extends CommandAlias> commandId, byte[] rawData)
        throws IOException {
        assert aliasClass.isAssignableFrom(commandId.getClass());

        ClientLogger.trace("{} sending {} \"{}\" to {}:{}", clientId, networkType.name(), commandId.name(), clientConfig.address(), clientConfig.port());

        switch (networkType) {
            case TCP -> SendUtils.sendTCPCommand(tcpOut, commandTarget, commandId, rawData);
            case UDP -> SendUtils.sendUDPCommand(udpSocket, clientConfig, commandTarget, commandId, clientId, rawData);
        }
    }

    @Override
    public synchronized void sendRequest(NetworkType networkType, RequestType requestType, byte[] rawData) throws IOException {
        ClientLogger.debug("{} sending {} \"{}\" to {}:{}", clientId, networkType.name(), requestType.name(), clientConfig.address(), clientConfig.port());

        switch (networkType) {
            case TCP -> SendUtils.sendTCPRequest(tcpOut, requestType, rawData);
            case UDP -> SendUtils.sendUDPRequest(udpSocket, clientConfig, requestType, clientId, rawData);
        }
    }

    @Override
    public synchronized void sendDisconnect(NetworkType networkType, byte[] rawData) throws IOException {
        ClientLogger.debug("{} sending {} disconnect to {}:{}", clientId, networkType.name(), clientConfig.address(), clientConfig.port());

        switch (networkType) {
            case TCP -> SendUtils.sendTCPDisconnect(tcpOut);
            case UDP -> SendUtils.sendUDPDisconnect(clientId, udpSocket, clientConfig);
        }
    }

    @Override
    public synchronized void sendKeepAlive(NetworkType networkType) throws IOException {
        switch (networkType) {
            case TCP -> SendUtils.sendTCPKeepAlive(tcpOut);
            case UDP -> SendUtils.sendUDPKeepAlive(clientId, udpSocket, clientConfig);
        }
    }
}
