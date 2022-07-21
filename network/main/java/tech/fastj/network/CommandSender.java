package tech.fastj.network;

import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.serial.Message;
import tech.fastj.network.serial.Serializer;

import java.io.IOException;

public interface CommandSender {

    Serializer getSerializer();

    void sendTCP(Command.Id commandId, byte[] rawData) throws IOException;

    default void sendTCP(Command.Id commandId) throws IOException {
        sendTCP(commandId, (byte[]) null);
    }

    default void sendTCP(Command.Id commandId, Message message) throws IOException {
        byte[] rawData = getSerializer().writeMessage(message);
        sendTCP(commandId, rawData);
    }

    default void sendTCP(Command.Id commandId, Message... messages) throws IOException {
        byte[] rawData = getSerializer().writeMessages(messages);
        sendTCP(commandId, rawData);
    }

    default void sendTCP(Command.Id commandId, Object... objects) throws IOException {
        byte[] rawData = getSerializer().writeObjects(objects);
        sendTCP(commandId, rawData);
    }

    default <T> void sendTCP(Command.Id commandId, T object) throws IOException {
        byte[] rawData = getSerializer().writeObject(object);
        sendTCP(commandId, rawData);
    }

    void sendUDP(Command.Id commandId, byte[] rawData) throws IOException;

    default void sendUDP(Command.Id commandId) throws IOException {
        sendUDP(commandId, (byte[]) null);
    }

    default void sendUDP(Command.Id commandId, Message message) throws IOException {
        byte[] rawData = getSerializer().writeMessage(message);
        sendUDP(commandId, rawData);
    }

    default void sendUDP(Command.Id commandId, Message... messages) throws IOException {
        byte[] rawData = getSerializer().writeMessages(messages);
        sendUDP(commandId, rawData);
    }

    default void sendUDP(Command.Id commandId, Object... objects) throws IOException {
        byte[] rawData = getSerializer().writeObjects(objects);
        sendUDP(commandId, rawData);
    }

    default <T> void sendUDP(Command.Id commandId, T object) throws IOException {
        byte[] rawData = getSerializer().writeObject(object);
        sendUDP(commandId, rawData);
    }
}
