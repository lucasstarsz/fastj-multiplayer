package tech.fastj.network.sessions;

import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.CommandHandler;
import tech.fastj.network.rpc.ConnectionHandler;
import tech.fastj.network.rpc.ServerClient;
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

public abstract class SessionHandler<H extends Enum<H> & CommandAlias, T extends ConnectionHandler<H, T>> extends CommandHandler<H, T> {

    private final List<H> pendingResponses;
    private final Map<ResponseId<H>, Object[]> responses;

    protected SessionHandler(Class<H> aliasClass) {
        super(aliasClass);
        pendingResponses = new ArrayList<>();
        responses = new HashMap<>();
    }

    public void trackResponses(H responseId) {
        idRegisterCheck(responseId);
        pendingResponses.add(responseId);
    }

    public boolean hasAllResponses(H responseId, List<ServerClient<H>> clients) {
        for (ServerClient<H> client : clients) {
            ResponseId<H> id = new ResponseId<>(responseId, client.getClientId());

            if (!responses.containsKey(id)) {
                return false;
            }
        }

        return true;
    }

    public Map<ResponseId<H>, Object[]> drainResponses(H responseId) {
        Map<ResponseId<H>, Object[]> responseResults = new HashMap<>();

        for (ResponseId<H> id : responses.keySet()) {
            if (responseId.equals(id.commandId())) {
                responseResults.put(id, responses.get(id));
            }
        }

        for (Map.Entry<ResponseId<H>, Object[]> responseResult : responseResults.entrySet()) {
            responses.remove(responseResult.getKey(), responseResult.getValue());
        }

        return responseResults;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void runCommand(H id, T client) {
        var command = (Command0<T>) commands.get(id);
        command.runCommand(client);

        if (pendingResponses.contains(id)) {
            responses.put(new ResponseId<>(id, client.getClientId()), new Object[0]);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T1> void runCommand(H id, T client, T1 t1) {
        var command = (Command1<T, T1>) commands.get(id);
        command.runCommand(client, t1);

        if (pendingResponses.contains(id)) {
            responses.put(new ResponseId<>(id, client.getClientId()), new Object[] {t1});
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T1, T2> void runCommand(H id, T client, T1 t1, T2 t2) {
        var command = (Command2<T, T1, T2>) commands.get(id);
        command.runCommand(client, t1, t2);

        if (pendingResponses.contains(id)) {
            responses.put(new ResponseId<>(id, client.getClientId()), new Object[] {t1, t2});
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T1, T2, T3> void runCommand(H id, T client, T1 t1, T2 t2, T3 t3) {
        var command = (Command3<T, T1, T2, T3>) commands.get(id);
        command.runCommand(client, t1, t2, t3);

        if (pendingResponses.contains(id)) {
            responses.put(new ResponseId<>(id, client.getClientId()), new Object[] {t1, t2, t3});
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T1, T2, T3, T4> void runCommand(H id, T client, T1 t1, T2 t2, T3 t3, T4 t4) {
        var command = (Command4<T, T1, T2, T3, T4>) commands.get(id);
        command.runCommand(client, t1, t2, t3, t4);

        if (pendingResponses.contains(id)) {
            responses.put(new ResponseId<>(id, client.getClientId()), new Object[] {t1, t2, t3, t4});
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T1, T2, T3, T4, T5> void runCommand(H id, T client, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        var command = (Command5<T, T1, T2, T3, T4, T5>) commands.get(id);
        command.runCommand(client, t1, t2, t3, t4, t5);

        if (pendingResponses.contains(id)) {
            responses.put(new ResponseId<>(id, client.getClientId()), new Object[] {t1, t2, t3, t4, t5});
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T1, T2, T3, T4, T5, T6> void runCommand(H id, T client, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        var command = (Command6<T, T1, T2, T3, T4, T5, T6>) commands.get(id);
        command.runCommand(client, t1, t2, t3, t4, t5, t6);

        if (pendingResponses.contains(id)) {
            responses.put(new ResponseId<>(id, client.getClientId()), new Object[] {t1, t2, t3, t4, t5, t6});
        }
    }
}
