package tech.fastj.network.rpc.commands;

import tech.fastj.network.client.Client;

@FunctionalInterface
public interface Command0 extends Command {
    void runCommand(Client client);
}
