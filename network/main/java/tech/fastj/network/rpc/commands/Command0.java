package tech.fastj.network.rpc.commands;

import tech.fastj.network.rpc.Client;

@FunctionalInterface
public interface Command0 extends Command {
    void runCommand(Client client);
}
