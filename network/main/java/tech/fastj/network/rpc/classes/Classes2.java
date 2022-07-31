package tech.fastj.network.rpc.classes;

public record Classes2<T1 extends Class<?>, T2 extends Class<?>>(T1 t1, T2 t2) implements Classes {
    @Override
    public Class<?>[] classesArray() {
        return new Class[] {t1, t2};
    }
}