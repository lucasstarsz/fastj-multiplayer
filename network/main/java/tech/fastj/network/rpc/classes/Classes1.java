package tech.fastj.network.rpc.classes;

public record Classes1<T1 extends Class<?>>(T1 t1) implements Classes {
    @Override
    public Class<?>[] classesArray() {
        return new Class[] {t1};
    }
}
