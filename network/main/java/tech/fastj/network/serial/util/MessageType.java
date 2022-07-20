package tech.fastj.network.serial.util;

import tech.fastj.network.serial.Networkable;
import tech.fastj.network.serial.Serializer;

public record MessageType<T extends Networkable>(Serializer serializer, Class<T> networkableType) {
}
