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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;

public abstract class CommandHandler<H extends Enum<H> & CommandAlias, T extends ConnectionHandler<H, T>> {

    protected final Serializer serializer;

    protected final Class<H> aliasClass;
    protected final Map<H, Command> commands;
    protected final H[] aliases;

    protected CommandHandler(Class<H> aliasClass) {
        this.aliasClass = aliasClass;
        this.aliases = aliasClass.getEnumConstants();

        commands = new HashMap<>();
        serializer = new Serializer();
    }

    public void addCommand(H id, Command0<T> command) {
        registerCommand(id, command);
    }

    public <T1> void addCommand(H id, Command1<T, T1> command) {
        registerCommand(id, command);
        tryAddSerializer(id.commandClassesArray());
    }

    public <T1, T2> void addCommand(H id, Command2<T, T1, T2> command) {
        registerCommand(id, command);
        tryAddSerializer(id.commandClassesArray());
    }

    public <T1, T2, T3> void addCommand(H id, Command3<T, T1, T2, T3> command) {
        registerCommand(id, command);
        tryAddSerializer(id.commandClassesArray());
    }

    public <T1, T2, T3, T4> void addCommand(H id, Command4<T, T1, T2, T3, T4> command) {
        registerCommand(id, command);
        tryAddSerializer(id.commandClassesArray());
    }

    public <T1, T2, T3, T4, T5> void addCommand(H id, Command5<T, T1, T2, T3, T4, T5> command) {
        registerCommand(id, command);
        tryAddSerializer(id.commandClassesArray());
    }

    public <T1, T2, T3, T4, T5, T6> void addCommand(H id, Command6<T, T1, T2, T3, T4, T5, T6> command) {
        registerCommand(id, command);
        tryAddSerializer(id.commandClassesArray());
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

    protected void readCommand(H commandId, MessageInputStream inputStream, T client) throws IOException {
        Classes classes = commandId.getCommandClasses();

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

    protected Object readObject(Class<?> objectClass, MessageInputStream inputStream) throws IOException {
        return inputStream.readObject(objectClass);
    }

    @SuppressWarnings("unchecked")
    protected void runCommand(H id, T client) {
        var command = (Command0<T>) commands.get(id);
        command.runCommand(client);
    }

    @SuppressWarnings("unchecked")
    protected <T1> void runCommand(H id, T client, T1 t1) {
        var command = (Command1<T, T1>) commands.get(id);
        command.runCommand(client, t1);
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2> void runCommand(H id, T client, T1 t1, T2 t2) {
        var command = (Command2<T, T1, T2>) commands.get(id);
        command.runCommand(client, t1, t2);
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3> void runCommand(H id, T client, T1 t1, T2 t2, T3 t3) {
        var command = (Command3<T, T1, T2, T3>) commands.get(id);
        command.runCommand(client, t1, t2, t3);
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3, T4> void runCommand(H id, T client, T1 t1, T2 t2, T3 t3, T4 t4) {
        var command = (Command4<T, T1, T2, T3, T4>) commands.get(id);
        command.runCommand(client, t1, t2, t3, t4);
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3, T4, T5> void runCommand(H id, T client, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        var command = (Command5<T, T1, T2, T3, T4, T5>) commands.get(id);
        command.runCommand(client, t1, t2, t3, t4, t5);
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3, T4, T5, T6> void runCommand(H id, T client, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        var command = (Command6<T, T1, T2, T3, T4, T5, T6>) commands.get(id);
        command.runCommand(client, t1, t2, t3, t4, t5, t6);
    }

    private void registerCommand(H id, Command command) {
        idRegisterCheck(id);
        commandNumberCheck(id, command);
        commands.put(id, command);
    }

    private void commandNumberCheck(H id, Command command) {
        if (id.commandCount() != command.commandArgumentCount()) {
            throw new IllegalArgumentException(
                "The number of command parameters must match to " + id.name()
                    + "'s parameter count of " + id.commandCount()
            );
        }
    }

    protected void idRegisterCheck(H id) {
        if (commands.containsKey(id)) {
            getLogger().warn("Replacing command {}.", id.name());
        }
    }
}
