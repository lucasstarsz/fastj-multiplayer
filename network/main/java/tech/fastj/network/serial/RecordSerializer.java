package tech.fastj.network.serial;

import java.util.function.Function;

import tech.fastj.network.serial.read.MessageReader;
import tech.fastj.network.serial.write.MessageWriter;

public record RecordSerializer<T extends Message>(Class<T> networkableClass,
                                                  Function<T, Integer> byteLengthFunction,
                                                  MessageReader<T> reader, MessageWriter<T> writer
) implements MessageSerializer<T> {
}
