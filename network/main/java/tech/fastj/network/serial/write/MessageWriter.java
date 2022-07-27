package tech.fastj.network.serial.write;

import tech.fastj.network.serial.Message;

import java.io.IOException;

@FunctionalInterface
public interface MessageWriter<T extends Message> {
    void write(MessageOutputStream outputStream, T networkable) throws IOException;
}
