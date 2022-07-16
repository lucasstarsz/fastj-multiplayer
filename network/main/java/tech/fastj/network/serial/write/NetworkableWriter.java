package tech.fastj.network.serial.write;

import java.io.IOException;

import tech.fastj.network.serial.Networkable;

@FunctionalInterface
public interface NetworkableWriter<T extends Networkable> {
    void write(NetworkableOutputStream outputStream, T networkable) throws IOException;
}
