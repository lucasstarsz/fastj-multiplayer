package tech.fastj.network.serial.write;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import tech.fastj.network.serial.Networkable;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.util.NetworkableStreamUtils;

public class NetworkableOutputStream extends DataOutputStream {

    private final Serializer serializer;

    public NetworkableOutputStream(OutputStream outputStream, Serializer serializer) {
        super(outputStream);
        this.serializer = serializer;
    }

    public void writeObject(Object object, Class<?> type) throws IOException {
        if (type.equals(Integer.class) || type.equals(int.class)) {
            writeInt((int) object);
        } else if (type.equals(Short.class) || type.equals(short.class)) {
            writeShort((short) object);
        } else if (type.equals(Byte.class) || type.equals(byte.class)) {
            writeByte((byte) object);
        } else if (type.equals(Long.class) || type.equals(long.class)) {
            writeLong((long) object);
        } else if (type.equals(Float.class) || type.equals(float.class)) {
            writeFloat((float) object);
        } else if (type.equals(Double.class) || type.equals(double.class)) {
            writeDouble((double) object);
        } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            writeBoolean((boolean) object);
        } else if (type.equals(String.class)) {
            writeString((String) object);
        } else if (type.equals(UUID.class)) {
            writeUUID((UUID) object);
        } else if (type.isEnum()) {
            writeEnum((Enum<?>) object);
        } else if (type.equals(byte[].class)) {
            writeArray((byte[]) object);
        } else if (type.equals(int[].class)) {
            writeArray((int[]) object);
        } else if (type.equals(float[].class)) {
            writeArray((float[]) object);
        } else if (type.isArray() && Networkable.class.isAssignableFrom(type.getComponentType())) {
            writeArray((Networkable[]) object);
        } else if (Networkable.class.isAssignableFrom(type)) {
            writeNetworkable((Networkable) object);
        } else {
            throw new IOException("Unsupported object type: " + object.getClass().getSimpleName());
        }
    }

    private <T extends Networkable> void writeNetworkable(T networkable) throws IOException {
        writeBoolean(networkable == null);
        if (networkable != null) {
            networkable.getSerializer(serializer).writer().write(this, networkable);
        }
    }

    private void writeEnum(Enum<?> enumValue) throws IOException {
        if (enumValue == null) {
            writeInt(NetworkableStreamUtils.Null);
        } else {
            writeInt(enumValue.ordinal());
        }
    }

    private void writeString(String string) throws IOException {
        if (string == null) {
            writeInt(NetworkableStreamUtils.Null);
        } else {
            writeInt(string.length());
            byte[] stringBytes = string.getBytes(StandardCharsets.UTF_8);
            write(stringBytes);
        }
    }

    private void writeUUID(UUID uuid) throws IOException {
        if (uuid == null) {
            writeLong(NetworkableStreamUtils.Null);
            writeLong(NetworkableStreamUtils.Null);
        } else {
            writeLong(uuid.getMostSignificantBits());
            writeLong(uuid.getLeastSignificantBits());
        }
    }

    public <T extends Networkable> void writeArray(T[] objectArray) throws IOException {
        if (objectArray == null) {
            writeInt(NetworkableStreamUtils.Null);
        } else {
            writeInt(objectArray.length);
            for (var item : objectArray) {
                writeNetworkable(item);
            }
        }
    }

    public void writeArray(byte[] byteArray) throws IOException {
        if (byteArray == null) {
            writeInt(NetworkableStreamUtils.Null);
        } else {
            writeInt(byteArray.length);
            write(byteArray);
        }
    }

    public void writeArray(int[] intArray) throws IOException {
        if (intArray == null) {
            writeInt(NetworkableStreamUtils.Null);
        } else {
            writeInt(intArray.length);
            for (var item : intArray) {
                writeInt(item);
            }
        }
    }

    public void writeArray(float[] floatArray) throws IOException {
        if (floatArray == null) {
            writeInt(NetworkableStreamUtils.Null);
        } else {
            writeInt(floatArray.length);
            for (var item : floatArray) {
                writeFloat(item);
            }
        }
    }
}
