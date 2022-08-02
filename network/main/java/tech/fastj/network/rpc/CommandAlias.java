package tech.fastj.network.rpc;

import tech.fastj.network.rpc.classes.Classes;
import tech.fastj.network.serial.Message;
import tech.fastj.network.serial.Serializer;

public interface CommandAlias {

    Classes getCommandClasses();

    default Class<?>[] commandClassesArray() {
        return getCommandClasses().classesArray();
    }

    default int commandCount() {
        return commandClassesArray().length;
    }

    @SuppressWarnings("unchecked")
    default void registerMessages(Serializer serializer) {
        for (Class<?> possiblySerializable : commandClassesArray()) {
            if (Message.class.isAssignableFrom(possiblySerializable)) {
                serializer.registerSerializer((Class<? extends Message>) possiblySerializable);
            }
        }
    }
}
