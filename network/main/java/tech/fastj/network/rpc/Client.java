package tech.fastj.network.rpc;

import tech.fastj.network.CommandSender;
import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.read.NetworkableInputStream;
import tech.fastj.network.serial.write.NetworkableOutputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client extends CommandSender implements Runnable {

    private final Logger clientLogger = LoggerFactory.getLogger(Client.class);

    public static final int Leave = 1;
    public static final int Join = 0;

    private final Socket tcpSocket;
    private final DatagramSocket udpSocket;
    private NetworkableInputStream tcpIn;
    private NetworkableOutputStream tcpOut;

    private final ClientConfig clientConfig;
    private final UUID clientId;

    private ExecutorService connectionListener;
    private boolean isConnected;
    private boolean isListening;

    private final Server server;
    private final boolean isServerSide;

    public Client(Socket socket, Server server, DatagramSocket udpServer) throws IOException {
        this.clientConfig = new ClientConfig(socket.getLocalAddress(), socket.getLocalPort());
        this.clientId = UUID.randomUUID();
        this.server = server;

        tcpSocket = socket;
        tcpSocket.setSoTimeout(10000);
        udpSocket = udpServer;
        isServerSide = true;
    }

    public Client(ClientConfig clientConfig) throws IOException {
        this.clientConfig = clientConfig;
        this.clientId = UUID.randomUUID();
        this.server = null;

        tcpSocket = new Socket();
        tcpSocket.setSoTimeout(10000);
        udpSocket = new DatagramSocket();
        isServerSide = false;
    }

    public void connect() throws IOException {
        InetSocketAddress address = new InetSocketAddress(clientConfig.address(), clientConfig.port());
        if (!tcpSocket.isConnected()) {
            clientLogger.debug("{} connecting TCP to {}:{}...", clientId, clientConfig.address(), clientConfig.port());

            tcpSocket.connect(address);
            isConnected = true;
        }

        tcpOut = new NetworkableOutputStream(tcpSocket.getOutputStream(), serializer);
        tcpOut.flush();

        tcpIn = new NetworkableInputStream(tcpSocket.getInputStream(), serializer);

        if (!isServerSide) {
            clientLogger.debug("{} checking server connection status...", clientId);

            int verification = tcpIn.readInt();
            if (verification != Client.Join) {
                disconnect();
                throw new IOException("Failed to join server " + clientConfig.address() + ":" + clientConfig.port() + ", connection status was " + verification + ".");
            }

            clientLogger.debug("{} connection status satisfactory. Connected to {}:{}.", clientId, clientConfig.address(), clientConfig.port());
        } else {
            clientLogger.debug("{} connection status satisfactory. Joined server {}:{}.", clientId, clientConfig.address(), clientConfig.port());
        }

        while (tcpIn.available() > 0) {
            tcpIn.skipNBytes(tcpIn.available());
        }
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public UUID getClientId() {
        return clientId;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public NetworkableOutputStream getTcpOut() {
        return tcpOut;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isListening() {
        return isListening;
    }

    @Override
    public synchronized void sendTCP(Command.Id commandId, byte[] rawData) throws IOException {
        clientLogger.trace("{} sending tcp \"{}\" to {}:{}", clientId, commandId.name(), clientConfig.address(), clientConfig.port());
        SendUtils.sendTCP(tcpOut, commandId, rawData);
    }

    @Override
    public void sendUDP(Command.Id commandId, byte[] rawData) throws IOException {
        clientLogger.trace("{} sending udp {} to {}:{}", clientId, commandId.name(), clientConfig.address(), clientConfig.port());
        SendUtils.sendUDP(udpSocket, clientConfig, commandId, rawData);
    }

    @Override
    public void run() {
        if (isListening) {
            clientLogger.warn("Client {} is already listening to {}:{}.", clientId, clientConfig.address(), clientConfig.port());
            return;
        }

        isListening = true;

        if (connectionListener != null) {
            if (!connectionListener.isShutdown()) {
                connectionListener.shutdownNow();
            }
            connectionListener = null;
        }

        connectionListener = Executors.newFixedThreadPool(2);
        connectionListener.submit(this::listenTCP);
        connectionListener.submit(this::listenUDP);
    }

    private void listenTCP() {
        clientLogger.debug("{} {} begin listening on TCP.", isServerSide ? "Server-side" : "Client-side", clientId);

        while (isListening && !tcpSocket.isClosed()) {
            try {
                clientLogger.trace("{} {} waiting for new TCP data...", isServerSide ? "Server-side" : "Client-side", clientId);

                UUID commandId = (UUID) tcpIn.readObject(UUID.class);

                clientLogger.trace("{} {} received new TCP data.", isServerSide ? "Server-side" : "Client-side", clientId);

                if (isServerSide) {
                    server.receiveCommand(commandId, this, tcpIn);
                } else {
                    readCommand(commandId, tcpIn, this);
                }
            } catch (IOException exception) {
                if (!udpSocket.isClosed() && isListening) {
                    clientLogger.error(clientId + " Error receiving UDP packet", exception);
                    break;
                }
            }
        }

        clientLogger.debug("{} {} no longer listening on TCP.", isServerSide ? "Server-side" : "Client-side", clientId);
    }

    private void listenUDP() {
        clientLogger.debug("{} {} begin listening on UDP.", isServerSide ? "Server-side" : "Client-side", clientId);

        while (isListening && !udpSocket.isClosed()) {
            try {
                byte[] receivePacketBuffer = new byte[SendUtils.UdpPacketBufferLength];
                DatagramPacket packet = new DatagramPacket(receivePacketBuffer, SendUtils.UdpPacketBufferLength);

                clientLogger.debug("{} {} waiting for new UDP packet...", isServerSide ? "Server-side" : "Client-side", clientId);
                udpSocket.receive(packet);

                clientLogger.debug("{} {} received new UDP packet.", isServerSide ? "Server-side" : "Client-side", clientId);
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, data.length);

                NetworkableInputStream tempStream = new NetworkableInputStream(new ByteArrayInputStream(data), serializer);
                UUID commandId = (UUID) tempStream.readObject(UUID.class);

                if (isServerSide) {
                    server.receiveCommand(commandId, this, tempStream);
                } else {
                    readCommand(commandId, tempStream, this);
                }
            } catch (IOException exception) {
                if (!udpSocket.isClosed() && isListening) {
                    clientLogger.error(clientId + " Error receiving UDP packet", exception);
                    break;
                }
            }
        }

        clientLogger.debug("{} {} no longer listening on UDP.", isServerSide ? "Server-side" : "Client-side", clientId);
    }

    public void disconnect() {
        try {
            shutdown();
        } catch (IOException shutdownException) {
            clientLogger.error(clientId + " Error shutting down socket(s)", shutdownException);
        }
    }

    private void shutdown() throws IOException {
        clientLogger.debug("{} shutting down", clientId);
        tcpSocket.close();

        if (!isServerSide) {
            udpSocket.close();
        }
    }
}
