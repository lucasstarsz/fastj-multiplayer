package tech.fastj.network.rpc;

import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.rpc.message.SentMessageType;
import tech.fastj.network.rpc.message.SpecialRequestType;
import tech.fastj.network.serial.util.MessageUtils;
import tech.fastj.network.serial.write.MessageOutputStream;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

public class SendUtils {

    /** Maximum length of a UDP packet. */
    public static final int UdpPacketBufferLength = 512;

    /** Maximum length of a UDP command packet's data. */
    public static final int UdpCommandPacketDataLength = UdpPacketBufferLength - MessageUtils.EnumBytes - MessageUtils.UuidBytes;

    /** Maximum length of a UDP special request packet's data. */
    public static final int UdpSpecialRequestPacketDataLength = UdpPacketBufferLength - (MessageUtils.EnumBytes * 2);

    public static void checkUDPCommandPacketSize(byte[] rawData) {
        assert rawData == null || rawData.length <= SendUtils.UdpCommandPacketDataLength;
    }

    public static void checkUDPSpecialRequestPacketSize(byte[] rawData) {
        assert rawData == null || rawData.length <= SendUtils.UdpSpecialRequestPacketDataLength;
    }

    public static void sendTCPCommand(MessageOutputStream tcpOut, Command.Id commandId, byte[] rawData) throws IOException {
        byte[] packetData = buildCommandData(commandId.uuid(), rawData);

        tcpOut.write(packetData);
        tcpOut.flush();
    }

    public static void sendUDPCommand(DatagramSocket udpSocket, ClientConfig clientConfig, Command.Id commandId, byte[] rawData)
            throws IOException {
        SendUtils.checkUDPCommandPacketSize(rawData);

        byte[] packetData = buildCommandData(commandId.uuid(), rawData);

        DatagramPacket packet = buildPacket(clientConfig, packetData);
        udpSocket.send(packet);
    }

    public static DatagramPacket buildPacket(ClientConfig clientConfig, byte[] packetData) {
        return new DatagramPacket(packetData, packetData.length, clientConfig.address(), clientConfig.port());
    }

    public static byte[] buildCommandData(UUID identifier, byte[] rawData) {
        ByteBuffer packetDataBuffer;

        if (rawData == null) {
            packetDataBuffer = ByteBuffer.allocate(MessageUtils.EnumBytes + MessageUtils.UuidBytes);
            return packetDataBuffer.putInt(SentMessageType.RPCCommand.ordinal())
                    .putLong(identifier.getMostSignificantBits())
                    .putLong(identifier.getLeastSignificantBits())
                    .array();
        } else {
            packetDataBuffer = ByteBuffer.allocate(
                    Math.min(UdpPacketBufferLength, MessageUtils.EnumBytes + MessageUtils.UuidBytes + rawData.length)
            );

            return packetDataBuffer.putInt(SentMessageType.RPCCommand.ordinal())
                    .putLong(identifier.getMostSignificantBits())
                    .putLong(identifier.getLeastSignificantBits())
                    .put(rawData)
                    .array();
        }
    }

    public static void sendTCPSpecialRequest(MessageOutputStream tcpOut, SpecialRequestType requestType, byte[] rawData)
            throws IOException {
        byte[] packetData = buildSpecialRequestData(requestType, rawData);

        tcpOut.write(packetData);
        tcpOut.flush();
    }

    public static void sendUDPSpecialRequest(DatagramSocket udpSocket, ClientConfig clientConfig, SpecialRequestType requestType,
                                             byte[] rawData) throws IOException {
        SendUtils.checkUDPSpecialRequestPacketSize(rawData);

        byte[] packetData = buildSpecialRequestData(requestType, rawData);

        DatagramPacket packet = buildPacket(clientConfig, packetData);
        udpSocket.send(packet);
    }

    public static byte[] buildSpecialRequestData(SpecialRequestType requestType, byte[] rawData) {
        ByteBuffer packetDataBuffer;

        if (rawData == null) {
            packetDataBuffer = ByteBuffer.allocate(MessageUtils.EnumBytes * 2);
            return packetDataBuffer.putInt(SentMessageType.SpecialRequest.ordinal())
                    .putInt(requestType.ordinal())
                    .array();
        } else {
            packetDataBuffer = ByteBuffer.allocate(Math.min(UdpPacketBufferLength, (MessageUtils.EnumBytes * 2) + rawData.length));
            return packetDataBuffer.putInt(SentMessageType.SpecialRequest.ordinal())
                    .putInt(requestType.ordinal())
                    .put(rawData)
                    .array();
        }
    }

    public static void sendTCPDisconnect(MessageOutputStream tcpOut) throws IOException {
        tcpOut.writeObject(SentMessageType.Disconnect, SentMessageType.class);
        tcpOut.flush();
    }

    public static void sendUDPDisconnect(DatagramSocket udpSocket, ClientConfig clientConfig) throws IOException {
        byte[] packetData = ByteBuffer.allocate(MessageUtils.EnumBytes)
                .putInt(SentMessageType.Disconnect.ordinal())
                .array();

        DatagramPacket packet = buildPacket(clientConfig, packetData);
        udpSocket.send(packet);
    }
}
