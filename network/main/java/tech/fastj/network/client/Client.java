package tech.fastj.network.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client implements Runnable {

    private final Logger clientLogger = LoggerFactory.getLogger(Client.class);

    public static final byte Leave = -1;
    public static final byte Join = 0;
    public static final int PacketBufferLength = 256;

    private final Socket tcpSocket;
    private final DatagramSocket udpSocket;
    private ObjectInputStream tcpIn;
    private ObjectOutputStream tcpOut;

    private final ClientConfig clientConfig;
    private final UUID clientId;

    private ExecutorService connectionListener;
    private boolean isRunning;
    private final boolean isServerSide;

    public Client(Socket socket, ClientListener clientListener, DatagramSocket udpServer) throws IOException {
        this.clientConfig = new ClientConfig(socket.getLocalAddress(), socket.getLocalPort(), clientListener);
        this.clientId = UUID.randomUUID();

        tcpSocket = socket;
        tcpSocket.setSoTimeout(10000);
        udpSocket = udpServer;
        isServerSide = true;
    }

    public Client(ClientConfig clientConfig) throws IOException {
        this.clientConfig = clientConfig;
        this.clientId = UUID.randomUUID();

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
        }

        tcpOut = new ObjectOutputStream(tcpSocket.getOutputStream());
        tcpOut.flush();

        tcpIn = new ObjectInputStream(tcpSocket.getInputStream());

        if (!isServerSide) {
            clientLogger.debug("{} checking server connection status...", clientId);

            int verification = tcpIn.readInt();
            if (verification != Client.Join) {
                throw new IOException("Failed to join server " + clientConfig.address() + ":" + clientConfig.port() + ", connection status was " + verification + ".");
            }

            clientLogger.debug("{} connection status satisfactory. Connected to {}:{}.", clientId, clientConfig.address(), clientConfig.port());
        } else {
            clientLogger.debug("{} connection status satisfactory. Joined server {}:{}.", clientId, clientConfig.address(), clientConfig.port());
        }
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public UUID getClientId() {
        return clientId;
    }

    public ObjectInputStream getTcpIn() {
        return tcpIn;
    }

    public void sendTCP(int identifier) throws IOException {
        sendTCP(identifier, null);
    }

    public synchronized void sendTCP(int identifier, Object data) throws IOException {
        if (tcpOut == null) {
            return;
        }

        clientLogger.trace("{} sending tcp {} to {}:{}", clientId, identifier, clientConfig.address(), clientConfig.port());

        tcpOut.writeInt(identifier);

        if (data != null) {
            if (data instanceof String s) {
                tcpOut.writeUTF(s);
            } else if (data instanceof Integer i) {
                tcpOut.writeInt(i);
            } else if (data instanceof Double d) {
                tcpOut.writeDouble(d);
            } else if (data instanceof Long l) {
                tcpOut.writeLong(l);
            } else if (data instanceof Float f) {
                tcpOut.writeFloat(f);
            } else if (data instanceof Byte b) {
                tcpOut.writeByte(b);
            } else if (data instanceof Boolean b) {
                tcpOut.writeBoolean(b);
            } else if (data instanceof Character c) {
                tcpOut.writeChar(c);
            } else if (data instanceof Short s) {
                tcpOut.writeShort(s);
            } else if (data instanceof byte[] bytes) {
                tcpOut.write(bytes);
            } else {
                tcpOut.writeObject(data);
            }
        }

        tcpOut.flush();
    }

    public void sendUDP(byte[] data) throws IOException {
        assert data.length <= PacketBufferLength;

        clientLogger.trace("{} sending udp {} to {}:{}", clientId, ByteBuffer.wrap(data).getInt(), clientConfig.address(), clientConfig.port());

        DatagramPacket packet = new DatagramPacket(data, data.length, clientConfig.address(), clientConfig.port());
        udpSocket.send(packet);
    }

    @Override
    public void run() {
        if (isRunning) {
            clientLogger.warn("Client {} is already listening to {}:{}.", clientId, clientConfig.address(), clientConfig.port());
            return;
        }

        isRunning = true;

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

        while (isRunning && !tcpSocket.isClosed()) {
            try {
                if (!isServerSide) {
                    clientLogger.trace("{} waiting for new TCP data...", clientId);
                }

                int identifier = tcpIn.readInt();

                if (!isServerSide) {
                    clientLogger.trace("{} received new TCP data.", clientId);
                }
                clientConfig.clientListener().receiveTCP(identifier, this);
            } catch (IOException exception) {
                tryCloseConnection(exception);
                break;
            }
        }

        clientLogger.debug("{} {} no longer listening on TCP.", isServerSide ? "Server-side" : "Client-side", clientId);
    }

    private void listenUDP() {
        clientLogger.debug("{} {} begin listening on UDP.", isServerSide ? "Server-side" : "Client-side", clientId);

        while (isRunning && !udpSocket.isClosed()) {
            try {
                byte[] receivePacketBuffer = new byte[PacketBufferLength];
                DatagramPacket packet = new DatagramPacket(receivePacketBuffer, PacketBufferLength);

                clientLogger.debug("{} waiting for new UDP packet...", clientId);
                udpSocket.receive(packet);

                clientLogger.debug("{} received new UDP packet.", clientId);
                clientConfig.clientListener().receiveUDP(packet, this);
            } catch (IOException exception) {
                if (!udpSocket.isClosed() && isRunning) {
                    clientLogger.error(clientId + " Error receiving UDP packet", exception);
                    break;
                }
            }
        }

        clientLogger.debug("{} {} no longer listening on UDP.", isServerSide ? "Server-side" : "Client-side", clientId);
    }

    private void tryCloseConnection(IOException ioException) {
        try {
            if (tcpIn.read() == Client.Leave) {
                clientLogger.debug("{} Connection closed.", clientId);
            }
        } catch (IOException closeCheckException) {
            clientLogger.error(clientId + " Error reading connection close check (1/2)", ioException);
            clientLogger.error(clientId + " Error reading connection close check (2/2)", closeCheckException);
        } finally {
            try {
                shutdown();
            } catch (IOException shutdownException) {
                clientLogger.error(clientId + " Error shutting down socket(s)", shutdownException);
            }
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
