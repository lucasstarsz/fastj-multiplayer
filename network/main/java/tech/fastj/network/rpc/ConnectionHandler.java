package tech.fastj.network.rpc;

import tech.fastj.network.CommandSender;
import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.serial.Serializer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;

public abstract class ConnectionHandler<T extends ConnectionHandler<?>> extends CommandHandler<T> implements Runnable, CommandSender {

    protected final Socket tcpSocket;
    protected final DatagramSocket udpSocket;

    protected final ClientConfig clientConfig;
    protected final UUID clientId;

    protected ExecutorService connectionListener;
    protected boolean isListening;

    protected ConnectionHandler(Socket socket, DatagramSocket udpServer) throws IOException {
        this.clientConfig = new ClientConfig(socket.getLocalAddress(), socket.getLocalPort());
        this.clientId = UUID.randomUUID();

        tcpSocket = socket;
        tcpSocket.setSoTimeout(10000);
        udpSocket = udpServer;
    }

    protected ConnectionHandler(ClientConfig clientConfig) throws IOException {
        this.clientConfig = clientConfig;
        this.clientId = UUID.randomUUID();

        tcpSocket = new Socket();
        tcpSocket.setSoTimeout(10000);
        udpSocket = new DatagramSocket();
    }

    public abstract void connect() throws IOException;

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

    public abstract void disconnect();

    protected abstract void listenTCP();

    protected abstract void listenUDP();

    protected abstract void shutdown() throws IOException;
}
