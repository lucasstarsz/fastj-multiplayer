package tech.fastj.network.client;

import java.net.InetAddress;
import java.net.UnknownHostException;

public record ClientConfig(InetAddress address, int port, ClientListener clientListener) {

    public ClientConfig(int port, ClientListener clientListener) throws UnknownHostException {
        this(InetAddress.getLocalHost(), port, clientListener);
    }
}
