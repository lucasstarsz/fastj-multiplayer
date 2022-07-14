package tech.fastj.network.client;

import java.io.IOException;
import java.net.DatagramPacket;

public interface ClientListener {
    void receiveTCP(byte identifier, Client client) throws IOException;

    void receiveUDP(DatagramPacket packet, Client client) throws IOException;
}
