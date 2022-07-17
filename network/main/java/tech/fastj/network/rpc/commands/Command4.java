package tech.fastj.network.rpc.commands;

import tech.fastj.network.rpc.Client;

@FunctionalInterface
public interface Command4<T1, T2, T3, T4> extends Command {
    void runCommand(Client client, T1 t1, T2 t2, T3 t3, T4 t4);
}
