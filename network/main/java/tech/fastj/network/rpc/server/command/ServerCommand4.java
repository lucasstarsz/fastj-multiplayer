package tech.fastj.network.rpc.server.command;

import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.server.ServerClient;

@FunctionalInterface
public interface ServerCommand4<T extends ServerClient<? extends Enum<? extends CommandAlias>>, T1, T2, T3, T4> extends ServerCommand {

    void runCommand(T client, T1 t1, T2 t2, T3 t3, T4 t4);

    @Override
    default int commandArgumentCount() {
        return 4;
    }
}
