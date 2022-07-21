package tech.fastj.network.rpc.commands;

import tech.fastj.network.rpc.ClientBase;

@FunctionalInterface
public interface Command0<T extends ClientBase<?>> extends Command {
    void runCommand(T client);
}
