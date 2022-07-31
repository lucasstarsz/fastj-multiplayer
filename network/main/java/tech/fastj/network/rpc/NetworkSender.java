package tech.fastj.network.rpc;

import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.rpc.message.RequestType;
import tech.fastj.network.serial.Message;
import tech.fastj.network.serial.Serializer;

import java.io.IOException;

public interface NetworkSender {

    Serializer getSerializer();

    void sendCommand(NetworkType networkType, CommandTarget commandTarget, Enum<? extends CommandAlias> commandId, byte[] rawData)
        throws IOException;

    default void sendCommand(NetworkType networkType, CommandTarget commandTarget, Enum<? extends CommandAlias> commandId)
        throws IOException {
        this.sendCommand(networkType, commandTarget, commandId, (byte[]) null);
    }

    default void sendCommand(NetworkType networkType, CommandTarget commandTarget, Enum<? extends CommandAlias> commandId, Message message)
        throws IOException {
        byte[] rawData = getSerializer().writeMessage(message);
        this.sendCommand(networkType, commandTarget, commandId, rawData);
    }

    default void sendCommand(NetworkType networkType, CommandTarget commandTarget, Enum<? extends CommandAlias> commandId, Message... messages)
        throws IOException {
        byte[] rawData = getSerializer().writeMessages(messages);
        this.sendCommand(networkType, commandTarget, commandId, rawData);
    }

    default void sendCommand(NetworkType networkType, CommandTarget commandTarget, Enum<? extends CommandAlias> commandId, Object... objects)
        throws IOException {
        byte[] rawData = getSerializer().writeObjects(objects);
        this.sendCommand(networkType, commandTarget, commandId, rawData);
    }

    default <T> void sendCommand(NetworkType networkType, CommandTarget commandTarget, Enum<? extends CommandAlias> commandId, T object)
        throws IOException {
        byte[] rawData = getSerializer().writeObject(object);
        this.sendCommand(networkType, commandTarget, commandId, rawData);
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

    void sendKeepAlive(NetworkType networkType) throws IOException;
}
