package tech.fastj.network.serial;

import tech.fastj.network.serial.read.MessageInputStream;
import tech.fastj.network.serial.util.MessageUtils;
import tech.fastj.network.serial.util.RecordSerializerUtils;
import tech.fastj.network.serial.write.MessageOutputStream;

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
    private final Map<MessageSerializer<?>, UUID> serializersToTypes;
    private final Map<Class<?>, MessageSerializer<?>> typeClassesToSerializers;

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

    public Serializer(Map<UUID, Class<? extends Message>> premappings) {
        this();

        for (var networkableType : premappings.entrySet()) {
            registerSerializer(networkableType.getKey(), networkableType.getValue());
        }
    }

    public <T extends Message> void registerSerializer(Class<T> networkableType) {
        registerSerializer(UUID.randomUUID(), RecordSerializerUtils.generate(this, networkableType));
    }

    public <T extends Message> void registerSerializer(UUID id, Class<T> networkableType) {
        registerSerializer(id, RecordSerializerUtils.generate(this, networkableType));
    }

    public synchronized <T extends Message> void registerSerializer(UUID id, MessageSerializer<T> serializer) {
        if (typeClassesToSerializers.containsKey(serializer.networkableClass())) {
            return;
        }

        serializersToTypes.put(serializer, id);
        typeClassesToSerializers.put(serializer.networkableClass(), serializer);
    }

    @SuppressWarnings("unchecked")
    public <T extends Message> MessageSerializer<T> getSerializer(Class<T> networkableType) {
        return (MessageSerializer<T>) typeClassesToSerializers.get(networkableType);
    }

    public Message readMessage(MessageInputStream inputStream, Class<? extends Message> networkableClass)
        throws IOException {
        try {
            boolean isMessageNull = inputStream.readBoolean();

            if (isMessageNull) {
                return null;
            } else {
                var type = typeClassesToSerializers.get(networkableClass);
                return type.reader().read(inputStream);
            }
        } catch (IOException exception) {
            throw new IOException("Unable to read networkable: " + exception.getMessage(), exception);
        }
    }

    public Message readMessage(InputStream inputStream, Class<? extends Message> networkableClass) throws IOException {
        return readMessage(new MessageInputStream(inputStream, this), networkableClass);
    }

    public Message readMessage(byte[] data, Class<? extends Message> networkableClass) throws IOException {
        return readMessage(new ByteArrayInputStream(data), networkableClass);
    }

    public <T extends Message> void writeMessage(MessageOutputStream outputStream, T networkable) throws IOException {
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

    public <T extends Message> void writeMessage(OutputStream outputStream, T networkable) throws IOException {
        writeMessage(new MessageOutputStream(outputStream, this), networkable);
    }

    public <T extends Message> byte[] writeMessage(T networkable) throws IOException {
        ByteArrayOutputStream outputStream;

        if (networkable == null) {
            outputStream = new ByteArrayOutputStream(MessageUtils.MinMessageBytes);
        } else {
            int byteLength = networkable.getSerializer(this).byteLengthFunction().apply(networkable);
            outputStream = new ByteArrayOutputStream(MessageUtils.MinMessageBytes + byteLength);
        }

        writeMessage(outputStream, networkable);
        return outputStream.toByteArray();
    }

    public byte[] writeMessages(Message... messages) throws IOException {
        int length = 0;

        for (Message message : messages) {
            length += MessageUtils.bytesLength(this, message);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(length);
        var writeStream = new MessageOutputStream(outputStream, this);

        for (Message message : messages) {
            writeMessage(writeStream, message);
        }

        return outputStream.toByteArray();
    }

    public <T> byte[] writeObject(T value, Class<T> type) throws IOException {
        typeCheck(type);

        int length = MessageUtils.bytesLength(this, value);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(length);
        new MessageOutputStream(outputStream, this).writeObject(value, type);

        return outputStream.toByteArray();
    }

    @SuppressWarnings("unchecked")
    public <T> byte[] writeObject(T value) throws IOException {
        return writeObject(value, (Class<T>) value.getClass());
    }

    public final byte[] writeObjects(Object... objects) throws IOException {
        int length = 0;

        for (Object object : objects) {
            typeCheck(object.getClass());

            length += MessageUtils.bytesLength(this, object);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(length);
        var writeStream = new MessageOutputStream(outputStream, this);

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
        } else if (Message.class.isAssignableFrom(type)) {
            UUID networkableId = serializersToTypes.get(typeClassesToSerializers.get(type));

            if (networkableId == null) {
                throw new IOException("Unsupported networkable type '" + type.getSimpleName() + "'");
            }

            return;
        }

        throw new IOException("Unsupoprted type " + type);
    }
}
