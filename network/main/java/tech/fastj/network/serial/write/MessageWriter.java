package tech.fastj.network.serial.write;

import java.io.IOException;

import tech.fastj.network.serial.Message;

@FunctionalInterface
public interface MessageWriter<T extends Message> {
    void write(MessageOutputStream outputStream, T networkable) throws IOException;
}
