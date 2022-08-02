package tech.fastj.network.rpc.server.command;

import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.server.ServerClient;

@FunctionalInterface
public interface ServerCommand2<T extends ServerClient<? extends Enum<? extends CommandAlias>>, T1, T2> extends ServerCommand {

    void runCommand(T client, T1 t1, T2 t2);

    @Override
    default int commandArgumentCount() {
        return 2;
    }
}
