package tech.fastj.network.rpc.message.prebuilt;

import tech.fastj.network.serial.Message;

import java.util.UUID;

public record SessionIdentifier(UUID sessionId, String sessionName) implements Message {
}
