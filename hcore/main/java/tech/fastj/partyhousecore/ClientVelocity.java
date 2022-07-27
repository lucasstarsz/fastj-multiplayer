package tech.fastj.partyhousecore;

import tech.fastj.network.serial.Message;

public record ClientVelocity(float angle, float speed) implements Message {
    public ClientVelocity() {
        this(0f, 0f);
    }
}