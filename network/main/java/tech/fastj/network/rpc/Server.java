package tech.fastj.network.rpc;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.fastj.network.config.ServerConfig;
import tech.fastj.network.rpc.commands.Command0;
import tech.fastj.network.rpc.commands.Command1;
import tech.fastj.network.rpc.commands.Command2;
import tech.fastj.network.rpc.commands.Command3;
import tech.fastj.network.rpc.commands.Command4;
import tech.fastj.network.rpc.commands.Command5;
import tech.fastj.network.rpc.commands.Command6;
import tech.fastj.network.serial.read.NetworkableInputStream;
import tech.fastj.network.serial.util.NetworkableStreamUtils;
import tech.fastj.network.serial.util.NetworkableUtils;

public class Server extends CommandHandler {

    private final List<Client> clients;

    private final ServerSocket tcpServer;
    private final DatagramSocket udpServer;

    private ExecutorService clientAccepter;

    private volatile boolean isRunning;
    private volatile boolean isAcceptingClients;

    private final Logger serverLogger = LoggerFactory.getLogger(Server.class);

    public Server(ServerConfig serverConfig) throws IOException {
        this.clients = new ArrayList<>(serverConfig.maxClients());

        tcpServer = new ServerSocket(serverConfig.port(), serverConfig.clientBacklog(), serverConfig.address());
        udpServer = new DatagramSocket(serverConfig.port(), serverConfig.address());
    }

    public List<Client> getClients() {
        return Collections.unmodifiableList(clients);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isAcceptingClients() {
        return isAcceptingClients;
    }

    public void stop() throws IOException {
        serverLogger.debug("stopping server");

        disallowClients();

        tcpServer.close();
        udpServer.close();

        isRunning = false;
    }

    public void allowClients() {
        if (!isRunning) {
            serverLogger.debug("Server not running.");
            return;
        }

        if (isAcceptingClients) {
            return;
        }

        if (clientAccepter == null || clientAccepter.isShutdown()) {
            clientAccepter = Executors.newSingleThreadExecutor();
        }

        isAcceptingClients = true;
        clientAccepter.submit(this::acceptClients);
    }

    public void disallowClients() {
        if (!isAcceptingClients) {
            return;
        }

        if (clientAccepter != null && !clientAccepter.isShutdown()) {
            clientAccepter.shutdownNow();
        }

        isAcceptingClients = false;
        serverLogger.debug("Server no longer accepting clients.");
    }

    private void acceptClients() {
        serverLogger.debug("Now accepting clients...");

        while (isAcceptingClients) {
            try {
                acceptClient();
            } catch (IOException exception) {
                if (!isRunning || !isAcceptingClients) {
                    break;
                }

                serverLogger.error("Failed to accept new client", exception);
            }
        }

        disallowClients();
    }

    private synchronized void acceptClient() throws IOException {
        Client client = null;

        try {
            serverLogger.debug("Accepting new client...");

            Socket clientSocket = tcpServer.accept();

            serverLogger.debug("Received new client, creating connection...");

            client = new Client(clientSocket, this, udpServer);
            client.connect();

            Map<String, UUID> aliasesToIds = getAliasesToIds();
            int dataCount = aliasesToIds.size();
            int dataLength = Integer.BYTES;

            for (Map.Entry<String, UUID> stringUUIDEntry : aliasesToIds.entrySet()) {
                dataLength += NetworkableUtils.MinStringBytes + stringUUIDEntry.getKey().length();
                dataLength += NetworkableUtils.UuidBytes;
            }

            ByteBuffer aliasesData = ByteBuffer.allocate(dataLength).putInt(dataCount);

            for (Map.Entry<String, UUID> aliasAndId : aliasesToIds.entrySet()) {
                String string = aliasAndId.getKey();
                UUID uuid = aliasAndId.getValue();

                if (string == null) {
                    aliasesData.putInt(NetworkableStreamUtils.Null);
                } else {
                    aliasesData.putInt(string.length());
                    aliasesData.put(string.getBytes(StandardCharsets.UTF_8));
                }

                if (uuid == null) {
                    aliasesData.putLong(NetworkableStreamUtils.Null);
                    aliasesData.putLong(NetworkableStreamUtils.Null);
                } else {
                    aliasesData.putLong(uuid.getMostSignificantBits());
                    long bits = uuid.getLeastSignificantBits();
                    aliasesData.putLong(bits);
                }
            }

            serverLogger.debug(
                    "Successful connection on {} to {}:{}. Sending success and command data...",
                    client.getClientId(),
                    client.getClientConfig().address(),
                    client.getClientConfig().port()
            );

            client.getTcpOut().writeInt(Client.Join);
            client.getTcpOut().write(aliasesData.array());
            client.getTcpOut().flush();

            serverLogger.debug("client {} connected.", client.getClientId());

            client.run();
            clients.add(client);
        } catch (IOException exception) {
            if (client != null && client.isConnected()) {
                client.getTcpOut().writeInt(Client.Leave);
                client.getTcpOut().flush();
                client.disconnect();
            }

            throw new IOException(
                    "Unable to connect to client"
                            + (client != null ? " " + client.getClientId() : ""),
                    exception
            );
        }
    }

    public void start() {
        isRunning = true;
    }

    public void receiveCommand(UUID commandId, Client client, NetworkableInputStream stream) throws IOException {
        readCommand(commandId, stream, client);
    }

    @Override
    public void addCommand(String alias, Command0 command) {
        super.registerAliasId(alias);
        super.addCommand(alias, command);
    }

    @Override
    public <T1> void addCommand(String alias, Class<T1> class1, Command1<T1> command) {
        super.registerAliasId(alias);
        super.addCommand(alias, class1, command);
    }

    @Override
    public <T1, T2> void addCommand(String alias, Class<T1> class1, Class<T2> class2, Command2<T1, T2> command) {
        super.registerAliasId(alias);
        super.addCommand(alias, class1, class2, command);
    }

    @Override
    public <T1, T2, T3> void addCommand(String alias, Class<T1> class1, Class<T2> class2, Class<T3> class3,
                                        Command3<T1, T2, T3> command) {
        super.registerAliasId(alias);
        super.addCommand(alias, class1, class2, class3, command);
    }

    @Override
    public <T1, T2, T3, T4> void addCommand(String alias,
                                            Class<T1> class1, Class<T2> class2, Class<T3> class3, Class<T4> class4,
                                            Command4<T1, T2, T3, T4> command) {
        super.registerAliasId(alias);
        super.addCommand(alias, class1, class2, class3, class4, command);
    }

    @Override
    public <T1, T2, T3, T4, T5> void addCommand(String alias,
                                                Class<T1> class1, Class<T2> class2, Class<T3> class3,
                                                Class<T4> class4, Class<T5> class5,
                                                Command5<T1, T2, T3, T4, T5> command) {
        super.registerAliasId(alias);
        super.addCommand(alias, class1, class2, class3, class4, class5, command);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6> void addCommand(String alias,
                                                    Class<T1> class1, Class<T2> class2, Class<T3> class3,
                                                    Class<T4> class4, Class<T5> class5, Class<T6> class6,
                                                    Command6<T1, T2, T3, T4, T5, T6> command) {
        super.registerAliasId(alias);
        super.addCommand(alias, class1, class2, class3, class4, class5, class6, command);
    }
}
