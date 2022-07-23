package tech.fastj.network.rpc;

import tech.fastj.network.CommandSender;
import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.serial.read.MessageInputStream;
import tech.fastj.network.serial.write.MessageOutputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client extends ConnectionHandler<Client> implements Runnable, CommandSender {

    private static final Logger ClientLogger = LoggerFactory.getLogger(Client.class);

    public static final int Leave = 1;
    public static final int Join = 0;

    private MessageInputStream tcpIn;
    private MessageOutputStream tcpOut;

    private boolean isConnected;

    public Client(ClientConfig clientConfig) throws IOException {
        super(clientConfig);
    }

    @Override
    public void connect() throws IOException {
        InetSocketAddress address = new InetSocketAddress(clientConfig.address(), clientConfig.port());

        if (!tcpSocket.isConnected()) {
            ClientLogger.debug("{} connecting TCP to {}:{}...", clientId, clientConfig.address(), clientConfig.port());

            tcpSocket.connect(address);
            isConnected = true;
        }

        tcpOut = new MessageOutputStream(tcpSocket.getOutputStream(), serializer);
        tcpOut.flush();

        tcpIn = new MessageInputStream(tcpSocket.getInputStream(), serializer);

        ClientLogger.debug("{} checking server connection status...", clientId);

        int verification = tcpIn.readInt();
        if (verification != Client.Join) {
            disconnect();
            throw new IOException("Failed to join server " + clientConfig.address() + ":" + clientConfig.port() + ", connection status was " + verification + ".");
        }

        ClientLogger.debug("{} connection status satisfactory. Connected to {}:{}.", clientId, clientConfig.address(), clientConfig.port());

        while (tcpIn.available() > 0) {
            tcpIn.skipNBytes(tcpIn.available());
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public Logger getClientLogger() {
        return ClientLogger;
    }

    @Override
    public synchronized void sendTCP(Command.Id commandId, byte[] rawData) throws IOException {
        ClientLogger.trace("{} sending tcp \"{}\" to {}:{}", clientId, commandId.name(), clientConfig.address(), clientConfig.port());
        SendUtils.sendTCP(tcpOut, commandId, rawData);
    }

    @Override
    public void sendUDP(Command.Id commandId, byte[] rawData) throws IOException {
        ClientLogger.trace("{} sending udp {} to {}:{}", clientId, commandId.name(), clientConfig.address(), clientConfig.port());
        SendUtils.sendUDP(udpSocket, clientConfig, commandId, rawData);
    }

    @Override
    protected void listenTCP() {
        ClientLogger.debug("{} begin listening on TCP.", clientId);

        while (isListening && !tcpSocket.isClosed()) {
            try {
                ClientLogger.trace("{} waiting for new TCP data...", clientId);

                UUID commandId = (UUID) tcpIn.readObject(UUID.class);

                ClientLogger.trace("{} received new TCP data.", clientId);

                readCommand(commandId, tcpIn, this);
            } catch (IOException exception) {
                if (!udpSocket.isClosed() && isListening) {
                    ClientLogger.error(clientId + " Error receiving UDP packet", exception);
                    break;
                }
            }
        }

        ClientLogger.debug("{} no longer listening on TCP.", clientId);
    }

    @Override
    protected void listenUDP() {
        ClientLogger.debug("{} begin listening on UDP.", clientId);

        while (isListening && !udpSocket.isClosed()) {
            try {
                byte[] receivePacketBuffer = new byte[SendUtils.UdpPacketBufferLength];
                DatagramPacket packet = new DatagramPacket(receivePacketBuffer, SendUtils.UdpPacketBufferLength);

                ClientLogger.debug("{} waiting for new UDP packet...", clientId);

                udpSocket.receive(packet);

                ClientLogger.debug("{} received new UDP packet.", clientId);

                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, data.length);

                MessageInputStream tempStream = new MessageInputStream(new ByteArrayInputStream(data), serializer);
                UUID commandId = (UUID) tempStream.readObject(UUID.class);

                readCommand(commandId, tempStream, this);
            } catch (IOException exception) {
                if (!udpSocket.isClosed() && isListening) {
                    ClientLogger.error(clientId + " Error receiving UDP packet", exception);
                    break;
                }
            }
        }

        ClientLogger.debug("{} no longer listening on UDP.", clientId);
    }

    @Override
    public void disconnect() {
        try {
            shutdown();
        } catch (IOException shutdownException) {
            ClientLogger.error(clientId + " Error shutting down sockets", shutdownException);
        }
    }

    @Override
    protected void shutdown() throws IOException {
        ClientLogger.debug("{} shutting down", clientId);
        tcpSocket.close();
        udpSocket.close();
    }
}
