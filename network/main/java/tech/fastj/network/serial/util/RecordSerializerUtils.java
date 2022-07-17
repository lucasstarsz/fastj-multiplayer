package tech.fastj.network.serial.util;

import tech.fastj.systems.collections.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import tech.fastj.network.serial.Networkable;
import tech.fastj.network.serial.RecordSerializer;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.read.NetworkableReader;
import tech.fastj.network.serial.write.NetworkableWriter;

public class RecordSerializerUtils {

    private static final Map<Pair<Class<?>, Serializer>, RecordSerializer<?>> generatedMessageTypes = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends Networkable> RecordSerializer<T> get(Serializer serializer, Class<T> networkableClass) {
        return (RecordSerializer<T>) generatedMessageTypes.computeIfAbsent(
                new Pair<>(networkableClass, serializer),
                p -> generate(serializer, (Class<T>) p.getLeft())
        );
    }

    public static <T extends Networkable> RecordSerializer<T> generate(Serializer serializer, Class<T> networkableType) {
        RecordComponent[] components = networkableType.getRecordComponents();
        if (components == null) {
            throw new IllegalArgumentException("Cannot generate a MessageTypeSerializer for non-record class " + networkableType.getSimpleName());
        }
        Constructor<T> constructor;
        try {
            constructor = networkableType.getDeclaredConstructor(
                    Arrays.stream(components)
                            .map(RecordComponent::getType)
                            .toArray(Class<?>[]::new)
            );
        } catch (NoSuchMethodException exception) {
            throw new IllegalArgumentException(exception);
        }
        return new RecordSerializer<>(
                networkableType,
                generateByteSizeFunction(serializer, components),
                generateReader(constructor),
                generateWriter(components)
        );
    }

    private static <T extends Networkable> Function<T, Integer> generateByteSizeFunction(Serializer serializer, RecordComponent[] components) {
        return networkable -> {
            int size = 0;
            for (var component : components) {
                try {
                    size += NetworkableUtils.bytesLength(serializer, component.getAccessor().invoke(networkable));
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            }
            return size;
        };
    }

    private static <T extends Networkable> NetworkableReader<T> generateReader(Constructor<T> constructor) {
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

    private static <T extends Networkable> NetworkableWriter<T> generateWriter(RecordComponent[] components) {
        return (outputStream, networkable) -> {
            for (var component : components) {
                try {
                    outputStream.writeObject(component.getAccessor().invoke(networkable), component.getType());
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            }
        };
    }
}
