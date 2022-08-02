package tech.fastj.network.rpc.local.command;

@FunctionalInterface
public interface LocalCommand3<T1, T2, T3> extends LocalCommand {

    void runCommand(T1 t1, T2 t2, T3 t3);

    @Override
    default int commandArgumentCount() {
        return 3;
    }
}
