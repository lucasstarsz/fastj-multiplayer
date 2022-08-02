package tech.fastj.network.rpc.local.command;

@FunctionalInterface
public interface LocalCommand1<T1> extends LocalCommand {

    void runCommand(T1 t1);

    @Override
    default int commandArgumentCount() {
        return 1;
    }
}
