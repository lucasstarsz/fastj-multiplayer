package tech.fastj.partyhousecore;

import tech.fastj.network.serial.Serializer;

public class Messages {
    public static void updateSerializer(Serializer serializer) {
        serializer.registerSerializer(ClientInfo.class);
        serializer.registerSerializer(ClientPosition.class);
        serializer.registerSerializer(ClientVelocity.class);
    }
}
