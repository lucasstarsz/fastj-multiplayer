package tech.fastj.network.rpc;

import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.SentMessageType;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.read.MessageInputStream;
import tech.fastj.network.serial.write.MessageOutputStream;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class ClientBase<H extends Enum<H> & CommandAlias> implements NetworkSender, CommandReader<H> {

    protected final Serializer serializer;
    protected final ClientConfig clientConfig;
    protected final Socket tcpSocket;
    protected final DatagramSocket udpSocket;

    protected UUID clientId;
    protected ConnectionStatus connectionStatus;

    protected MessageInputStream tcpIn;
    protected MessageOutputStream tcpOut;

    protected ExecutorService connectionListener;
    protected boolean isListening;

    private Runnable onDisconnect;

    protected ClientBase(Socket tcpSocket, DatagramSocket udpServer) throws IOException {
        this.clientConfig = new ClientConfig(tcpSocket.getInetAddress(), tcpSocket.getPort());
        this.clientId = UUID.randomUUID();
        this.serializer = new Serializer();

        this.tcpSocket = tcpSocket;
        this.tcpSocket.setSoTimeout(10000);
        udpSocket = udpServer;

        onDisconnect = () -> {
        };
    }

    protected ClientBase(ClientConfig clientConfig) throws IOException {
        this.clientConfig = clientConfig;
        this.serializer = new Serializer();

        tcpSocket = new Socket();
        tcpSocket.setSoTimeout(10000);
        udpSocket = new DatagramSocket();

        onDisconnect = () -> {
        };
    }

    public void connect() throws IOException {
        if (!tcpSocket.isConnected()) {
            getLogger().debug("{} connecting TCP to {}:{}...", clientId, clientConfig.address(), clientConfig.port());

            InetSocketAddress address = new InetSocketAddress(clientConfig.address(), clientConfig.port());
            tcpSocket.connect(address);
        }

        tcpOut = new MessageOutputStream(tcpSocket.getOutputStream(), getSerializer());
        tcpOut.flush();

        tcpIn = new MessageInputStream(tcpSocket.getInputStream(), getSerializer());
        connectionStatus = ConnectionStatus.InServer;

        getLogger().debug("{} connected on TCP to {}:{}.", clientId, clientConfig.address(), clientConfig.port());
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public boolean isConnected() {
        return connectionStatus.ordinal() >= ConnectionStatus.InServer.ordinal();
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public UUID getClientId() {
        return clientId;
    }

    public boolean isListening() {
        return isListening;
    }

    public void setOnDisconnect(Runnable onDisconnect) {
        this.onDisconnect = onDisconnect;
    }

    @Override
    public Serializer getSerializer() {
        return serializer;
    }

    public void startListening() {
        if (isListening) {
            getLogger().warn("Client {} is already listening to {}:{}.", clientId, clientConfig.address(), clientConfig.port());
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

    public void stopListening() {
        if (!isListening) {
            getLogger().warn("Client {} is not listening to anything.", clientId);
            return;
        }

        isListening = false;

        if (connectionListener != null) {
            if (!connectionListener.isShutdown()) {
                connectionListener.shutdownNow();
            }

            connectionListener = null;
        }
    }

    protected void listenTCP() {
        getLogger().debug("{} started listening on TCP.", clientId);

        while (isListening && !tcpSocket.isClosed()) {
            try {
                SentMessageType sentMessageType = (SentMessageType) tcpIn.readObject(SentMessageType.class);
                readMessageType(NetworkType.TCP, clientId, tcpIn, sentMessageType);
            } catch (SocketException | EOFException exception) {
                getLogger().warn("{} Error receiving TCP packet: {}", clientId, exception.getMessage());

                connectionStatus = ConnectionStatus.Disconnected;
                disconnect();

                break;
            } catch (IOException exception) {
                if (!tcpSocket.isClosed() && isListening) {
                    try {
                        getLogger().error(clientId + " Error receiving TCP packet", exception);
                        getLogger().warn("discarding data: {}", Arrays.toString(tcpIn.readAllBytes()));
                    } catch (IOException e) {
                        getLogger().warn("unable to discard {}'s TCP data fully.", clientId);
                    }
                } else {
                    getLogger().warn("IOException while reading TCP packet: {}, {}", exception.getMessage(), exception);
                }
            } catch (Exception exception) {
                try {
                    getLogger().error("Exception while reading TCP packet: " + exception.getMessage(), exception);
                    getLogger().warn("discarding data: {}", Arrays.toString(tcpIn.readAllBytes()));
                } catch (IOException e) {
                    getLogger().warn("unable to discard {}'s TCP data fully.", clientId);
                }
            }
        }

        getLogger().debug("{} stopped listening on TCP.", clientId);
    }

    protected void listenUDP() {
        getLogger().debug("{} started listening on UDP.", clientId);

        while (isListening && !udpSocket.isClosed()) {
            try {
                byte[] receivePacketBuffer = new byte[SendUtils.UdpPacketBufferLength];
                DatagramPacket packet = new DatagramPacket(receivePacketBuffer, SendUtils.UdpPacketBufferLength);

                udpSocket.receive(packet);

                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, data.length);

                MessageInputStream tempStream = new MessageInputStream(new ByteArrayInputStream(data), getSerializer());
                UUID senderId = (UUID) tempStream.readObject(UUID.class);
                SentMessageType sentMessageType = (SentMessageType) tempStream.readObject(SentMessageType.class);

                readMessageType(NetworkType.UDP, senderId, tempStream, sentMessageType);
            } catch (SocketException exception) {
                getLogger().warn("{} Error receiving UDP packet: {}", clientId, exception.getMessage());

                connectionStatus = ConnectionStatus.Disconnected;
                disconnect();

                break;
            } catch (IOException exception) {
                if (!udpSocket.isClosed() && isListening) {
                    getLogger().error("IOException receiving UDP packet", exception);
                } else {
                    getLogger().warn("IOException while reading UDP packet: {}, {}", exception.getMessage(), exception);
                }

                getLogger().warn("Discarding packet.");
            } catch (Exception exception) {
                getLogger().error("Exception while reading UDP packet", exception);
                getLogger().warn("Discarding packet.");
            }
        }

        getLogger().debug("{} stopped listening on UDP.", clientId);
    }

    protected abstract void readMessageType(NetworkType tcp, UUID senderId, MessageInputStream in, SentMessageType sentMessageType)
        throws IOException;

    public void disconnect() {
        try {
            if (isConnected()) {
                sendDisconnect(NetworkType.TCP);
            }

            connectionStatus = ConnectionStatus.Disconnected;
            shutdown();
        } catch (IOException shutdownException) {
            getLogger().error(clientId + " Error shutting down socket(s)", shutdownException);
        } finally {
            onDisconnect.run();
        }
    }

    protected void shutdown() throws IOException {
        getLogger().debug("{} shutting down", clientId);

        stopListening();
        tcpSocket.close();
    }
}
