package tech.fastj.network.rpc;

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
import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client extends ConnectionHandler<Client> implements Runnable, NetworkSender {

    private static final Logger ClientLogger = LoggerFactory.getLogger(Client.class);

    public static final int Leave = 1;
    public static final int Join = 0;

    public Client(ClientConfig clientConfig) throws IOException {
        super(clientConfig);
    }

    @Override
    public void connect() throws IOException {
        super.connect();

        ClientLogger.debug("{} checking server connection status...", clientId);

        int verification = tcpIn.readInt();
        if (verification != Client.Join) {
            disconnect();
            throw new IOException("Failed to join server " + clientConfig.address() + ":" + clientConfig.port() + ", connection status was " + verification + ".");
        }

        clientId = (UUID) tcpIn.readObject(UUID.class);
        ClientLogger.debug("Client id synced to server, now {}.", clientId);
        ClientLogger.debug("{} connection status to {}:{} satisfactory.", clientId, clientConfig.address(), clientConfig.port());
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
            case UDP -> SendUtils.sendUDPDisconnect(udpSocket, clientConfig);
        }
    }

    @Override
    protected void readMessageType(NetworkType networkType, UUID senderId, MessageInputStream inputStream, SentMessageType sentMessageType)
            throws IOException {
        switch (sentMessageType) {
            case Disconnect -> disconnect();
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
