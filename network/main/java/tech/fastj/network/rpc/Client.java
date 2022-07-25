package tech.fastj.network.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.RequestType;
import tech.fastj.network.rpc.message.SentMessageType;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;
import tech.fastj.network.serial.read.MessageInputStream;
import tech.fastj.network.serial.util.MessageUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

public class Client extends ConnectionHandler<Client> implements Runnable, NetworkSender {

    private static final Logger ClientLogger = LoggerFactory.getLogger(Client.class);

    public static final int Leave = 1;
    public static final int Join = 0;

    private ScheduledExecutorService pingSender;
    private boolean isSendingPings;

    private ScheduledExecutorService keepAliveSender;
    private boolean isSendingKeepAlives;

    private LongConsumer onPingReceived;

    public Client(ClientConfig clientConfig) throws IOException {
        super(clientConfig);
        onPingReceived = (ping) -> {
        };
    }

    @Override
    public void connect() throws IOException {
        super.connect();

        ClientLogger.debug("{} connecting UDP to {}:{}...", clientId, clientConfig.address(), clientConfig.port());

        udpSocket.connect(clientConfig.address(), clientConfig.port());

        ClientLogger.debug("UDP satisfactory.");

        ClientLogger.debug("{} checking server connection status...", clientId);

        int verification = tcpIn.readInt();
        if (verification != Client.Join) {
            disconnect();
            throw new IOException("Failed to join server " + clientConfig.address() + ":" + clientConfig.port() + ", connection status was " + verification + ".");
        }

        clientId = (UUID) tcpIn.readObject(UUID.class);

        ClientLogger.debug("Client id synced to server, now {}.", clientId);
        ClientLogger.debug("{} connection status to {}:{} satisfactory.", clientId, clientConfig.address(), clientConfig.port());
        ClientLogger.debug("Sending UDP port {}", udpSocket.getLocalPort());

        tcpOut.writeInt(udpSocket.getLocalPort());
        tcpOut.flush();
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
        byte[] packetData = ByteBuffer.allocate(MessageUtils.UuidBytes + MessageUtils.EnumBytes + Long.BYTES)
                .putLong(clientId.getMostSignificantBits())
                .putLong(clientId.getLeastSignificantBits())
                .putInt(SentMessageType.PingRequest.ordinal())
                .putLong(System.nanoTime())
                .array();

        DatagramPacket packet = SendUtils.buildPacket(clientConfig, packetData);

        ClientLogger.debug("sending ping to {}:{}", clientConfig.address(), clientConfig.port());

        try {
            udpSocket.send(packet);
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
            sendKeepAlive(NetworkType.UDP);
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

    public LobbyIdentifier[] getAvailableLobbies() throws IOException {
        if (!isConnected()) {
            throw new IOException("Cannot ask for available lobbies while client is not connected.");
        }

        if (isListening) {
            throw new IOException("Cannot ask for available lobbies while listening for commands.");
        }

        sendRequest(NetworkType.TCP, RequestType.GetAvailableLobbies);

        return (LobbyIdentifier[]) tcpIn.readObject(LobbyIdentifier[].class);
    }

    public LobbyIdentifier createLobby() throws IOException {
        return createLobby(null);
    }

    public LobbyIdentifier createLobby(String lobbyName) throws IOException {
        if (connectionStatus.ordinal() < ConnectionStatus.InServer.ordinal()) {
            throw new IOException("Cannot create lobby while client is not connected.");
        }

        if (isListening) {
            throw new IOException("Cannot create lobby while listening for commands.");
        }

        sendRequest(NetworkType.TCP, RequestType.CreateLobby, serializer.writeObject(lobbyName, String.class));
        return (LobbyIdentifier) tcpIn.readObject(LobbyIdentifier.class);
    }

    public LobbyIdentifier joinLobby(UUID lobbyId) throws IOException {
        if (connectionStatus.ordinal() < ConnectionStatus.InServer.ordinal()) {
            throw new IOException("Cannot join lobby while client is not connected.");
        }

        if (isListening) {
            throw new IOException("Cannot join lobby while listening for commands.");
        }

        sendRequest(NetworkType.TCP, RequestType.JoinLobby, serializer.writeObject(lobbyId, UUID.class));
        return (LobbyIdentifier) tcpIn.readObject(LobbyIdentifier.class);
    }

    @Override
    public Logger getLogger() {
        return ClientLogger;
    }

    @Override
    public synchronized void sendCommand(NetworkType networkType, CommandTarget commandTarget, Command.Id commandId, byte[] rawData)
            throws IOException {
        ClientLogger.trace("{} sending {} \"{}\" to {}:{}", clientId, networkType.name(), commandId.name(), clientConfig.address(), clientConfig.port());

        switch (networkType) {
            case TCP -> SendUtils.sendTCPCommand(tcpOut, commandTarget, commandId, rawData);
            case UDP -> SendUtils.sendUDPCommand(udpSocket, clientConfig, commandTarget, commandId, clientId, rawData);
        }
    }

    public void sendRequest(NetworkType networkType, RequestType requestType, byte[] rawData) throws IOException {
        ClientLogger.trace("{} sending {} \"{}\" to {}:{}", clientId, networkType.name(), requestType.name(), clientConfig.address(), clientConfig.port());

        switch (networkType) {
            case TCP -> SendUtils.sendTCPRequest(tcpOut, requestType, rawData);
            case UDP -> SendUtils.sendUDPRequest(udpSocket, clientConfig, requestType, clientId, rawData);
        }
    }

    @Override
    public void sendDisconnect(NetworkType networkType, byte[] rawData) throws IOException {
        ClientLogger.trace("{} sending {} disconnect to {}:{}", clientId, networkType.name(), clientConfig.address(), clientConfig.port());

        switch (networkType) {
            case TCP -> SendUtils.sendTCPDisconnect(tcpOut);
            case UDP -> SendUtils.sendUDPDisconnect(clientId, udpSocket, clientConfig);
        }
    }

    @Override
    public void sendKeepAlive(NetworkType networkType) throws IOException {
        ClientLogger.trace("{} sending {} keep-alive to {}:{}", clientId, networkType.name(), clientConfig.address(), clientConfig.port());

        switch (networkType) {
            case TCP -> SendUtils.sendTCPKeepAlive(tcpOut);
            case UDP -> SendUtils.sendUDPKeepAlive(clientId, udpSocket, clientConfig);
        }
    }

    @Override
    protected void readMessageType(NetworkType networkType, UUID senderId, MessageInputStream inputStream, SentMessageType sentMessageType)
            throws IOException {
        switch (sentMessageType) {
            case KeepAlive -> ClientLogger.debug("{} Received {} keep-alive packet.", senderId, networkType);
            case Disconnect -> disconnect();
            case PingResponse -> {
                long currentTimestamp = System.nanoTime();
                long otherTimestamp = inputStream.readLong();
                long pingNanos = currentTimestamp - otherTimestamp;
                onPingReceived.accept(pingNanos);
            }
            case RPCCommand -> {
                CommandTarget commandTarget = (CommandTarget) inputStream.readObject(CommandTarget.class);
                long dataLength;

                if (networkType == NetworkType.TCP) {
                    dataLength = inputStream.readLong();
                } else {
                    dataLength = inputStream.available() - MessageUtils.UuidBytes;
                }

                UUID commandId = (UUID) inputStream.readObject(UUID.class);

                if (commandTarget != CommandTarget.Client) {
                    ClientLogger.warn("Received command \"{}\" targeted at {} instead of client", commandId, commandTarget.name());
                    inputStream.skipNBytes(dataLength);
                }

                readCommand(dataLength, commandId, inputStream, this);
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
        super.shutdown();
        udpSocket.close();
    }
}
