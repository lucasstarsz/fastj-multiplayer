package tech.fastj.network.sessions;

import java.util.UUID;

public record ResponseId(UUID commandId, UUID clientId) {
}
