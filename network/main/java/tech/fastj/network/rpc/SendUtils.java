package tech.fastj.network.rpc;

import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.RequestType;
import tech.fastj.network.rpc.message.SentMessageType;
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
    public static final int UdpCommandPacketDataLength = UdpPacketBufferLength - (MessageUtils.EnumBytes * 2) - (MessageUtils.UuidBytes * 2) - Long.BYTES;

    /** Maximum length of a UDP special request packet's data. */
    public static final int UdpRequestPacketDataLength = UdpPacketBufferLength - (MessageUtils.EnumBytes * 2) - MessageUtils.UuidBytes - Long.BYTES;

    public static void checkUDPCommandPacketSize(byte[] rawData) {
        assert rawData == null || rawData.length <= SendUtils.UdpCommandPacketDataLength;
    }

    public static void checkUDPRequestPacketSize(byte[] rawData) {
        assert rawData == null || rawData.length <= SendUtils.UdpRequestPacketDataLength;
    }

    public static void sendTCPCommand(MessageOutputStream tcpOut, CommandTarget commandTarget, Enum<? extends CommandAlias> commandId, byte[] rawData)
        throws IOException {
        byte[] packetData = buildTCPCommandData(commandTarget, commandId, rawData);

        tcpOut.write(packetData);
        tcpOut.flush();
    }

    public static void sendUDPCommand(DatagramSocket udpSocket, ClientConfig clientConfig, CommandTarget commandTarget,
                                      Enum<? extends CommandAlias> commandId, UUID senderId, byte[] rawData)
        throws IOException {
        SendUtils.checkUDPCommandPacketSize(rawData);

        byte[] packetData = buildUDPCommandData(commandTarget, senderId, commandId, rawData);

        DatagramPacket packet = buildPacket(clientConfig, packetData);
        udpSocket.send(packet);
    }

    public static DatagramPacket buildPacket(ClientConfig clientConfig, byte[] packetData) {
        return new DatagramPacket(packetData, packetData.length, clientConfig.address(), clientConfig.port());
    }

    public static byte[] buildTCPCommandData(CommandTarget commandTarget, Enum<? extends CommandAlias> commandId, byte[] rawData) {
        ByteBuffer packetDataBuffer;

        if (rawData == null) {
            packetDataBuffer = ByteBuffer.allocate((MessageUtils.EnumBytes * 3));
            return packetDataBuffer.putInt(SentMessageType.RPCCommand.ordinal())
                .putInt(commandTarget.ordinal())
                .putInt(commandId.ordinal())
                .array();
        } else {
            packetDataBuffer = ByteBuffer.allocate((MessageUtils.EnumBytes * 3) + rawData.length);
            return packetDataBuffer.putInt(SentMessageType.RPCCommand.ordinal())
                .putInt(commandTarget.ordinal())
                .putInt(commandId.ordinal())
                .put(rawData)
                .array();
        }
    }

    public static byte[] buildUDPCommandData(CommandTarget commandTarget, UUID senderId, Enum<? extends CommandAlias> commandId, byte[] rawData) {
        ByteBuffer packetDataBuffer;

        if (rawData == null) {
            packetDataBuffer = ByteBuffer.allocate((MessageUtils.EnumBytes * 2) + (MessageUtils.UuidBytes * 2));
            return packetDataBuffer.putLong(senderId.getMostSignificantBits())
                .putLong(senderId.getLeastSignificantBits())
                .putInt(SentMessageType.RPCCommand.ordinal())
                .putInt(commandTarget.ordinal())
                .putInt(commandId.ordinal())
                .array();
        } else {
            packetDataBuffer = ByteBuffer.allocate(
                Math.min(UdpPacketBufferLength, (MessageUtils.EnumBytes * 2) + (MessageUtils.UuidBytes * 2) + rawData.length)
            );

            return packetDataBuffer.putLong(senderId.getMostSignificantBits())
                .putLong(senderId.getLeastSignificantBits())
                .putInt(SentMessageType.RPCCommand.ordinal())
                .putInt(commandTarget.ordinal())
                .putInt(commandId.ordinal())
                .put(rawData)
                .array();
        }
    }

    public static void sendTCPRequest(MessageOutputStream tcpOut, RequestType requestType, byte[] rawData)
        throws IOException {
        byte[] packetData = buildTCPRequestData(requestType, rawData);

        tcpOut.write(packetData);
        tcpOut.flush();
    }

    public static void sendUDPRequest(DatagramSocket udpSocket, ClientConfig clientConfig, RequestType requestType,
                                      UUID senderId, byte[] rawData) throws IOException {
        SendUtils.checkUDPRequestPacketSize(rawData);

        byte[] packetData = buildUDPRequestData(senderId, requestType, rawData);

        DatagramPacket packet = buildPacket(clientConfig, packetData);
        udpSocket.send(packet);
    }

    public static byte[] buildTCPRequestData(RequestType requestType, byte[] rawData) {
        ByteBuffer packetDataBuffer;

        if (rawData == null) {
            packetDataBuffer = ByteBuffer.allocate(MessageUtils.EnumBytes * 2);
            return packetDataBuffer.putInt(SentMessageType.Request.ordinal())
                .putInt(requestType.ordinal())
                .array();
        } else {
            packetDataBuffer = ByteBuffer.allocate((MessageUtils.EnumBytes * 2) + rawData.length);
            return packetDataBuffer.putInt(SentMessageType.Request.ordinal())
                .putInt(requestType.ordinal())
                .put(rawData)
                .array();
        }
    }

    public static byte[] buildUDPRequestData(UUID senderId, RequestType requestType, byte[] rawData) {
        ByteBuffer packetDataBuffer;

        if (rawData == null) {
            packetDataBuffer = ByteBuffer.allocate(MessageUtils.UuidBytes + (MessageUtils.EnumBytes * 2));
            return packetDataBuffer.putLong(senderId.getMostSignificantBits())
                .putLong(senderId.getLeastSignificantBits())
                .putInt(SentMessageType.Request.ordinal())
                .putInt(requestType.ordinal())
                .array();
        } else {
            packetDataBuffer = ByteBuffer.allocate(
                Math.min(UdpPacketBufferLength, MessageUtils.UuidBytes + (MessageUtils.EnumBytes * 2) + rawData.length)
            );

            return packetDataBuffer.putLong(senderId.getMostSignificantBits())
                .putLong(senderId.getLeastSignificantBits())
                .putInt(SentMessageType.Request.ordinal())
                .putInt(requestType.ordinal())
                .put(rawData)
                .array();
        }
    }

    public static void sendTCPDisconnect(MessageOutputStream tcpOut) throws IOException {
        byte[] packetData = buildTCPDisconnect();
        tcpOut.write(packetData);
        tcpOut.flush();
    }

    public static void sendUDPDisconnect(UUID senderId, DatagramSocket udpSocket, ClientConfig clientConfig) throws IOException {
        byte[] packetData = buildUDPDisconnect(senderId);
        DatagramPacket packet = buildPacket(clientConfig, packetData);
        udpSocket.send(packet);
    }

    public static byte[] buildTCPDisconnect() {
        ByteBuffer packetDataBuffer = ByteBuffer.allocate(MessageUtils.EnumBytes);
        return packetDataBuffer.putInt(SentMessageType.Disconnect.ordinal())
            .array();
    }

    public static byte[] buildUDPDisconnect(UUID senderId) {
        ByteBuffer packetDataBuffer = ByteBuffer.allocate(MessageUtils.UuidBytes + (MessageUtils.EnumBytes));
        return packetDataBuffer.putLong(senderId.getMostSignificantBits())
            .putLong(senderId.getLeastSignificantBits())
            .putInt(SentMessageType.Disconnect.ordinal())
            .array();
    }

    public static void sendTCPKeepAlive(MessageOutputStream tcpOut) throws IOException {
        byte[] packetData = buildTCPKeepAlive();
        tcpOut.write(packetData);
        tcpOut.flush();
    }

    public static void sendUDPKeepAlive(UUID senderId, DatagramSocket udpSocket, ClientConfig udpConfig)
        throws IOException {
        byte[] packetData = buildUDPKeepAlive(senderId);
        DatagramPacket packet = buildPacket(udpConfig, packetData);
        udpSocket.send(packet);
    }

    public static byte[] buildTCPKeepAlive() {
        ByteBuffer packetDataBuffer = ByteBuffer.allocate(MessageUtils.EnumBytes);
        return packetDataBuffer.putInt(SentMessageType.KeepAlive.ordinal())
            .array();
    }

    public static byte[] buildUDPKeepAlive(UUID senderId) {
        ByteBuffer packetDataBuffer = ByteBuffer.allocate(MessageUtils.UuidBytes + (MessageUtils.EnumBytes));
        return packetDataBuffer.putLong(senderId.getMostSignificantBits())
            .putLong(senderId.getLeastSignificantBits())
            .putInt(SentMessageType.KeepAlive.ordinal())
            .array();
    }

    public static void sendTCPLobbyUpdate(MessageOutputStream tcpOut, byte[] rawData) throws IOException {
        byte[] packetData = bulidTCPLobbyUpdate(rawData);
        tcpOut.write(packetData);
        tcpOut.flush();
    }

    public static byte[] bulidTCPLobbyUpdate(byte[] rawData) {
        ByteBuffer packetDataBuffer = ByteBuffer.allocate(MessageUtils.EnumBytes + rawData.length);
        return packetDataBuffer.putInt(SentMessageType.LobbyUpdate.ordinal())
            .put(rawData)
            .array();
    }

    public static void sendTCPSessionUpdate(MessageOutputStream tcpOut, byte[] rawData) throws IOException {
        byte[] packetData = bulidTCPSessionUpdate(rawData);
        tcpOut.write(packetData);
        tcpOut.flush();
    }

    public static byte[] bulidTCPSessionUpdate(byte[] rawData) {
        ByteBuffer packetDataBuffer = ByteBuffer.allocate(MessageUtils.EnumBytes + rawData.length);
        return packetDataBuffer.putInt(SentMessageType.SessionUpdate.ordinal())
            .put(rawData)
            .array();
    }
}
