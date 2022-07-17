package tech.fastj.network.rpc.commands;

import tech.fastj.network.client.Client;

@FunctionalInterface
public interface Command2<T1, T2> extends Command {
    void runCommand(Client client, T1 t1, T2 t2);
}
