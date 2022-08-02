package tech.fastj.network.rpc.server.command;

import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.server.ServerClient;
import tech.fastj.network.rpc.server.Session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface SessionCommandReader<H extends Enum<H> & CommandAlias> extends ServerCommandReader<H> {

    List<H> getPendingResponses();

    Map<Session.ResponseId<H>, Object[]> getResponses();

    default void trackResponses(H responseId) {
        getPendingResponses().add(responseId);
    }

    default boolean hasAllResponses(H responseId, List<ServerClient<H>> clients) {
        for (ServerClient<H> client : clients) {
            Session.ResponseId<H> id = new Session.ResponseId<>(responseId, client.getClientId());

            if (!getResponses().containsKey(id)) {
                return false;
            }
        }

        return true;
    }

    default Map<Session.ResponseId<H>, Object[]> drainResponses(H responseId) {
        Map<Session.ResponseId<H>, Object[]> responseResults = new HashMap<>();

        for (Session.ResponseId<H> id : getResponses().keySet()) {
            if (responseId.equals(id.commandId())) {
                responseResults.put(id, getResponses().get(id));
            }
        }

        for (Map.Entry<Session.ResponseId<H>, Object[]> responseResult : responseResults.entrySet()) {
            getResponses().remove(responseResult.getKey(), responseResult.getValue());
        }

        return responseResults;
    }

    @Override
    default void runCommand(H id, ServerClient<H> client) {
        ServerCommandReader.super.runCommand(id, client);

        if (getPendingResponses().contains(id)) {
            getResponses().put(new Session.ResponseId<>(id, client.getClientId()), new Object[0]);
        }
    }

    @Override
    default <T1> void runCommand(H id, ServerClient<H> client, T1 t1) {
        ServerCommandReader.super.runCommand(id, client, t1);

        if (getPendingResponses().contains(id)) {
            getResponses().put(new Session.ResponseId<>(id, client.getClientId()), new Object[] {t1});
        }
    }

    @Override
    default <T1, T2> void runCommand(H id, ServerClient<H> client, T1 t1, T2 t2) {
        ServerCommandReader.super.runCommand(id, client, t1, t2);

        if (getPendingResponses().contains(id)) {
            getResponses().put(new Session.ResponseId<>(id, client.getClientId()), new Object[] {t1, t2});
        }
    }

    @Override
    default <T1, T2, T3> void runCommand(H id, ServerClient<H> client, T1 t1, T2 t2, T3 t3) {
        ServerCommandReader.super.runCommand(id, client, t1, t2, t3);

        if (getPendingResponses().contains(id)) {
            getResponses().put(new Session.ResponseId<>(id, client.getClientId()), new Object[] {t1, t2, t3});
        }
    }

    @Override
    default <T1, T2, T3, T4> void runCommand(H id, ServerClient<H> client, T1 t1, T2 t2, T3 t3, T4 t4) {
        ServerCommandReader.super.runCommand(id, client, t1, t2, t3, t4);

        if (getPendingResponses().contains(id)) {
            getResponses().put(new Session.ResponseId<>(id, client.getClientId()), new Object[] {t1, t2, t3, t4});
        }
    }

    @Override
    default <T1, T2, T3, T4, T5> void runCommand(H id, ServerClient<H> client, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        ServerCommandReader.super.runCommand(id, client, t1, t2, t3, t4, t5);

        if (getPendingResponses().contains(id)) {
            getResponses().put(new Session.ResponseId<>(id, client.getClientId()), new Object[] {t1, t2, t3, t4, t5});
        }
    }

    @Override
    default <T1, T2, T3, T4, T5, T6> void runCommand(H id, ServerClient<H> client, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        ServerCommandReader.super.runCommand(id, client, t1, t2, t3, t4, t5, t6);

        if (getPendingResponses().contains(id)) {
            getResponses().put(new Session.ResponseId<>(id, client.getClientId()), new Object[] {t1, t2, t3, t4, t5, t6});
        }
    }
}
