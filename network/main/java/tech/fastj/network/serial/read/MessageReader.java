package tech.fastj.network.serial.read;

import java.io.IOException;

import tech.fastj.network.serial.Message;

@FunctionalInterface
public interface MessageReader<T extends Message> {
    T read(MessageInputStream inputStream) throws IOException;
}
