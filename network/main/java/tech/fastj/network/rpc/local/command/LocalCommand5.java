package tech.fastj.network.rpc.local.command;

@FunctionalInterface
public interface LocalCommand5<T1, T2, T3, T4, T5> extends LocalCommand {

    void runCommand(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);

    @Override
    default int commandArgumentCount() {
        return 5;
    }
}
