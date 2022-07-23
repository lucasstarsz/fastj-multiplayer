package tech.fastj.network.rpc.commands;

import tech.fastj.network.rpc.ConnectionHandler;

@FunctionalInterface
public interface Command0<T extends ConnectionHandler<?>> extends Command {
    void runCommand(T client);
}
