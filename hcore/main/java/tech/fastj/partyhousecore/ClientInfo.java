package tech.fastj.partyhousecore;

import tech.fastj.network.serial.Message;

import java.util.UUID;

public record ClientInfo(UUID clientId, String clientName) implements Message {}
