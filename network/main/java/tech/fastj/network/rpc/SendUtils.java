package tech.fastj.network.rpc;

import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.serial.util.NetworkableUtils;
import tech.fastj.network.serial.write.NetworkableOutputStream;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

public class SendUtils {

    /** Maximum length of a UDP packet's data -- this does not account for the data length. */
    public static final int UdpPacketDataLength = 504;

    /** Maximum length of a UDP packet, including both the identifier and the data length. */
    public static final int UdpPacketBufferLength = UdpPacketDataLength + Integer.BYTES;

    public static void checkUDPPacketSize(byte[] rawData) {
        assert rawData == null || rawData.length <= SendUtils.UdpPacketBufferLength;
    }

    public static void sendTCP(NetworkableOutputStream tcpOut, Command.Id commandId, byte[] rawData) throws IOException {
        byte[] packetData = buildNetworkData(commandId.uuid(), rawData);

        tcpOut.write(packetData);
        tcpOut.flush();
    }

    public static void sendUDP(DatagramSocket udpSocket, ClientConfig clientConfig, Command.Id commandId, byte[] rawData)
            throws IOException {
        SendUtils.checkUDPPacketSize(rawData);
        byte[] packetData = buildNetworkData(commandId.uuid(), rawData);

        DatagramPacket packet = buildPacket(clientConfig, packetData);
        udpSocket.send(packet);
    }

    public static DatagramPacket buildPacket(ClientConfig clientConfig, byte[] packetData) {
        return new DatagramPacket(packetData, packetData.length, clientConfig.address(), clientConfig.port());
    }

    public static byte[] buildNetworkData(UUID identifier, byte[] rawData) {
        ByteBuffer packetDataBuffer;

        if (rawData == null) {
            packetDataBuffer = ByteBuffer.allocate(NetworkableUtils.UuidBytes);
            return packetDataBuffer.putLong(identifier.getMostSignificantBits())
                    .putLong(identifier.getLeastSignificantBits())
                    .array();
        } else {
            packetDataBuffer = ByteBuffer.allocate(Math.min(UdpPacketBufferLength, NetworkableUtils.UuidBytes + rawData.length));
            return packetDataBuffer.putLong(identifier.getMostSignificantBits())
                    .putLong(identifier.getLeastSignificantBits())
                    .put(rawData)
                    .array();
        }
    }
}
