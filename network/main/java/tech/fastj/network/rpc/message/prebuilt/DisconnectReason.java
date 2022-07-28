package tech.fastj.network.rpc.message.prebuilt;

import tech.fastj.network.serial.Message;

public record DisconnectReason(String reasonMessage, String additionalInfo) implements Message {}
