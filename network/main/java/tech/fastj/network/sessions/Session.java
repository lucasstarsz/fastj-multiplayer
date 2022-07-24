package tech.fastj.network.sessions;

import tech.fastj.network.rpc.Client;
import tech.fastj.network.rpc.CommandHandler;
import tech.fastj.network.rpc.NetworkSender;
import tech.fastj.network.rpc.SendUtils;
import tech.fastj.network.rpc.ServerClient;
import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.SpecialRequestType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Session extends CommandHandler<ServerClient> implements NetworkSender {

    private final Lobby lobby;
    private final Logger sessionLogger = LoggerFactory.getLogger(Client.class);
    private final List<ServerClient> clients;
    private final UUID sessionId;
    private BiConsumer<Session, ServerClient> onReceiveNewClient;
    private BiConsumer<Session, ServerClient> onClientDisconnect;

    protected Session(Lobby lobby, List<ServerClient> clients) {
        this.lobby = lobby;
        this.clients = clients;
        sessionId = UUID.randomUUID();

        onReceiveNewClient = (session, client) -> {};
        onClientDisconnect = (session, client) -> {};
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public List<ServerClient> getClients() {
        return Collections.unmodifiableList(clients);
    }

    public void setOnReceiveNewClient(BiConsumer<Session, ServerClient> onReceiveNewClient) {
        this.onReceiveNewClient = onReceiveNewClient;
    }

    public void setOnClientDisconnect(BiConsumer<Session, ServerClient> onClientDisconnect) {
        this.onClientDisconnect = onClientDisconnect;
    }

    @Override
    public synchronized void sendCommand(NetworkType networkType, Command.Id commandId, byte[] rawData) throws IOException {
        sessionLogger.trace("Session {} sending {} \"{}\" to {} client(s)", sessionId, networkType.name(), commandId.name(), clients.size());

        switch (networkType) {
            case TCP -> {
                byte[] data = SendUtils.buildTCPCommandData(commandId.uuid(), rawData);
                sendTCP(data);
            }
            case UDP -> {
                DatagramSocket udpServer = lobby.getServer().getUdpServer();
                byte[] data = SendUtils.buildUDPCommandData(sessionId, commandId.uuid(), rawData);
                sendUDP(udpServer, data);
            }
        }
    }


    @Override
    public void sendSpecialRequest(NetworkType networkType, SpecialRequestType specialRequestType, byte[] rawData) throws IOException {
        sessionLogger.trace("Session {} sending {} \"{}\" to {} client(s)", sessionId, networkType.name(), specialRequestType.name(), clients.size());

        switch (networkType) {
            case TCP -> {
                byte[] data = SendUtils.buildTCPSpecialRequestData(specialRequestType, rawData);
                sendTCP(data);
            }
            case UDP -> {
                DatagramSocket udpServer = lobby.getServer().getUdpServer();
                byte[] data = SendUtils.buildUDPSpecialRequestData(sessionId, specialRequestType, rawData);
                sendUDP(udpServer, data);
            }
        }
    }

    private void sendTCP(byte[] data) throws IOException {
        for (ServerClient client : clients) {
            client.getTcpOut().write(data);
            client.getTcpOut().flush();
        }
    }

    private void sendUDP(DatagramSocket udpServer, byte[] data) throws IOException {
        for (ServerClient client : clients) {
            DatagramPacket packet = SendUtils.buildPacket(client.getClientConfig(), data);
            udpServer.send(packet);
        }
    }

    public void receiveNewClient(ServerClient client) {
        clients.add(client);
        onReceiveNewClient.accept(this, client);
    }

    public void clientDisconnect(ServerClient client) {
        clients.remove(client);
        onClientDisconnect.accept(this, client);
    }
}
