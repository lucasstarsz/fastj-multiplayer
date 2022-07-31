package tech.fastj.network.rpc.commands;

import tech.fastj.network.rpc.ConnectionHandler;

@FunctionalInterface
public interface Command3<T extends ConnectionHandler<?, ?>, T1, T2, T3> extends Command {
    void runCommand(T client, T1 t1, T2 t2, T3 t3);

    @Override
    default int commandArgumentCount() {
        return 3;
    }
}
