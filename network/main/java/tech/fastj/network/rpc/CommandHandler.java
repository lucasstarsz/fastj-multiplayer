package tech.fastj.network.rpc;

import tech.fastj.network.rpc.classes.Classes;
import tech.fastj.network.rpc.classes.Classes0;
import tech.fastj.network.rpc.classes.Classes1;
import tech.fastj.network.rpc.classes.Classes2;
import tech.fastj.network.rpc.classes.Classes3;
import tech.fastj.network.rpc.classes.Classes4;
import tech.fastj.network.rpc.classes.Classes5;
import tech.fastj.network.rpc.classes.Classes6;
import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.rpc.commands.Command0;
import tech.fastj.network.rpc.commands.Command1;
import tech.fastj.network.rpc.commands.Command2;
import tech.fastj.network.rpc.commands.Command3;
import tech.fastj.network.rpc.commands.Command4;
import tech.fastj.network.rpc.commands.Command5;
import tech.fastj.network.rpc.commands.Command6;
import tech.fastj.network.serial.Message;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.read.MessageInputStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;

public abstract class CommandHandler<T extends ConnectionHandler<?>> {

    private final Map<UUID, Command> commands;
    private final Map<UUID, Classes> commandClasses;
    private final Map<UUID, Command.Id> idsToCommandIds;
    private final Set<Command.Id> commandIds;

    protected final Serializer serializer;

    protected CommandHandler() {
        commands = new HashMap<>();
        commandClasses = new HashMap<>();
        idsToCommandIds = new HashMap<>();
        commandIds = new HashSet<>();
        serializer = new Serializer();
    }

    public void addCommand(Command.Id id, Command0<T> command) {
        Classes classes = new Classes0();
        registerCommand(id, classes, command);
    }

    public <T1> void addCommand(Command.Id id, Class<T1> class1, Command1<T, T1> command) {
        Classes classes = new Classes1<>(class1);
        registerCommand(id, classes, command);
        tryAddSerializer(class1);
    }

    public <T1, T2> void addCommand(Command.Id id, Class<T1> class1, Class<T2> class2, Command2<T, T1, T2> command) {
        Classes classes = new Classes2<>(class1, class2);
        registerCommand(id, classes, command);
        tryAddSerializer(class1, class2);
    }

    public <T1, T2, T3> void addCommand(Command.Id id, Class<T1> class1, Class<T2> class2, Class<T3> class3,
                                        Command3<T, T1, T2, T3> command) {
        Classes classes = new Classes3<>(class1, class2, class3);
        registerCommand(id, classes, command);
        tryAddSerializer(class1, class2, class3);
    }

    public <T1, T2, T3, T4> void addCommand(Command.Id id, Class<T1> class1, Class<T2> class2, Class<T3> class3,
                                            Class<T4> class4, Command4<T, T1, T2, T3, T4> command) {
        Classes classes = new Classes4<>(class1, class2, class3, class4);
        registerCommand(id, classes, command);
        tryAddSerializer(class1, class2, class3, class4);
    }

    public <T1, T2, T3, T4, T5> void addCommand(Command.Id id, Class<T1> class1, Class<T2> class2, Class<T3> class3,
                                                Class<T4> class4, Class<T5> class5, Command5<T, T1, T2, T3, T4, T5> command) {
        Classes classes = new Classes5<>(class1, class2, class3, class4, class5);
        registerCommand(id, classes, command);
        tryAddSerializer(class1, class2, class3, class4, class5);
    }

    public <T1, T2, T3, T4, T5, T6> void addCommand(Command.Id id, Class<T1> class1, Class<T2> class2, Class<T3> class3,
                                                    Class<T4> class4, Class<T5> class5, Class<T6> class6,
                                                    Command6<T, T1, T2, T3, T4, T5, T6> command) {
        Classes classes = new Classes6<>(class1, class2, class3, class4, class5, class6);
        registerCommand(id, classes, command);
        tryAddSerializer(class1, class2, class3, class4, class5, class6);
    }

    @SuppressWarnings("unchecked")
    private void tryAddSerializer(Class<?>... possibleClasses) {
        for (Class<?> possibleClass : possibleClasses) {
            if (Message.class.isAssignableFrom(possibleClass)) {
                serializer.registerSerializer(UUID.randomUUID(), (Class<? extends Message>) possibleClass);
            }
        }
    }

    public abstract Logger getLogger();

    protected void readCommand(long dataLength, UUID commandId, MessageInputStream inputStream, T client) throws IOException {
        Classes classes = getClasses(commandId);

        if (classes == null) {
            getLogger().warn(
                    "Received unregistered command {} (Command name may have been \"{}\")",
                    commandId,
                    idsToCommandIds.get(commandId).name()
            );

            if (dataLength > inputStream.available()) {
                getLogger().warn(
                        "Received incomplete command {}\nBad data: {}",
                        idsToCommandIds.get(commandId).formattedToString(),
                        Arrays.toString(inputStream.readAllBytes())
                );
            } else {
                getLogger().warn(
                        "Received incomplete command {}\nBad data: {}",
                        idsToCommandIds.get(commandId).formattedToString(),
                        Arrays.toString(inputStream.readNBytes((int) dataLength))
                );
            }
        }

        if (dataLength > inputStream.available()) {
            getLogger().warn(
                    "Received incomplete command {}\nBad data: {}",
                    idsToCommandIds.get(commandId).formattedToString(),
                    Arrays.toString(inputStream.readAllBytes())
            );
        }

        if (classes instanceof Classes0) {
            runCommand(commandId, client);
        } else if (classes instanceof Classes1<?> classes1) {
            runCommand(commandId, client, readObject(classes1.t1(), inputStream));
        } else if (classes instanceof Classes2<?, ?> classes2) {
            runCommand(
                    commandId,
                    client,
                    readObject(classes2.t1(), inputStream),
                    readObject(classes2.t2(), inputStream)
            );
        } else if (classes instanceof Classes3<?, ?, ?> classes3) {
            runCommand(
                    commandId,
                    client,
                    readObject(classes3.t1(), inputStream),
                    readObject(classes3.t2(), inputStream),
                    readObject(classes3.t3(), inputStream)
            );
        } else if (classes instanceof Classes4<?, ?, ?, ?> classes4) {
            runCommand(
                    commandId,
                    client,
                    readObject(classes4.t1(), inputStream),
                    readObject(classes4.t2(), inputStream),
                    readObject(classes4.t3(), inputStream),
                    readObject(classes4.t4(), inputStream)
            );
        } else if (classes instanceof Classes5<?, ?, ?, ?, ?> classes5) {
            runCommand(
                    commandId,
                    client,
                    readObject(classes5.t1(), inputStream),
                    readObject(classes5.t2(), inputStream),
                    readObject(classes5.t3(), inputStream),
                    readObject(classes5.t4(), inputStream),
                    readObject(classes5.t5(), inputStream)
            );
        } else if (classes instanceof Classes6<?, ?, ?, ?, ?, ?> classes6) {
            runCommand(
                    commandId,
                    client,
                    readObject(classes6.t1(), inputStream),
                    readObject(classes6.t2(), inputStream),
                    readObject(classes6.t3(), inputStream),
                    readObject(classes6.t4(), inputStream),
                    readObject(classes6.t5(), inputStream),
                    readObject(classes6.t6(), inputStream)
            );
        }
    }

    @SuppressWarnings("unchecked")
    private Object readObject(Class<?> objectClass, MessageInputStream inputStream) throws IOException {
        Object result;

        if (Message.class.isAssignableFrom(objectClass)) {
            result = serializer.readMessage(inputStream, (Class<? extends Message>) objectClass);
        } else {
            result = inputStream.readObject(objectClass);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    protected void runCommand(UUID id, T client) {
        var command = (Command0<T>) commands.get(id);
        command.runCommand(client);
    }

    @SuppressWarnings("unchecked")
    protected <T1> void runCommand(UUID id, T client, T1 t1) {
        var command = (Command1<T, T1>) commands.get(id);
        command.runCommand(client, t1);
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2> void runCommand(UUID id, T client, T1 t1, T2 t2) {
        var command = (Command2<T, T1, T2>) commands.get(id);
        command.runCommand(client, t1, t2);
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3> void runCommand(UUID id, T client, T1 t1, T2 t2, T3 t3) {
        var command = (Command3<T, T1, T2, T3>) commands.get(id);
        command.runCommand(client, t1, t2, t3);
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3, T4> void runCommand(UUID id, T client, T1 t1, T2 t2, T3 t3, T4 t4) {
        var command = (Command4<T, T1, T2, T3, T4>) commands.get(id);
        command.runCommand(client, t1, t2, t3, t4);
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3, T4, T5> void runCommand(UUID id, T client, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        var command = (Command5<T, T1, T2, T3, T4, T5>) commands.get(id);
        command.runCommand(client, t1, t2, t3, t4, t5);
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3, T4, T5, T6> void runCommand(UUID id, T client, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        var command = (Command6<T, T1, T2, T3, T4, T5, T6>) commands.get(id);
        command.runCommand(client, t1, t2, t3, t4, t5, t6);
    }

    @SuppressWarnings("unchecked")
    protected <C extends Classes> C getClasses(UUID id) {
        return (C) commandClasses.get(id);
    }

    private void registerCommand(Command.Id id, Classes classes, Command command) {
        idRegisterCheck(id);

        commandIds.add(id);
        commands.put(id.uuid(), command);
        commandClasses.put(id.uuid(), classes);
        idsToCommandIds.put(id.uuid(), id);
    }

    private void idRegisterCheck(Command.Id id) {
        if (commandIds.contains(id)) {
            throw new IllegalArgumentException("Command (" + id.formattedToString() + ") has already been registered.");
        }
    }
}
