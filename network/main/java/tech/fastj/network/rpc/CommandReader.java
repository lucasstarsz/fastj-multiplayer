package tech.fastj.network.rpc;

import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.read.MessageInputStream;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;

public interface CommandReader<E extends Enum<E> & CommandAlias> {

    Logger getLogger();

    Class<E> getAliasClass();

    Map<E, ? extends Command> getCommands();

    Serializer getSerializer();

    default void resetCommands() {
        for (E enumConstant : getAliasClass().getEnumConstants()) {
            clearCommand(enumConstant);
        }
    }

    void clearCommand(E id);

    default Object readObject(Class<?> objectClass, MessageInputStream inputStream) throws IOException {
        return inputStream.readObject(objectClass);
    }
}
