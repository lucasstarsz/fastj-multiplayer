package tech.fastj.network.rpc.commands;

import tech.fastj.network.rpc.ConnectionHandler;

@FunctionalInterface
public interface Command2<T extends ConnectionHandler<?>, T1, T2> extends Command {
    void runCommand(T client, T1 t1, T2 t2);
}
