package tech.fastj.network.rpc.commands;

import tech.fastj.network.rpc.ConnectionHandler;

@FunctionalInterface
public interface Command1<T extends ConnectionHandler<?, ?>, T1> extends Command {
    void runCommand(T client, T1 t1);

    @Override
    default int commandArgumentCount() {
        return 1;
    }
}
