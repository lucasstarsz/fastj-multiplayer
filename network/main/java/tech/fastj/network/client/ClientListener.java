package tech.fastj.network.client;

import java.io.IOException;

public interface ClientListener {
    void receiveTCP(byte[] data, Client client) throws IOException;

    void receiveUDP(byte[] data, Client client) throws IOException;
}
