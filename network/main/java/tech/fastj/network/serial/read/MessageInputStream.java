package tech.fastj.network.serial.read;

import tech.fastj.network.serial.Message;
import tech.fastj.network.serial.MessageSerializer;
import tech.fastj.network.serial.RecordSerializer;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.util.MessageUtils;
import tech.fastj.network.serial.util.RecordSerializerUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.UUID;


public class MessageInputStream extends DataInputStream {

    private final Serializer serializer;

    public MessageInputStream(InputStream inputStream, Serializer serializer) {
        super(inputStream);
        this.serializer = serializer;
    }

    @SuppressWarnings("unchecked")
    public Object readObject(Class<?> objectType) throws IOException {
        if (objectType.equals(Integer.class) || objectType.equals(int.class)) {
            return readInt();
        } else if (objectType.equals(Short.class) || objectType.equals(short.class)) {
            return readShort();
        } else if (objectType.equals(Byte.class) || objectType.equals(byte.class)) {
            return (byte) read();
        } else if (objectType.equals(Long.class) || objectType.equals(long.class)) {
            return readLong();
        } else if (objectType.equals(Float.class) || objectType.equals(float.class)) {
            return readFloat();
        } else if (objectType.equals(Double.class) || objectType.equals(double.class)) {
            return readDouble();
        } else if (objectType.equals(Boolean.class) || objectType.equals(boolean.class)) {
            return readBoolean();
        } else if (objectType.isEnum()) {
            return readEnum((Class<? extends Enum<?>>) objectType);
        } else if (objectType.equals(String.class)) {
            return readString();
        } else if (objectType.equals(UUID.class)) {
            return readUUID();
        } else if (objectType.isAssignableFrom(byte[].class)) {
            return readByteArray();
        } else if (objectType.isAssignableFrom(float[].class)) {
            return readFloatArray();
        } else if (objectType.isAssignableFrom(int[].class)) {
            return readIntArray();
        } else if (objectType.isArray() && Message.class.isAssignableFrom(objectType.getComponentType())) {
            var networkableType = RecordSerializerUtils.get(serializer, (Class<? extends Message>) objectType.getComponentType());
            return readArray(networkableType);
        } else if (Message.class.isAssignableFrom(objectType)) {
            var networkableType = RecordSerializerUtils.get(serializer, (Class<? extends Message>) objectType);
            return readMessage(networkableType);
        } else {
            throw new IOException("Unsupported object type: " + objectType.getSimpleName());
        }
    }

    private <T extends Message> T readMessage(RecordSerializer<T> networkableType) throws IOException {
        boolean isMessageNull = readBoolean();
        if (isMessageNull) {
            return null;
        } else {
            return networkableType.reader().read(this);
        }
    }

    private Enum<?> readEnum(Class<? extends Enum<?>> enumType) throws IOException {
        int enumOrdinal = readInt();

        if (enumOrdinal == MessageUtils.Null) {
            return null;
        } else {
            return enumType.getEnumConstants()[enumOrdinal];
        }
    }

    private String readString() throws IOException {
        int stringLength = readInt();

        if (stringLength == MessageUtils.Null) {
            return null;
        } else {
            byte[] stringBytes = readNBytes(stringLength);
            return new String(stringBytes);
        }
    }

    private UUID readUUID() throws IOException {
        long least = readLong();
        long most = readLong();

        if (least == MessageUtils.Null && most == MessageUtils.Null) {
            return null;
        } else {
            return new UUID(least, most);
        }
    }

    private byte[] readByteArray() throws IOException {
        int arrayLength = readInt();

        if (arrayLength == MessageUtils.Null) {
            return null;
        } else {
            return readNBytes(arrayLength);
        }
    }

    private float[] readFloatArray() throws IOException {
        int arrayLength = readInt();

        if (arrayLength == MessageUtils.Null) {
            return null;
        } else {
            float[] floatArray = new float[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                floatArray[i] = readFloat();
            }

            return floatArray;
        }
    }

    private int[] readIntArray() throws IOException {
        int arrayLength = readInt();

        if (arrayLength == MessageUtils.Null) {
            return null;
        } else {
            int[] intArray = new int[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                intArray[i] = readInt();
            }

            return intArray;
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Message> T[] readArray(MessageSerializer<T> serializer) throws IOException {
        int arrayLength = readInt();

        if (arrayLength == MessageUtils.Null) {
            return null;
        } else {
            T[] array = (T[]) Array.newInstance(serializer.networkableClass(), arrayLength);
            for (int i = 0; i < arrayLength; i++) {
                array[i] = (T) readMessage((RecordSerializer<?>) serializer);
            }

            return array;
        }
    }
}
