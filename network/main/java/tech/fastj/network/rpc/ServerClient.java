package tech.fastj.network.rpc;

import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.RequestType;
import tech.fastj.network.rpc.message.SentMessageType;
import tech.fastj.network.rpc.message.prebuilt.LobbyIdentifier;
import tech.fastj.network.rpc.message.prebuilt.SessionIdentifier;
import tech.fastj.network.serial.read.MessageInputStream;
import tech.fastj.network.serial.util.MessageUtils;
import tech.fastj.network.serial.write.MessageOutputStream;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerClient extends ConnectionHandler<ServerClient> implements Runnable, NetworkSender {

    private final Logger ServerClientLogger = LoggerFactory.getLogger(ServerClient.class);

    private final Server server;

    private ClientConfig udpConfig;

    public ServerClient(Socket socket, Server server, DatagramSocket udpServer) throws IOException {
        super(socket, udpServer);
        this.server = server;
        serializer.registerSerializer(SessionIdentifier.class);
        serializer.registerSerializer(LobbyIdentifier.class);
    }

    public MessageOutputStream getTcpOut() {
        return tcpOut;
    }

    @Override
    public Logger getLogger() {
        return ServerClientLogger;
    }

    @Override
    public void connect() throws IOException {
        super.connect();
        ServerClientLogger.debug("{} syncing client id.", clientId);

        tcpOut.writeInt(Client.Join);
        tcpOut.writeObject(clientId, UUID.class);
        tcpOut.flush();

        int udpPort = tcpIn.readInt();

        ServerClientLogger.debug("Received port: {}", udpPort);

        udpConfig = new ClientConfig(tcpSocket.getInetAddress(), udpPort);

        ServerClientLogger.debug("{} connected on UDP to {}:{}.", clientId, clientConfig.address(), clientConfig.port());
    }

    @Override
    public synchronized void sendCommand(NetworkType networkType, CommandTarget commandTarget, Command.Id commandId, byte[] rawData)
        throws IOException {
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
        ServerClientLogger.trace("{} sending {} keep-alive to {}:{}", clientId, networkType.name(), clientConfig.address(), clientConfig.port());

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

    @Override
    protected void readMessageType(NetworkType networkType, UUID senderId, MessageInputStream inputStream, SentMessageType sentMessageType)
        throws IOException {
        switch (sentMessageType) {
            case KeepAlive -> {
                ServerClientLogger.debug("{} Received {} keep-alive packet. Now returning the favor.", senderId, networkType);
                sendKeepAlive(networkType);
            }
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

                UUID commandId = (UUID) inputStream.readObject(UUID.class);

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

    public void sendPingResponse(long timestamp) throws IOException {
        byte[] packetData = ByteBuffer.allocate(MessageUtils.UuidBytes + MessageUtils.EnumBytes + Long.BYTES)
            .putLong(clientId.getMostSignificantBits())
            .putLong(clientId.getLeastSignificantBits())
            .putInt(SentMessageType.PingResponse.ordinal())
            .putLong(timestamp)
            .array();

        ServerClientLogger.trace("{} sending ping response to {}:{}", clientId, clientConfig.address(), clientConfig.port());

        DatagramPacket packet = SendUtils.buildPacket(udpConfig, packetData);
        udpSocket.send(packet);
    }

    @Override
    public void disconnect() {
        server.disconnectClient(this);
    }
}
