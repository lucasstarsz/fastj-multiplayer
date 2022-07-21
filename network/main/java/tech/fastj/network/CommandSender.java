package tech.fastj.network;

import tech.fastj.network.rpc.CommandHandler;
import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.serial.Message;

import java.io.IOException;

public abstract class CommandSender extends CommandHandler {

    public abstract void sendTCP(Command.Id commandId, byte[] rawData) throws IOException;

    public void sendTCP(Command.Id commandId) throws IOException {
        sendTCP(commandId, (byte[]) null);
    }

    public void sendTCP(Command.Id commandId, Message message) throws IOException {
        byte[] rawData = serializer.writeMessage(message);
        sendTCP(commandId, rawData);
    }

    public void sendTCP(Command.Id commandId, Message... messages) throws IOException {
        byte[] rawData = serializer.writeMessages(messages);
        sendTCP(commandId, rawData);
    }

    public void sendTCP(Command.Id commandId, Object... objects) throws IOException {
        byte[] rawData = serializer.writeObjects(objects);
        sendTCP(commandId, rawData);
    }

    public <T> void sendTCP(Command.Id commandId, T object) throws IOException {
        byte[] rawData = serializer.writeObject(object);
        sendTCP(commandId, rawData);
    }

    public abstract void sendUDP(Command.Id commandId, byte[] rawData) throws IOException;

    public void sendUDP(Command.Id commandId) throws IOException {
        sendUDP(commandId, (byte[]) null);
    }

    public void sendUDP(Command.Id commandId, Message message) throws IOException {
        byte[] rawData = serializer.writeMessage(message);
        sendUDP(commandId, rawData);
    }

    public void sendUDP(Command.Id commandId, Message... messages) throws IOException {
        byte[] rawData = serializer.writeMessages(messages);
        sendUDP(commandId, rawData);
    }

    public void sendUDP(Command.Id commandId, Object... objects) throws IOException {
        byte[] rawData = serializer.writeObjects(objects);
        sendUDP(commandId, rawData);
    }

    public <T> void sendUDP(Command.Id commandId, T object) throws IOException {
        byte[] rawData = serializer.writeObject(object);
        sendUDP(commandId, rawData);
    }
}
