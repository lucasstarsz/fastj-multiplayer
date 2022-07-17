package tech.fastj.network.rpc;

import java.net.InetAddress;
import java.net.UnknownHostException;

public record ServerConfig(InetAddress address, int port, int maxClients, int clientBacklog) {

    public static final int DefaultMaxClients = 4;
    public static final int DefaultClientBacklog = 10;

    public ServerConfig(int port) throws UnknownHostException {
        this(InetAddress.getLocalHost(), port, DefaultMaxClients, DefaultClientBacklog);
    }

    public ServerConfig(int port, int maxClients) throws UnknownHostException {
        this(InetAddress.getLocalHost(), port, maxClients, DefaultClientBacklog);
    }

    public ServerConfig(int port, int maxClients, int backlog) throws UnknownHostException {
        this(InetAddress.getLocalHost(), port, maxClients, backlog);
    }
}
