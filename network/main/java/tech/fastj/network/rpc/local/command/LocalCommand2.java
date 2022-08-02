package tech.fastj.network.rpc.local.command;

@FunctionalInterface
public interface LocalCommand2<T1, T2> extends LocalCommand {

    void runCommand(T1 t1, T2 t2);

    @Override
    default int commandArgumentCount() {
        return 2;
    }
}
