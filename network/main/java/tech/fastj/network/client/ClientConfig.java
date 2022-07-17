package tech.fastj.network.client;

import java.net.InetAddress;
import java.net.UnknownHostException;

public record ClientConfig(InetAddress address, int port) {

    public ClientConfig(int port) throws UnknownHostException {
        this(InetAddress.getLocalHost(), port);
    }
}
