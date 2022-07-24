package tech.fastj.network.rpc;

import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.SpecialRequestType;
import tech.fastj.network.serial.Message;
import tech.fastj.network.serial.Serializer;

import java.io.IOException;

public interface NetworkSender {

    Serializer getSerializer();

    void sendCommand(NetworkType networkType, Command.Id commandId, byte[] rawData) throws IOException;

    default void sendCommand(NetworkType networkType, Command.Id commandId) throws IOException {
        this.sendCommand(networkType, commandId, (byte[]) null);
    }

    default void sendCommand(NetworkType networkType, Command.Id commandId, Message message) throws IOException {
        byte[] rawData = getSerializer().writeMessage(message);
        this.sendCommand(networkType, commandId, rawData);
    }

    default void sendCommand(NetworkType networkType, Command.Id commandId, Message... messages) throws IOException {
        byte[] rawData = getSerializer().writeMessages(messages);
        this.sendCommand(networkType, commandId, rawData);
    }

    default void sendCommand(NetworkType networkType, Command.Id commandId, Object... objects) throws IOException {
        byte[] rawData = getSerializer().writeObjects(objects);
        this.sendCommand(networkType, commandId, rawData);
    }

    default <T> void sendCommand(NetworkType networkType, Command.Id commandId, T object) throws IOException {
        byte[] rawData = getSerializer().writeObject(object);
        this.sendCommand(networkType, commandId, rawData);
    }

    void sendSpecialRequest(NetworkType networkType, SpecialRequestType requestType, byte[] rawData) throws IOException;

    default void sendSpecialRequest(NetworkType networkType, SpecialRequestType requestType) throws IOException {
        this.sendSpecialRequest(networkType, requestType, (byte[]) null);
    }

    default void sendSpecialRequest(NetworkType networkType, SpecialRequestType requestType, Message message) throws IOException {
        byte[] rawData = getSerializer().writeMessage(message);
        this.sendSpecialRequest(networkType, requestType, rawData);
    }

    default void sendSpecialRequest(NetworkType networkType, SpecialRequestType requestType, Message... messages) throws IOException {
        byte[] rawData = getSerializer().writeMessages(messages);
        this.sendSpecialRequest(networkType, requestType, rawData);
    }

    default void sendSpecialRequest(NetworkType networkType, SpecialRequestType requestType, Object... objects) throws IOException {
        byte[] rawData = getSerializer().writeObjects(objects);
        this.sendSpecialRequest(networkType, requestType, rawData);
    }

    default <T> void sendSpecialRequest(NetworkType networkType, SpecialRequestType requestType, T object) throws IOException {
        byte[] rawData = getSerializer().writeObject(object);
        this.sendSpecialRequest(networkType, requestType, rawData);
    }

    void sendDisconnect(NetworkType networkType, byte[] rawData) throws IOException;

    default void sendDisconnect(NetworkType networkType) throws IOException {
        this.sendDisconnect(networkType, null);
    }
}
