package tech.fastj.network;

import tech.fastj.network.rpc.CommandHandler;
import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.serial.Networkable;

import java.io.IOException;

public abstract class CommandSender extends CommandHandler {

    public abstract void sendTCP(Command.Id commandId, byte[] rawData) throws IOException;

    public void sendTCP(Command.Id commandId) throws IOException {
        sendTCP(commandId, (byte[]) null);
    }

    public void sendTCP(Command.Id commandId, Networkable networkable) throws IOException {
        byte[] rawData = serializer.writeNetworkable(networkable);
        sendTCP(commandId, rawData);
    }

    public void sendTCP(Command.Id commandId, Networkable... networkables) throws IOException {
        byte[] rawData = serializer.writeNetworkables(networkables);
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

    public void sendUDP(Command.Id commandId, Networkable networkable) throws IOException {
        byte[] rawData = serializer.writeNetworkable(networkable);
        sendUDP(commandId, rawData);
    }

    public void sendUDP(Command.Id commandId, Networkable... networkables) throws IOException {
        byte[] rawData = serializer.writeNetworkables(networkables);
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
