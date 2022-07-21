package tech.fastj.network.rpc.commands;

import tech.fastj.network.rpc.ClientBase;

@FunctionalInterface
public interface Command2<T extends ClientBase<?>, T1, T2> extends Command {
    void runCommand(T client, T1 t1, T2 t2);
}
