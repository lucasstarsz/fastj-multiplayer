package tech.fastj.network.rpc;

import tech.fastj.network.CommandSender;
import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.serial.read.MessageInputStream;
import tech.fastj.network.serial.write.MessageOutputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerClient extends ConnectionHandler<ServerClient> implements Runnable, CommandSender {

    private final Logger ServerClientLogger = LoggerFactory.getLogger(ServerClient.class);

    private MessageInputStream tcpIn;
    private MessageOutputStream tcpOut;

    private boolean isConnected;

    private final Server server;

    public ServerClient(Socket socket, Server server, DatagramSocket udpServer) throws IOException {
        super(socket, udpServer);
        this.server = server;
    }

    @Override
    public void connect() throws IOException {
        InetSocketAddress address = new InetSocketAddress(clientConfig.address(), clientConfig.port());
        if (!tcpSocket.isConnected()) {
            ServerClientLogger.debug("{} connecting TCP to {}:{}...", clientId, clientConfig.address(), clientConfig.port());

            tcpSocket.connect(address);
            isConnected = true;
        }

        tcpOut = new MessageOutputStream(tcpSocket.getOutputStream(), serializer);
        tcpOut.flush();

        tcpIn = new MessageInputStream(tcpSocket.getInputStream(), serializer);

        ServerClientLogger.debug("{} connection status satisfactory. Joined server {}:{}.", clientId, clientConfig.address(), clientConfig.port());

        while (tcpIn.available() > 0) {
            tcpIn.skipNBytes(tcpIn.available());
        }
    }

    public MessageOutputStream getTcpOut() {
        return tcpOut;
    }

    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public Logger getClientLogger() {
        return ServerClientLogger;
    }

    @Override
    public synchronized void sendTCP(Command.Id commandId, byte[] rawData) throws IOException {
        ServerClientLogger.trace("{} sending tcp \"{}\" to {}:{}", clientId, commandId.name(), clientConfig.address(), clientConfig.port());
        SendUtils.sendTCP(tcpOut, commandId, rawData);
    }

    @Override
    public void sendUDP(Command.Id commandId, byte[] rawData) throws IOException {
        ServerClientLogger.trace("{} sending udp {} to {}:{}", clientId, commandId.name(), clientConfig.address(), clientConfig.port());
        SendUtils.sendUDP(udpSocket, clientConfig, commandId, rawData);
    }

    @Override
    protected void listenTCP() {
        ServerClientLogger.debug("{} begin listening on TCP.", clientId);

        while (isListening && !tcpSocket.isClosed()) {
            try {
                ServerClientLogger.trace("{} waiting for new TCP data...", clientId);

                UUID commandId = (UUID) tcpIn.readObject(UUID.class);

                ServerClientLogger.trace("{} received new TCP data.", clientId);

                server.receiveCommand(commandId, this, tcpIn);
            } catch (IOException exception) {
                if (!udpSocket.isClosed() && isListening) {
                    ServerClientLogger.error(clientId + " Error receiving UDP packet", exception);
                    break;
                }
            }
        }

        ServerClientLogger.debug("{} no longer listening on TCP.", clientId);
    }

    @Override
    protected void listenUDP() {
        ServerClientLogger.debug("{} begin listening on UDP.", clientId);

        while (isListening && !udpSocket.isClosed()) {
            try {
                byte[] receivePacketBuffer = new byte[SendUtils.UdpPacketBufferLength];
                DatagramPacket packet = new DatagramPacket(receivePacketBuffer, SendUtils.UdpPacketBufferLength);

                ServerClientLogger.debug("{} waiting for new UDP packet...", clientId);
                udpSocket.receive(packet);

                ServerClientLogger.debug("{} received new UDP packet.", clientId);
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, data.length);

                MessageInputStream tempStream = new MessageInputStream(new ByteArrayInputStream(data), serializer);
                UUID commandId = (UUID) tempStream.readObject(UUID.class);

                server.receiveCommand(commandId, this, tempStream);
            } catch (IOException exception) {
                if (!udpSocket.isClosed() && isListening) {
                    ServerClientLogger.error(clientId + " Error receiving UDP packet", exception);
                    break;
                }
            }
        }

        ServerClientLogger.debug("{} no longer listening on UDP.", clientId);
    }

    @Override
    public void disconnect() {
        try {
            shutdown();
        } catch (IOException shutdownException) {
            ServerClientLogger.error(clientId + " Error shutting down TCP socket", shutdownException);
        }
    }

    @Override
    protected void shutdown() throws IOException {
        ServerClientLogger.debug("{} shutting down", clientId);
        tcpSocket.close();
    }
}
