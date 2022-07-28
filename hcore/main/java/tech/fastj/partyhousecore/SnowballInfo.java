package tech.fastj.partyhousecore;

import tech.fastj.network.serial.Message;

import java.util.UUID;

public record SnowballInfo(ClientInfo clientInfo, UUID snowballId, float trajectoryX, float trajectoryY, float positionX, float positionY,
                           float rotation, float currentLife) implements Message {}
