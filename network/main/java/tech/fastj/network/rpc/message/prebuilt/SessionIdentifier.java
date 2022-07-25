package tech.fastj.network.rpc.message.prebuilt;

import java.util.UUID;

public record SessionIdentifier(UUID sessionId, String sessionName) {
}
