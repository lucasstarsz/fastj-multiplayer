package tech.fastj.network.serial.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import tech.fastj.network.serial.Networkable;
import tech.fastj.network.serial.NetworkableSerializer;
import tech.fastj.network.serial.Serializer;

public class NetworkableUtils {

    public static final int UuidBytes = 2 * Long.BYTES;
    public static final int EnumBytes = Integer.BYTES;
    public static final int MinStringBytes = Integer.BYTES;
    public static final int MinNetworkableBytes = 1;

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
    public static <T extends Networkable> int bytesLength(Serializer serializer, T networkable) {
        if (networkable == null) {
            return MinNetworkableBytes;
        } else {
            NetworkableSerializer<T> typeSerializer = (NetworkableSerializer<T>) serializer.getSerializer(networkable.getClass());
            return MinNetworkableBytes + typeSerializer.byteLengthFunction().apply(networkable);
        }
    }

    public static <T extends Networkable> int bytesLength(Serializer serializer, T[] items) {
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
        } else if (object.getClass().isArray() && Networkable.class.isAssignableFrom(object.getClass().getComponentType())) {
            return bytesLength(serializer, (Networkable[]) object);
        } else if (object instanceof Networkable) {
            return bytesLength(serializer, (Networkable) object);
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