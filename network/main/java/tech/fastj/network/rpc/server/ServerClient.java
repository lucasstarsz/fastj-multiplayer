package tech.fastj.network.rpc.server;

import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.rpc.ClientBase;
import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.SendUtils;
import tech.fastj.network.rpc.local.LocalClient;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.RequestType;
import tech.fastj.network.rpc.message.SentMessageType;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;
import tech.fastj.network.rpc.message.prebuilt.SessionIdentifier;
import tech.fastj.network.rpc.server.command.ServerCommand;
import tech.fastj.network.rpc.server.command.ServerCommandReader;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.read.MessageInputStream;
import tech.fastj.network.serial.util.MessageUtils;
import tech.fastj.network.serial.write.MessageOutputStream;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerClient<E extends Enum<E> & CommandAlias> extends ClientBase<E> implements ServerCommandReader<E> {

    private final Logger ServerClientLogger = LoggerFactory.getLogger(ServerClient.class);

    private final Server<E> server;
    private final Class<E> aliasClass;
    private final Map<E, ServerCommand> commands;

    private ClientConfig udpConfig;

    public ServerClient(Socket socket, Server<E> server, DatagramSocket udpServer, Class<E> aliasClass) throws IOException {
        super(socket, udpServer);

        this.server = server;
        this.aliasClass = aliasClass;
        this.commands = new EnumMap<>(aliasClass);

        serializer.registerSerializer(SessionIdentifier.class);
        serializer.registerSerializer(LobbyIdentifier.class);

        for (E commandAlias : aliasClass.getEnumConstants()) {
            commandAlias.registerMessages(serializer);
        }

        resetCommands();
    }

    public MessageOutputStream getTcpOut() {
        return tcpOut;
    }

    @Override
    public Logger getLogger() {
        return ServerClientLogger;
    }

    @Override
    public Class<E> getAliasClass() {
        return aliasClass;
    }

    @Override
    public Map<E, ServerCommand> getCommands() {
        return commands;
    }

    @Override
    public void connect() throws IOException {
        super.connect();

        ServerClientLogger.debug("{} syncing client id.", clientId);

        tcpOut.writeInt(LocalClient.Join);
        tcpOut.writeObject(clientId, UUID.class);
        tcpOut.flush();

        int udpPort = tcpIn.readInt();
        udpConfig = new ClientConfig(tcpSocket.getInetAddress(), udpPort);

        ServerClientLogger.debug("Received port: {}", udpPort);
        ServerClientLogger.debug("{} connected on UDP to {}:{}.", clientId, clientConfig.address(), clientConfig.port());
    }

    @Override
    public Serializer getSerializer() {
        return serializer;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void readMessageType(NetworkType networkType, UUID senderId, MessageInputStream inputStream, SentMessageType sentMessageType)
        throws IOException {
        switch (sentMessageType) {
            case KeepAlive -> sendKeepAlive(networkType);
            case Disconnect -> disconnect();
            case PingRequest -> {
                long timestamp = inputStream.readLong();
                server.sendPingResponse(senderId, timestamp, inputStream);
            }
            case RPCCommand -> {
                CommandTarget commandTarget = (CommandTarget) inputStream.readObject(CommandTarget.class);
                long dataLength;

                if (networkType == NetworkType.TCP) {
                    dataLength = inputStream.readLong();
                } else {
                    dataLength = inputStream.available() - MessageUtils.UuidBytes;
                }

                E commandId = (E) inputStream.readObject(aliasClass);

                getLogger().trace("{} received RPC command \"{}\" targeting {} with length {}", senderId, commandId, commandTarget, dataLength);

                server.receiveCommand(commandTarget, dataLength, commandId, senderId, inputStream);
            }
            case Request -> {
                RequestType requestType = (RequestType) inputStream.readObject(RequestType.class);
                long dataLength;

                if (networkType == NetworkType.TCP) {
                    dataLength = inputStream.readLong();
                } else {
                    dataLength = inputStream.available() - MessageUtils.UuidBytes;
                }

                getLogger().trace("{} received special request: {}", senderId, requestType);

                server.receiveRequest(requestType, dataLength, senderId, inputStream);
            }
            default -> ServerClientLogger.warn(
                "{} Received unused message type {}, discarding {}",
                senderId,
                sentMessageType.name(),
                Arrays.toString(inputStream.readAllBytes())
            );
        }
    }

    @Override
    public void disconnect() {
        try {
            super.disconnect();
        } finally {
            server.disconnectClient(this);
        }
    }

    @Override
    public synchronized void sendCommand(NetworkType networkType, CommandTarget commandTarget, Enum<? extends CommandAlias> commandId, byte[] rawData)
        throws IOException {
        assert aliasClass.isAssignableFrom(commandId.getClass());

        ServerClientLogger.trace("{} sending {} \"{}\" to {}:{}", clientId, networkType.name(), commandId.name(), clientConfig.address(), clientConfig.port());

        switch (networkType) {
            case TCP -> SendUtils.sendTCPCommand(tcpOut, commandTarget, commandId, rawData);
            case UDP -> SendUtils.sendUDPCommand(udpSocket, udpConfig, commandTarget, commandId, clientId, rawData);
        }
    }

    @Override
    public synchronized void sendRequest(NetworkType networkType, RequestType requestType, byte[] rawData) throws IOException {
        ServerClientLogger.trace("{} sending {} \"{}\" to {}:{}", clientId, networkType.name(), requestType.name(), clientConfig.address(), clientConfig.port());

        switch (networkType) {
            case TCP -> SendUtils.sendTCPRequest(tcpOut, requestType, rawData);
            case UDP -> SendUtils.sendUDPRequest(udpSocket, udpConfig, requestType, clientId, rawData);
        }
    }

    @Override
    public synchronized void sendDisconnect(NetworkType networkType, byte[] rawData) throws IOException {
        ServerClientLogger.trace("{} sending {} disconnect to {}:{}", clientId, networkType.name(), clientConfig.address(), clientConfig.port());

        switch (networkType) {
            case TCP -> SendUtils.sendTCPDisconnect(tcpOut);
            case UDP -> SendUtils.sendUDPDisconnect(clientId, udpSocket, udpConfig);
        }
    }

    @Override
    public synchronized void sendKeepAlive(NetworkType networkType) throws IOException {
        switch (networkType) {
            case TCP -> SendUtils.sendTCPKeepAlive(tcpOut);
            case UDP -> SendUtils.sendUDPKeepAlive(clientId, udpSocket, udpConfig);
        }
    }

    public synchronized void sendLobbyUpdate(LobbyIdentifier lobbyUpdate) throws IOException {
        ServerClientLogger.trace("{} sending TCP lobby update to {}:{}", clientId, clientConfig.address(), clientConfig.port());
        SendUtils.sendTCPLobbyUpdate(tcpOut, serializer.writeMessage(lobbyUpdate));
    }

    public synchronized void sendSessionUpdate(SessionIdentifier sessionUpdate) throws IOException {
        ServerClientLogger.trace("{} sending TCP session update to {}:{}", clientId, clientConfig.address(), clientConfig.port());
        SendUtils.sendTCPSessionUpdate(tcpOut, serializer.writeMessage(sessionUpdate));
    }

    public void sendPingResponse(long timestamp) throws IOException {
        byte[] packetData = ByteBuffer.allocate(MessageUtils.EnumBytes + Long.BYTES)
            .putInt(SentMessageType.PingResponse.ordinal())
            .putLong(timestamp)
            .array();

        tcpOut.write(packetData);
        tcpOut.flush();
    }
}
