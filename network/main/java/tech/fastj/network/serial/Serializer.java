package tech.fastj.network.serial;

import tech.fastj.network.serial.read.NetworkableInputStream;
import tech.fastj.network.serial.util.NetworkableUtils;
import tech.fastj.network.serial.util.RecordSerializerUtils;
import tech.fastj.network.serial.write.NetworkableOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Serializer {
    private final Map<NetworkableSerializer<?>, UUID> serializersToTypes;
    private final Map<Class<?>, NetworkableSerializer<?>> typeClassesToSerializers;

    private static final Set<Class<?>> DefaultAllowedTypes = Set.of(
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            byte[].class,
            int[].class,
            float[].class,
            String.class,
            UUID.class
    );

    public Serializer() {
        serializersToTypes = new HashMap<>();
        typeClassesToSerializers = new HashMap<>();
    }

    public Serializer(Map<UUID, Class<? extends Networkable>> premappings) {
        this();

        for (var networkableType : premappings.entrySet()) {
            registerSerializer(networkableType.getKey(), networkableType.getValue());
        }
    }

    public <T extends Networkable> void registerSerializer(Class<T> networkableType) {
        registerSerializer(UUID.randomUUID(), RecordSerializerUtils.generate(this, networkableType));
    }

    public <T extends Networkable> void registerSerializer(UUID id, Class<T> networkableType) {
        registerSerializer(id, RecordSerializerUtils.generate(this, networkableType));
    }

    public synchronized <T extends Networkable> void registerSerializer(UUID id, NetworkableSerializer<T> serializer) {
        serializersToTypes.put(serializer, id);
        typeClassesToSerializers.put(serializer.networkableClass(), serializer);
    }

    @SuppressWarnings("unchecked")
    public <T extends Networkable> NetworkableSerializer<T> getSerializer(Class<T> networkableType) {
        return (NetworkableSerializer<T>) typeClassesToSerializers.get(networkableType);
    }

    public Networkable readNetworkable(NetworkableInputStream inputStream, Class<? extends Networkable> networkableClass)
            throws IOException {
        try {
            boolean isNetworkableNull = inputStream.readBoolean();

            if (isNetworkableNull) {
                return null;
            } else {
                var type = typeClassesToSerializers.get(networkableClass);
                return type.reader().read(inputStream);
            }
        } catch (IOException exception) {
            throw new IOException("Unable to read networkable: " + exception.getMessage(), exception);
        }
    }

    public Networkable readNetworkable(InputStream inputStream, Class<? extends Networkable> networkableClass) throws IOException {
        return readNetworkable(new NetworkableInputStream(inputStream, this), networkableClass);
    }

    public Networkable readNetworkable(byte[] data, Class<? extends Networkable> networkableClass) throws IOException {
        return readNetworkable(new ByteArrayInputStream(data), networkableClass);
    }

    public <T extends Networkable> void writeNetworkable(NetworkableOutputStream outputStream, T networkable) throws IOException {
        try {
            outputStream.writeBoolean(networkable == null);

            if (networkable != null) {
                UUID networkableId = serializersToTypes.get(typeClassesToSerializers.get(networkable.getClass()));
                if (networkableId == null) {
                    throw new IOException("Unsupported networkable type '" + networkable.getClass().getSimpleName() + "'");
                }

                networkable.getSerializer(this).writer().write(outputStream, networkable);
            }
        } catch (IOException exception) {
            throw new IOException("Unable to write networkable: " + exception.getMessage(), exception);
        }
    }

    public <T extends Networkable> void writeNetworkable(OutputStream outputStream, T networkable) throws IOException {
        writeNetworkable(new NetworkableOutputStream(outputStream, this), networkable);
    }

    public <T extends Networkable> byte[] writeNetworkable(T networkable) throws IOException {
        ByteArrayOutputStream outputStream;

        if (networkable == null) {
            outputStream = new ByteArrayOutputStream(NetworkableUtils.MinNetworkableBytes);
        } else {
            int byteLength = networkable.getSerializer(this).byteLengthFunction().apply(networkable);
            outputStream = new ByteArrayOutputStream(NetworkableUtils.MinNetworkableBytes + byteLength);
        }

        writeNetworkable(outputStream, networkable);
        return outputStream.toByteArray();
    }

    public byte[] writeNetworkables(Networkable... networkables) throws IOException {
        int length = 0;

        for (Networkable networkable : networkables) {
            length += NetworkableUtils.bytesLength(this, networkable);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(length);
        var writeStream = new NetworkableOutputStream(outputStream, this);

        for (Networkable networkable : networkables) {
            writeNetworkable(writeStream, networkable);
        }

        return outputStream.toByteArray();
    }

    public <T> byte[] writeObject(T value) throws IOException {
        typeCheck(value.getClass());

        int length = NetworkableUtils.bytesLength(this, value);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(length);
        new NetworkableOutputStream(outputStream, this).writeObject(value, value.getClass());

        return outputStream.toByteArray();
    }

    public final byte[] writeObjects(Object... objects) throws IOException {
        int length = 0;

        for (Object object : objects) {
            typeCheck(object.getClass());

            length += NetworkableUtils.bytesLength(this, object);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(length);
        var writeStream = new NetworkableOutputStream(outputStream, this);

        for (Object object : objects) {
            writeStream.writeObject(object, object.getClass());
        }

        return outputStream.toByteArray();
    }

    private void typeCheck(Class<?> type) throws IOException {
        if (DefaultAllowedTypes.contains(type)) {
            return;
        } else if (Enum.class.isAssignableFrom(type)) {
            return;
        } else if (Networkable.class.isAssignableFrom(type)) {
            UUID networkableId = serializersToTypes.get(typeClassesToSerializers.get(type));

            if (networkableId == null) {
                throw new IOException("Unsupported networkable type '" + type.getSimpleName() + "'");
            }

            return;
        }

        throw new IOException("Unsupoprted type " + type);
    }
}
