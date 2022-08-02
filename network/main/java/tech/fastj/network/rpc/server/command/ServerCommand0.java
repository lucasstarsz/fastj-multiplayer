package tech.fastj.network.rpc.server.command;

import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.server.ServerClient;

@FunctionalInterface
public interface ServerCommand0<T extends ServerClient<? extends Enum<? extends CommandAlias>>> extends ServerCommand {

    void runCommand(T client);

    @Override
    default int commandArgumentCount() {
        return 0;
    }
}
