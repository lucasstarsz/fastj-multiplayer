package tech.fastj.network.serial;

public interface Message {
    @SuppressWarnings("unchecked")
    default <T extends Message> MessageSerializer<T> getSerializer(Serializer serializer) {
        return (MessageSerializer<T>) serializer.getSerializer(this.getClass());
    }
}
