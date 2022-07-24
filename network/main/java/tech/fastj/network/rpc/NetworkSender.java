package tech.fastj.network.rpc;

import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.RequestType;
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

    void sendRequest(NetworkType networkType, RequestType requestType, byte[] rawData) throws IOException;

    default void sendRequest(NetworkType networkType, RequestType requestType) throws IOException {
        this.sendRequest(networkType, requestType, (byte[]) null);
    }

    default void sendRequest(NetworkType networkType, RequestType requestType, Message message) throws IOException {
        byte[] rawData = getSerializer().writeMessage(message);
        this.sendRequest(networkType, requestType, rawData);
    }

    default void sendRequest(NetworkType networkType, RequestType requestType, Message... messages) throws IOException {
        byte[] rawData = getSerializer().writeMessages(messages);
        this.sendRequest(networkType, requestType, rawData);
    }

    default void sendRequest(NetworkType networkType, RequestType requestType, Object... objects) throws IOException {
        byte[] rawData = getSerializer().writeObjects(objects);
        this.sendRequest(networkType, requestType, rawData);
    }

    default <T> void sendRequest(NetworkType networkType, RequestType requestType, T object) throws IOException {
        byte[] rawData = getSerializer().writeObject(object);
        this.sendRequest(networkType, requestType, rawData);
    }

    void sendDisconnect(NetworkType networkType, byte[] rawData) throws IOException;

    default void sendDisconnect(NetworkType networkType) throws IOException {
        this.sendDisconnect(networkType, null);
    }
}
