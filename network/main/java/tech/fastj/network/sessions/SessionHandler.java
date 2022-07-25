package tech.fastj.network.sessions;

import tech.fastj.network.rpc.CommandHandler;
import tech.fastj.network.rpc.ConnectionHandler;
import tech.fastj.network.rpc.ServerClient;
import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.rpc.commands.Command0;
import tech.fastj.network.rpc.commands.Command1;
import tech.fastj.network.rpc.commands.Command2;
import tech.fastj.network.rpc.commands.Command3;
import tech.fastj.network.rpc.commands.Command4;
import tech.fastj.network.rpc.commands.Command5;
import tech.fastj.network.rpc.commands.Command6;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class SessionHandler<T extends ConnectionHandler<?>> extends CommandHandler<T> {

    private final List<UUID> pendingResponses;
    private final Map<ResponseId, Object[]> responses;

    protected SessionHandler() {
        pendingResponses = new ArrayList<>();
        responses = new HashMap<>();
    }

    public void trackResponses(Command.Id responseId) {
        idRegisterCheck(responseId);
        pendingResponses.add(responseId.uuid());
    }

    public boolean hasAllResponses(Command.Id responseId, List<ServerClient> clients) {
        for (ServerClient client : clients) {
            ResponseId id = new ResponseId(responseId.uuid(), client.getClientId());

            if (!responses.containsKey(id)) {
                return false;
            }
        }

        return true;
    }

    public Map<ResponseId, Object[]> drainResponses(Command.Id responseId) {
        Map<ResponseId, Object[]> responseResults = new HashMap<>();

        for (ResponseId id : responses.keySet()) {
            if (responseId.uuid().equals(id.commandId())) {
                responseResults.put(id, responses.get(id));
            }
        }

        for (Map.Entry<ResponseId, Object[]> responseResult : responseResults.entrySet()) {
            responses.remove(responseResult.getKey(), responseResult.getValue());
        }

        return responseResults;
    }

    @SuppressWarnings("unchecked")
    protected void runCommand(UUID id, T client) {
        var command = (Command0<T>) commands.get(id);
        command.runCommand(client);

        if (pendingResponses.contains(id)) {
            responses.put(new ResponseId(id, client.getClientId()), new Object[0]);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T1> void runCommand(UUID id, T client, T1 t1) {
        var command = (Command1<T, T1>) commands.get(id);
        command.runCommand(client, t1);

        if (pendingResponses.contains(id)) {
            responses.put(new ResponseId(id, client.getClientId()), new Object[] {t1});
        }
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2> void runCommand(UUID id, T client, T1 t1, T2 t2) {
        var command = (Command2<T, T1, T2>) commands.get(id);
        command.runCommand(client, t1, t2);

        if (pendingResponses.contains(id)) {
            responses.put(new ResponseId(id, client.getClientId()), new Object[] {t1, t2});
        }
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3> void runCommand(UUID id, T client, T1 t1, T2 t2, T3 t3) {
        var command = (Command3<T, T1, T2, T3>) commands.get(id);
        command.runCommand(client, t1, t2, t3);

        if (pendingResponses.contains(id)) {
            responses.put(new ResponseId(id, client.getClientId()), new Object[] {t1, t2, t3});
        }
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3, T4> void runCommand(UUID id, T client, T1 t1, T2 t2, T3 t3, T4 t4) {
        var command = (Command4<T, T1, T2, T3, T4>) commands.get(id);
        command.runCommand(client, t1, t2, t3, t4);

        if (pendingResponses.contains(id)) {
            responses.put(new ResponseId(id, client.getClientId()), new Object[] {t1, t2, t3, t4});
        }
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3, T4, T5> void runCommand(UUID id, T client, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        var command = (Command5<T, T1, T2, T3, T4, T5>) commands.get(id);
        command.runCommand(client, t1, t2, t3, t4, t5);

        if (pendingResponses.contains(id)) {
            responses.put(new ResponseId(id, client.getClientId()), new Object[] {t1, t2, t3, t4, t5});
        }
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3, T4, T5, T6> void runCommand(UUID id, T client, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        var command = (Command6<T, T1, T2, T3, T4, T5, T6>) commands.get(id);
        command.runCommand(client, t1, t2, t3, t4, t5, t6);

        if (pendingResponses.contains(id)) {
            responses.put(new ResponseId(id, client.getClientId()), new Object[] {t1, t2, t3, t4, t5, t6});
        }
    }
}
