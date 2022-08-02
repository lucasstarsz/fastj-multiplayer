package tech.fastj.network.serial.util;

import tech.fastj.network.serial.Message;
import tech.fastj.network.serial.MessageSerializer;
import tech.fastj.network.serial.Serializer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MessageUtils {

    public static final int Null = -1;
    public static final int UuidBytes = 2 * Long.BYTES;
    public static final int EnumBytes = Integer.BYTES;
    public static final int MinStringBytes = Integer.BYTES;
    public static final int MinMessageBytes = 1;

    public static int bytesLength(String string) {
        return MinStringBytes + (string == null ? 0 : string.getBytes(StandardCharsets.UTF_8).length);
    }

    public static int bytesLength(String... strings) {
        int size = 0;
        for (var s : strings) {
            size += bytesLength(s);
        }

        return size;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Message> int bytesLength(Serializer serializer, T message) {
        if (message == null) {
            return MinMessageBytes;
        } else {
            MessageSerializer<T> typeSerializer = (MessageSerializer<T>) serializer.getSerializer(message.getClass());
            return MinMessageBytes + typeSerializer.byteLengthFunction().apply(message);
        }
    }

    public static <T extends Message> int bytesLength(Serializer serializer, T[] items) {
        int count = Integer.BYTES;
        for (var item : items) {
            count += bytesLength(serializer, item);
        }

        return count;
    }

    public static int bytesLength(Serializer serializer, Object object) {
        if (object instanceof Integer) {
            return Integer.BYTES;
        } else if (object instanceof Float) {
            return Float.BYTES;
        } else if (object instanceof Byte) {
            return Byte.BYTES;
        } else if (object instanceof Double) {
            return Double.BYTES;
        } else if (object instanceof Short) {
            return Short.BYTES;
        } else if (object instanceof Long) {
            return Long.BYTES;
        } else if (object instanceof Boolean) {
            return 1;
        } else if (object instanceof String) {
            return bytesLength((String) object);
        } else if (object instanceof UUID) {
            return UuidBytes;
        } else if (object instanceof Enum<?>) {
            return EnumBytes;
        } else if (object instanceof byte[]) {
            return Integer.BYTES + ((byte[]) object).length;
        } else if (object instanceof int[]) {
            return Integer.BYTES + ((int[]) object).length;
        } else if (object instanceof float[]) {
            return Integer.BYTES + ((float[]) object).length;
        } else if (object.getClass().isArray()) {
            if (Message.class.isAssignableFrom(object.getClass().getComponentType())) {
                return bytesLength(serializer, (Message[]) object);
            } else {
                return bytesLength(serializer, (Object[]) object);
            }
        } else if (object instanceof Message) {
            return bytesLength(serializer, (Message) object);
        } else {
            throw new IllegalArgumentException("Unsupported object type: " + object.getClass().getSimpleName());
        }
    }

    public static int bytesLength(Serializer serializer, Object... objects) {
        int size = 0;
        for (var o : objects) {
            size += bytesLength(serializer, o);
        }

        return size;
    }
}