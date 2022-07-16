package mock;

import tech.fastj.network.serial.Networkable;

public record ChatMessage(String username, long timestamp, String message) implements Networkable {
}
