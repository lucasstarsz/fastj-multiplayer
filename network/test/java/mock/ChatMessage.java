package mock;

import tech.fastj.network.serial.Message;

public record ChatMessage(String username, long timestamp, String message) implements Message {
}
