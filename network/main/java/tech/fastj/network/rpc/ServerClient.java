package tech.fastj.network.rpc;

import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.SentMessageType;
import tech.fastj.network.rpc.message.SpecialRequestType;
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
    public synchronized void sendCommand(NetworkType networkType, Command.Id commandId, byte[] rawData) throws IOException {
        ServerClientLogger.trace("{} sending {} \"{}\" to {}:{}", clientId, networkType.name(), commandId.name(), clientConfig.address(), clientConfig.port());

        switch (networkType) {
            case TCP -> SendUtils.sendTCPCommand(tcpOut, commandId, rawData);
            case UDP -> SendUtils.sendUDPCommand(udpSocket, clientConfig, commandId, rawData);
        }
    }

    @Override
    public void sendSpecialRequest(NetworkType networkType, SpecialRequestType specialRequestType, byte[] rawData) throws IOException {
        ServerClientLogger.trace("{} sending {} \"{}\" to {}:{}", clientId, networkType.name(), specialRequestType.name(), clientConfig.address(), clientConfig.port());

        switch (networkType) {
            case TCP -> SendUtils.sendTCPSpecialRequest(tcpOut, specialRequestType, rawData);
            case UDP -> SendUtils.sendUDPSpecialRequest(udpSocket, clientConfig, specialRequestType, rawData);
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
    protected void readMessageType(MessageInputStream inputStream, SentMessageType sentMessageType) throws IOException {
        switch (sentMessageType) {
            case Disconnect -> disconnect();
            case PingRequest -> {
            }
            case RPCCommand -> {
                UUID commandId = (UUID) inputStream.readObject(UUID.class);
                server.receiveCommand(commandId, this, inputStream);
            }
            case SpecialRequest -> {
                SpecialRequestType requestType = (SpecialRequestType) inputStream.readObject(SpecialRequestType.class);

                getClientLogger().trace("{} received special request: {}", clientId, requestType);

                server.receiveSpecialRequest(requestType, this, inputStream);
            }
        }
    }
}
