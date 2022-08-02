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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Serializer {
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
    private final Map<MessageSerializer<?>, UUID> serializersToTypes;
    private final Map<Class<?>, MessageSerializer<?>> typeClassesToSerializers;

    public Serializer() {
        serializersToTypes = new HashMap<>();
        typeClassesToSerializers = new HashMap<>();
    }

    @SafeVarargs
    public Serializer(Class<? extends Message>... messageTypes) {
        this();

        for (Class<? extends Message> messageType : messageTypes) {
            registerSerializer(messageType);
        }
    }

    public Serializer(Map<UUID, Class<? extends Message>> messageSerializers) {
        this();

        for (var messageType : messageSerializers.entrySet()) {
            registerSerializer(messageType.getKey(), messageType.getValue());
        }
    }

    public <T extends Message> void registerSerializer(Class<T> messageType) {
        registerSerializer(UUID.randomUUID(), RecordSerializerUtils.generate(this, messageType));
    }

    public <T extends Message> void registerSerializer(UUID id, Class<T> messageType) {
        registerSerializer(id, RecordSerializerUtils.generate(this, messageType));
    }

    public synchronized <T extends Message> void registerSerializer(UUID id, MessageSerializer<T> serializer) {
        if (typeClassesToSerializers.containsKey(serializer.messageClass())) {
            return;
        }

        serializersToTypes.put(serializer, id);
        typeClassesToSerializers.put(serializer.messageClass(), serializer);
    }

    @SuppressWarnings("unchecked")
    public <T extends Message> MessageSerializer<T> getSerializer(Class<T> messageType) {
        return (MessageSerializer<T>) typeClassesToSerializers.get(messageType);
    }

    public Message readMessage(MessageInputStream inputStream, Class<? extends Message> messageClass)
        throws IOException {
        try {
            boolean isMessageNull = inputStream.readBoolean();

            if (isMessageNull) {
                return null;
            } else {
                var type = typeClassesToSerializers.get(messageClass);
                return type.reader().read(inputStream);
            }
        } catch (IOException exception) {
            throw new IOException("Unable to read message: " + exception.getMessage(), exception);
        }
    }

    public Message readMessage(InputStream inputStream, Class<? extends Message> messageClass) throws IOException {
        return readMessage(new MessageInputStream(inputStream, this), messageClass);
    }

    public Message readMessage(byte[] data, Class<? extends Message> messageClass) throws IOException {
        return readMessage(new ByteArrayInputStream(data), messageClass);
    }

    public <T extends Message> void writeMessage(MessageOutputStream outputStream, T message) throws IOException {
        try {
            outputStream.writeBoolean(message == null);

            if (message != null) {
                UUID messageId = serializersToTypes.get(typeClassesToSerializers.get(message.getClass()));
                if (messageId == null) {
                    throw new IOException("Unsupported message type '" + message.getClass().getSimpleName() + "'");
                }

                message.getSerializer(this).writer().write(outputStream, message);
            }
        } catch (IOException exception) {
            throw new IOException("Unable to write message: " + exception.getMessage(), exception);
        }
    }

    public <T extends Message> void writeMessage(OutputStream outputStream, T message) throws IOException {
        writeMessage(new MessageOutputStream(outputStream, this), message);
    }

    public <T extends Message> byte[] writeMessage(T message) throws IOException {
        ByteArrayOutputStream outputStream;

        if (message == null) {
            outputStream = new ByteArrayOutputStream(MessageUtils.MinMessageBytes);
        } else {
            int byteLength = message.getSerializer(this).byteLengthFunction().apply(message);
            outputStream = new ByteArrayOutputStream(MessageUtils.MinMessageBytes + byteLength);
        }

        writeMessage(outputStream, message);
        return outputStream.toByteArray();
    }

    public byte[] writeMessages(Message... messages) throws IOException {
        int length = 0;

        for (Message message : messages) {
            length += MessageUtils.bytesLength(this, message);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(length + Integer.BYTES);
        var writeStream = new MessageOutputStream(outputStream, this);

        System.out.println(Arrays.toString(messages));

        System.out.println("array " + messages.getClass());
        if (messages.getClass() != null) {
            System.out.println(messages.getClass().componentType());
        } else {
            throw new IOException("no");
        }

        if (!messages.getClass().componentType().equals(Message.class)) {
            System.out.println("write array");
            writeStream.writeArray(messages);
        } else {
            System.out.println("write separate messages");
            for (Message message : messages) {
                writeMessage(writeStream, message);
            }
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
            UUID messageId = serializersToTypes.get(typeClassesToSerializers.get(type));

            if (messageId == null) {
                throw new IOException("Unsupported message type '" + type.getSimpleName() + "'");
            }

            return;
        }

        throw new IOException("Unsupoprted type " + type);
    }
}
