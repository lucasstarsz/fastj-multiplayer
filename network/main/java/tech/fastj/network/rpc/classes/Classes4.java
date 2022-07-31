package tech.fastj.network.rpc.classes;

public record Classes4<T1 extends Class<?>, T2 extends Class<?>, T3 extends Class<?>, T4 extends Class<?>>
    (T1 t1, T2 t2, T3 t3, T4 t4) implements Classes {
    @Override
    public Class<?>[] classesArray() {
        return new Class[] {t1, t2, t3, t4};
    }
}