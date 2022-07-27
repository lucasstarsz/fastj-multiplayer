package tech.fastj.network.serial;

import tech.fastj.network.serial.read.MessageReader;
import tech.fastj.network.serial.write.MessageWriter;

import java.util.function.Function;

public interface MessageSerializer<T extends Message> {

    Class<T> networkableClass();

    Function<T, Integer> byteLengthFunction();

    MessageReader<T> reader();

    MessageWriter<T> writer();
}
