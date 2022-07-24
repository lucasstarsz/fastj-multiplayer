package tech.fastj.network.rpc;

import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.RequestType;
import tech.fastj.network.rpc.message.SentMessageType;
import tech.fastj.network.serial.read.MessageInputStream;
import tech.fastj.network.serial.write.MessageOutputStream;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerClient extends ConnectionHandler<ServerClient> implements Runnable, NetworkSender {

    private final Logger ServerClientLogger = LoggerFactory.getLogger(ServerClient.class);

    private final Server server;

    public ServerClient(Socket socket, Server server, DatagramSocket udpServer) throws IOException {
        super(socket, udpServer);
        this.server = server;
    }

    public MessageOutputStream getTcpOut() {
        return tcpOut;
    }

    @Override
    public Logger getClientLogger() {
        return ServerClientLogger;
    }

    @Override
    public void connect() throws IOException {
        super.connect();

        ServerClientLogger.debug("{} syncing client id.", clientId);

        tcpOut.writeInt(Client.Join);
        tcpOut.writeObject(clientId, UUID.class);
        tcpOut.flush();
    }

    @Override
    public synchronized void sendCommand(NetworkType networkType, CommandTarget commandTarget, Command.Id commandId, byte[] rawData)
            throws IOException {
        ServerClientLogger.trace("{} sending {} \"{}\" to {}:{}", clientId, networkType.name(), commandId.name(), clientConfig.address(), clientConfig.port());

        switch (networkType) {
            case TCP -> SendUtils.sendTCPCommand(tcpOut, commandTarget, commandId, rawData);
            case UDP -> SendUtils.sendUDPCommand(udpSocket, clientConfig, commandTarget, commandId, clientId, rawData);
        }
    }

    @Override
    public void sendRequest(NetworkType networkType, RequestType requestType, byte[] rawData) throws IOException {
        ServerClientLogger.trace("{} sending {} \"{}\" to {}:{}", clientId, networkType.name(), requestType.name(), clientConfig.address(), clientConfig.port());

        switch (networkType) {
            case TCP -> SendUtils.sendTCPRequest(tcpOut, requestType, rawData);
            case UDP -> SendUtils.sendUDPRequest(udpSocket, clientConfig, requestType, clientId, rawData);
        }
    }

    @Override
    public void sendDisconnect(NetworkType networkType, byte[] rawData) throws IOException {
        ServerClientLogger.trace("{} sending {} disconnect to {}:{}", clientId, networkType.name(), clientConfig.address(), clientConfig.port());

        switch (networkType) {
            case TCP -> SendUtils.sendTCPDisconnect(tcpOut);
            case UDP -> SendUtils.sendUDPDisconnect(udpSocket, clientConfig);
        }
    }

    @Override
    protected void readMessageType(UUID senderId, MessageInputStream inputStream, SentMessageType sentMessageType) throws IOException {
        switch (sentMessageType) {
            case Disconnect -> disconnect();
            case PingRequest -> {
            }
            case RPCCommand -> {
                CommandTarget commandTarget = (CommandTarget) inputStream.readObject(CommandTarget.class);
                UUID commandId = (UUID) inputStream.readObject(UUID.class);
                server.receiveCommand(commandTarget, commandId, senderId, inputStream);
            }
            case Request -> {
                RequestType requestType = (RequestType) inputStream.readObject(RequestType.class);

                getClientLogger().trace("{} received special request: {}", senderId, requestType);

                server.receiveRequest(requestType, senderId, inputStream);
            }
        }
    }
}
