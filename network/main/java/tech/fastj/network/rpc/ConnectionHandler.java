package tech.fastj.network.rpc;

import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.SentMessageType;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.read.MessageInputStream;
import tech.fastj.network.serial.write.MessageOutputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;

public abstract class ConnectionHandler<T extends ConnectionHandler<?>> extends CommandHandler<T> implements Runnable, NetworkSender {

    protected final Socket tcpSocket;
    protected final DatagramSocket udpSocket;

    protected final ClientConfig clientConfig;
    protected UUID clientId;

    protected MessageInputStream tcpIn;
    protected MessageOutputStream tcpOut;

    private Consumer<T> onDisconnect;

    protected ConnectionStatus connectionStatus;

    protected ExecutorService connectionListener;
    protected boolean isListening;

    protected ConnectionHandler(Socket tcpSocket, DatagramSocket udpServer) throws IOException {
        this.clientConfig = new ClientConfig(tcpSocket.getLocalAddress(), tcpSocket.getLocalPort());
        this.clientId = UUID.randomUUID();

        this.tcpSocket = tcpSocket;
        this.tcpSocket.setSoTimeout(10000);
        udpSocket = udpServer;

        onDisconnect = connectionHandler -> {};
    }

    protected ConnectionHandler(ClientConfig clientConfig) throws IOException {
        this.clientConfig = clientConfig;

        tcpSocket = new Socket();
        tcpSocket.setSoTimeout(10000);
        udpSocket = new DatagramSocket();

        onDisconnect = connectionHandler -> {};
    }

    public void connect() throws IOException {
        if (!tcpSocket.isConnected()) {
            getClientLogger().debug("{} connecting to {}:{}...", clientId, clientConfig.address(), clientConfig.port());

            InetSocketAddress address = new InetSocketAddress(clientConfig.address(), clientConfig.port());
            tcpSocket.connect(address);
        }

        tcpOut = new MessageOutputStream(tcpSocket.getOutputStream(), serializer);
        tcpOut.flush();

        tcpIn = new MessageInputStream(tcpSocket.getInputStream(), serializer);
        connectionStatus = ConnectionStatus.InServer;

        getClientLogger().debug("{} connected to {}:{}.", clientId, clientConfig.address(), clientConfig.port());
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public boolean isConnected() {
        return connectionStatus.ordinal() >= ConnectionStatus.InServer.ordinal();
    }

    public abstract Logger getClientLogger();

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public UUID getClientId() {
        return clientId;
    }

    public boolean isListening() {
        return isListening;
    }

    public void setOnDisconnect(Consumer<T> onDisconnect) {
        this.onDisconnect = onDisconnect;
    }

    @Override
    public Serializer getSerializer() {
        return serializer;
    }

    @Override
    public void run() {
        if (isListening) {
            getClientLogger().warn("Client {} is already listening to {}:{}.", clientId, clientConfig.address(), clientConfig.port());
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

    protected void listenTCP() {
        getClientLogger().debug("{} begin listening on TCP.", clientId);

        while (isListening && !tcpSocket.isClosed()) {
            try {
                getClientLogger().trace("{} waiting for new TCP data...", clientId);

                SentMessageType sentMessageType = (SentMessageType) tcpIn.readObject(SentMessageType.class);

                getClientLogger().trace("{} received TCP: {}", clientId, sentMessageType);

                readMessageType(tcpIn, sentMessageType);
            } catch (IOException exception) {
                if (!tcpSocket.isClosed() && isListening) {
                    getClientLogger().error(clientId + " Error receiving TCP packet", exception);
                    break;
                }
            }
        }

        getClientLogger().debug("{} no longer listening on TCP.", clientId);
    }

    protected void listenUDP() {
        getClientLogger().debug("{} begin listening on UDP.", clientId);

        while (isListening && !udpSocket.isClosed()) {
            try {
                byte[] receivePacketBuffer = new byte[SendUtils.UdpPacketBufferLength];
                DatagramPacket packet = new DatagramPacket(receivePacketBuffer, SendUtils.UdpPacketBufferLength);

                getClientLogger().debug("{} waiting for new UDP packet...", clientId);

                udpSocket.receive(packet);

                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, data.length);

                MessageInputStream tempStream = new MessageInputStream(new ByteArrayInputStream(data), serializer);
                SentMessageType sentMessageType = (SentMessageType) tempStream.readObject(SentMessageType.class);

                getClientLogger().trace("{} received UDP: {}", clientId, sentMessageType);

                readMessageType(tempStream, sentMessageType);
            } catch (IOException exception) {
                if (!udpSocket.isClosed() && isListening) {
                    getClientLogger().error(clientId + " Error receiving UDP packet", exception);
                    break;
                }
            }
        }

        getClientLogger().debug("{} no longer listening on UDP.", clientId);
    }

    protected abstract void readMessageType(MessageInputStream in, SentMessageType sentMessageType) throws IOException;

    public void disconnect() {
        disconnect(NetworkType.TCP);
    }

    @SuppressWarnings("unchecked")
    public void disconnect(NetworkType disconnectNetworkType) {
        try {
            if (isConnected()) {
                sendDisconnect(disconnectNetworkType);
            }

            connectionStatus = ConnectionStatus.Disconnected;
            shutdown();
        } catch (IOException shutdownException) {
            getClientLogger().error(clientId + " Error shutting down socket(s)", shutdownException);
        } finally {
            onDisconnect.accept((T) this);
        }
    }

    protected void shutdown() throws IOException {
        getClientLogger().debug("{} shutting down", clientId);
        tcpSocket.close();
    }
}
