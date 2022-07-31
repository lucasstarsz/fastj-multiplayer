package tech.fastj.network.rpc;

import tech.fastj.network.rpc.classes.Classes;

public interface CommandAlias {

    Classes getCommandClasses();

    default Class<?>[] commandClassesArray() {
        return getCommandClasses().classesArray();
    }

    default int commandCount() {
        return commandClassesArray().length;
    }
}
