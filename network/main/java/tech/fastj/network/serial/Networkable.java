package tech.fastj.network.serial;

public interface Networkable {
    @SuppressWarnings("unchecked")
    default <T extends Networkable> NetworkableSerializer<T> getSerializer(Serializer serializer) {
        return (NetworkableSerializer<T>) serializer.getSerializer(this.getClass());
    }
}
