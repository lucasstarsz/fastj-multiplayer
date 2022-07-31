package tech.fastj.network.sessions;

import tech.fastj.network.rpc.CommandAlias;

import java.util.UUID;

public record ResponseId<H extends Enum<H> & CommandAlias>(H commandId, UUID clientId) {
}
