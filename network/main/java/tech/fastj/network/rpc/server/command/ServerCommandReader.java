package tech.fastj.network.rpc.server.command;

import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.CommandReader;
import tech.fastj.network.rpc.classes.Classes;
import tech.fastj.network.rpc.classes.Classes0;
import tech.fastj.network.rpc.classes.Classes1;
import tech.fastj.network.rpc.classes.Classes2;
import tech.fastj.network.rpc.classes.Classes3;
import tech.fastj.network.rpc.classes.Classes4;
import tech.fastj.network.rpc.classes.Classes5;
import tech.fastj.network.rpc.classes.Classes6;
import tech.fastj.network.rpc.server.ServerClient;
import tech.fastj.network.serial.Message;
import tech.fastj.network.serial.read.MessageInputStream;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public interface ServerCommandReader<E extends Enum<E> & CommandAlias> extends CommandReader<E> {

    Map<Integer, ServerCommand> ServerCommandClears = Map.of(
        0, (ServerCommand0<?>) client -> {},
        1, (ServerCommand1<?, ?>) (client, t1) -> {},
        2, (ServerCommand2<?, ?, ?>) (client, t1, t2) -> {},
        3, (ServerCommand3<?, ?, ?, ?>) (client, t1, t2, t3) -> {},
        4, (ServerCommand4<?, ?, ?, ?, ?>) (client, t1, t2, t3, t4) -> {},
        5, (ServerCommand5<?, ?, ?, ?, ?, ?>) (client, t1, t2, t3, t4, t5) -> {},
        6, (ServerCommand6<?, ?, ?, ?, ?, ?, ?>) (client, t1, t2, t3, t4, t5, t6) -> {}
    );

    default void addCommand(E id, ServerCommand0<ServerClient<E>> command) {
        registerCommand(id, command);
    }

    default <T1> void addCommand(E id, ServerCommand1<ServerClient<E>, T1> command) {
        registerCommand(id, command);
        tryAddSerializer(id.commandClassesArray());
    }

    default <T1, T2> void addCommand(E id, ServerCommand2<ServerClient<E>, T1, T2> command) {
        registerCommand(id, command);
        tryAddSerializer(id.commandClassesArray());
    }

    default <T1, T2, T3> void addCommand(E id, ServerCommand3<ServerClient<E>, T1, T2, T3> command) {
        registerCommand(id, command);
        tryAddSerializer(id.commandClassesArray());
    }

    default <T1, T2, T3, T4> void addCommand(E id, ServerCommand4<ServerClient<E>, T1, T2, T3, T4> command) {
        registerCommand(id, command);
        tryAddSerializer(id.commandClassesArray());
    }

    default <T1, T2, T3, T4, T5> void addCommand(E id, ServerCommand5<ServerClient<E>, T1, T2, T3, T4, T5> command) {
        registerCommand(id, command);
        tryAddSerializer(id.commandClassesArray());
    }

    default <T1, T2, T3, T4, T5, T6> void addCommand(E id, ServerCommand6<ServerClient<E>, T1, T2, T3, T4, T5, T6> command) {
        registerCommand(id, command);
        tryAddSerializer(id.commandClassesArray());
    }

    @Override
    Map<E, ServerCommand> getCommands();

    @Override
    default void clearCommand(E id) {
        ServerCommand clearCommand = ServerCommandClears.get(id.commandCount());

        registerCommand(id, clearCommand);
        tryAddSerializer(id.commandClassesArray());
    }

    @SuppressWarnings("unchecked")
    private void tryAddSerializer(Class<?>... possibleClasses) {
        for (Class<?> possibleClass : possibleClasses) {
            if (Message.class.isAssignableFrom(possibleClass)) {
                getSerializer().registerSerializer(UUID.randomUUID(), (Class<? extends Message>) possibleClass);
            }
        }
    }

    default void readCommand(E commandId, MessageInputStream inputStream, ServerClient<E> client) throws IOException {
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
            System.out.println(inputStream.available());
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
    default void runCommand(E id, ServerClient<E> client) {
        var command = (ServerCommand0<ServerClient<E>>) getCommands().get(id);
        command.runCommand(client);
    }

    @SuppressWarnings("unchecked")
    default <T1> void runCommand(E id, ServerClient<E> client, T1 t1) {
        var command = (ServerCommand1<ServerClient<E>, T1>) getCommands().get(id);
        command.runCommand(client, t1);
    }

    @SuppressWarnings("unchecked")
    default <T1, T2> void runCommand(E id, ServerClient<E> client, T1 t1, T2 t2) {
        var command = (ServerCommand2<ServerClient<E>, T1, T2>) getCommands().get(id);
        command.runCommand(client, t1, t2);
    }

    @SuppressWarnings("unchecked")
    default <T1, T2, T3> void runCommand(E id, ServerClient<E> client, T1 t1, T2 t2, T3 t3) {
        var command = (ServerCommand3<ServerClient<E>, T1, T2, T3>) getCommands().get(id);
        command.runCommand(client, t1, t2, t3);
    }

    @SuppressWarnings("unchecked")
    default <T1, T2, T3, T4> void runCommand(E id, ServerClient<E> client, T1 t1, T2 t2, T3 t3, T4 t4) {
        var command = (ServerCommand4<ServerClient<E>, T1, T2, T3, T4>) getCommands().get(id);
        command.runCommand(client, t1, t2, t3, t4);
    }

    @SuppressWarnings("unchecked")
    default <T1, T2, T3, T4, T5> void runCommand(E id, ServerClient<E> client, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        var command = (ServerCommand5<ServerClient<E>, T1, T2, T3, T4, T5>) getCommands().get(id);
        command.runCommand(client, t1, t2, t3, t4, t5);
    }

    @SuppressWarnings("unchecked")
    default <T1, T2, T3, T4, T5, T6> void runCommand(E id, ServerClient<E> client, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        var command = (ServerCommand6<ServerClient<E>, T1, T2, T3, T4, T5, T6>) getCommands().get(id);
        command.runCommand(client, t1, t2, t3, t4, t5, t6);
    }

    private void registerCommand(E id, ServerCommand command) {
        idReplacementCheck(id);
        commandArgumentMatchCheck(id, command);
        getCommands().put(id, command);
    }

    private void commandArgumentMatchCheck(E id, ServerCommand command) {
        if (id.commandCount() != command.commandArgumentCount()) {
            throw new IllegalArgumentException(
                "The number of command parameters must match to " + id.name()
                    + "'s parameter count of " + id.commandCount()
            );
        }
    }

    private void idReplacementCheck(E id) {
        if (getCommands().containsKey(id)) {
            getLogger().debug("Updating command {}.", id.name());
        }
    }
}
