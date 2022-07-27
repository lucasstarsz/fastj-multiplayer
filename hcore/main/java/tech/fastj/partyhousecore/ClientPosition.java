package tech.fastj.partyhousecore;

import tech.fastj.network.serial.Message;

public record ClientPosition(float x, float y) implements Message {
    public ClientPosition() {
        this(0f, 0f);
    }
}
