package tech.fastj.network.serial.read;

import java.io.IOException;

import tech.fastj.network.serial.Networkable;

@FunctionalInterface
public interface NetworkableReader<T extends Networkable> {
    T read(NetworkableInputStream inputStream) throws IOException;
}
