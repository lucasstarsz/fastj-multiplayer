package unittest.serial.util;

import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import mock.ChatMessage;
import org.junit.jupiter.api.Test;
import tech.fastj.network.serial.Networkable;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.util.NetworkableUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkableUtilsTests {

    @Test
    void checkGetByteSize() {
        assertEquals(NetworkableUtils.MinStringBytes, NetworkableUtils.bytesLength((String) null), "The length of a written null string should be the minimum amount.");
        assertEquals(NetworkableUtils.MinStringBytes + 1, NetworkableUtils.bytesLength("a"));
        assertEquals(NetworkableUtils.MinStringBytes + 12, NetworkableUtils.bytesLength("Hello world!"));
        assertEquals(NetworkableUtils.MinStringBytes * 2, NetworkableUtils.bytesLength("", ""));
        assertEquals((NetworkableUtils.MinStringBytes + 1) * 2, NetworkableUtils.bytesLength("a", "b"));

        String username = "lucasstarsz";
        String message = "Hello world!";
        Networkable chatMessage = new ChatMessage(username, System.currentTimeMillis(), message);
        int expectedMessageSize = NetworkableUtils.MinNetworkableBytes + (NetworkableUtils.MinStringBytes + username.length()) + Long.BYTES + (NetworkableUtils.MinStringBytes + message.length());

        Serializer serializer = new Serializer();
        byte chatMessageId = (byte) ThreadLocalRandom.current().nextInt(0, Byte.MAX_VALUE);
        serializer.registerSerializer(chatMessageId, ChatMessage.class);

        assertEquals(NetworkableUtils.MinNetworkableBytes, NetworkableUtils.bytesLength(serializer, (Networkable) null), "The length of a written null networkable should be the minimum amount.");
        assertEquals(expectedMessageSize, NetworkableUtils.bytesLength(serializer, chatMessage));
        assertEquals(4 * expectedMessageSize, NetworkableUtils.bytesLength(serializer, chatMessage, chatMessage, chatMessage, chatMessage));

        assertEquals(NetworkableUtils.UuidBytes, NetworkableUtils.bytesLength(serializer, UUID.randomUUID()));
        assertEquals(NetworkableUtils.EnumBytes, NetworkableUtils.bytesLength(serializer, StandardCopyOption.ATOMIC_MOVE));
    }
}