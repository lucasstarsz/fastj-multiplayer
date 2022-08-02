package tech.fastj.network.rpc.local.command;

@FunctionalInterface
public interface LocalCommand0 extends LocalCommand {

    void runCommand();

    @Override
    default int commandArgumentCount() {
        return 0;
    }
}
