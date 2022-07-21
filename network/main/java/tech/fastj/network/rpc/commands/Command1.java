package tech.fastj.network.rpc.commands;

import tech.fastj.network.rpc.ClientBase;

@FunctionalInterface
public interface Command1<T extends ClientBase<?>, T1> extends Command {
    void runCommand(T client, T1 t1);
}
