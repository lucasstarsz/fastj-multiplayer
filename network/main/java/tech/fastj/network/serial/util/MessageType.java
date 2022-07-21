package tech.fastj.network.serial.util;

import tech.fastj.network.serial.Message;
import tech.fastj.network.serial.Serializer;

public record MessageType<T extends Message>(Serializer serializer, Class<T> networkableType) {
}
