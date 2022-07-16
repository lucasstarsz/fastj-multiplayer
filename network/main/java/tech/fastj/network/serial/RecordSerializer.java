package tech.fastj.network.serial;

import java.util.function.Function;

import tech.fastj.network.serial.read.NetworkableReader;
import tech.fastj.network.serial.write.NetworkableWriter;

public record RecordSerializer<T extends Networkable>(Class<T> networkableClass,
                                                      Function<T, Integer> byteLengthFunction,
                                                      NetworkableReader<T> reader, NetworkableWriter<T> writer
) implements NetworkableSerializer<T> {
}
