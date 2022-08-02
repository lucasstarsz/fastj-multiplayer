package tech.fastj.network.serial.util;

import tech.fastj.network.serial.Message;
import tech.fastj.network.serial.RecordSerializer;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.read.MessageReader;
import tech.fastj.network.serial.write.MessageWriter;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RecordSerializerUtils {

    private static final Map<MessageType<?>, RecordSerializer<?>> generatedMessageTypes = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends Message> RecordSerializer<T> get(Serializer serializer, Class<T> messageType) {
        return (RecordSerializer<T>) generatedMessageTypes.computeIfAbsent(
            new MessageType<>(serializer, messageType),
            type -> generate(serializer, (Class<T>) type.messageType())
        );
    }

    public static <T extends Message> RecordSerializer<T> generate(Serializer serializer, Class<T> messageType) {
        RecordComponent[] components = messageType.getRecordComponents();

        if (components == null) {
            throw new IllegalArgumentException("Cannot generate a MessageTypeSerializer for non-record class " + messageType.getSimpleName());
        }

        Constructor<T> constructor;

        try {
            constructor = messageType.getDeclaredConstructor(
                Arrays.stream(components)
                    .map(RecordComponent::getType)
                    .toArray(Class<?>[]::new)
            );
        } catch (NoSuchMethodException exception) {
            throw new IllegalArgumentException(exception);
        }

        return new RecordSerializer<>(
            messageType,
            generateByteSizeFunction(serializer, components),
            generateReader(constructor),
            generateWriter(components)
        );
    }

    private static <T extends Message> Function<T, Integer> generateByteSizeFunction(Serializer serializer, RecordComponent[] components) {
        return message -> {
            int size = 0;

            for (var component : components) {
                try {
                    size += MessageUtils.bytesLength(serializer, component.getAccessor().invoke(message));
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            }

            return size;
        };
    }

    private static <T extends Message> MessageReader<T> generateReader(Constructor<T> constructor) {
        return inputStream -> {
            Object[] values = new Object[constructor.getParameterCount()];

            for (int i = 0; i < values.length; i++) {
                values[i] = inputStream.readObject(constructor.getParameterTypes()[i]);
            }

            try {
                return constructor.newInstance(values);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        };
    }

    private static <T extends Message> MessageWriter<T> generateWriter(RecordComponent[] components) {
        return (outputStream, message) -> {
            for (var component : components) {
                try {
                    outputStream.writeObject(component.getAccessor().invoke(message), component.getType());
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            }
        };
    }
}
