package tech.fastj.network.serial;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import tech.fastj.network.serial.read.NetworkableInputStream;
import tech.fastj.network.serial.util.NetworkableUtils;
import tech.fastj.network.serial.util.RecordSerializerUtils;
import tech.fastj.network.serial.write.NetworkableOutputStream;

public class Serializer {
    private final Map<NetworkableSerializer<?>, Byte> serializersToTypes;
    private final Map<Class<?>, NetworkableSerializer<?>> typeClassesToSerializers;

    public Serializer() {
        serializersToTypes = new HashMap<>();
        typeClassesToSerializers = new HashMap<>();
    }

    public Serializer(Map<Byte, Class<? extends Networkable>> premappings) {
        this();

        for (var networkableType : premappings.entrySet()) {
            registerSerializer(networkableType.getKey(), networkableType.getValue());
        }
    }

    public <T extends Networkable> void registerSerializer(byte id, Class<T> networkableType) {
        registerSerializer(id, RecordSerializerUtils.generate(this, networkableType));
    }

    public synchronized <T extends Networkable> void registerSerializer(byte id, NetworkableSerializer<T> serializer) {
        serializersToTypes.put(serializer, id);
        typeClassesToSerializers.put(serializer.networkableClass(), serializer);
    }

    @SuppressWarnings("unchecked")
    public <T extends Networkable> NetworkableSerializer<T> getSerializer(Class<T> networkableType) {
        return (NetworkableSerializer<T>) typeClassesToSerializers.get(networkableType);
    }

    public Networkable readNetworkable(NetworkableInputStream inputStream, Class<? extends Networkable> networkableClass) throws IOException {
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
                Byte networkableId = serializersToTypes.get(typeClassesToSerializers.get(networkable.getClass()));
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
}
