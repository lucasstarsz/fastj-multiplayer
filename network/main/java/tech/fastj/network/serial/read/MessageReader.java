package tech.fastj.network.serial.read;

import tech.fastj.network.serial.Message;

import java.io.IOException;

@FunctionalInterface
public interface MessageReader<T extends Message> {
    T read(MessageInputStream inputStream) throws IOException;
}
