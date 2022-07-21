package unittest.serial.util;

import java.nio.file.StandardCopyOption;
import java.util.UUID;

import mock.ChatMessage;
import org.junit.jupiter.api.Test;
import tech.fastj.network.serial.Message;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.util.MessageUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageUtilsTests {

    @Test
    void checkGetByteSize() {
        assertEquals(MessageUtils.MinStringBytes, MessageUtils.bytesLength((String) null), "The length of a written null string should be the minimum amount.");
        assertEquals(MessageUtils.MinStringBytes + 1, MessageUtils.bytesLength("a"));
        assertEquals(MessageUtils.MinStringBytes + 12, MessageUtils.bytesLength("Hello world!"));
        assertEquals(MessageUtils.MinStringBytes * 2, MessageUtils.bytesLength("", ""));
        assertEquals((MessageUtils.MinStringBytes + 1) * 2, MessageUtils.bytesLength("a", "b"));

        String username = "lucasstarsz";
        String message = "Hello world!";
        Message chatMessage = new ChatMessage(username, System.currentTimeMillis(), message);
        int expectedMessageSize = MessageUtils.MinMessageBytes + (MessageUtils.MinStringBytes + username.length()) + Long.BYTES + (MessageUtils.MinStringBytes + message.length());

        Serializer serializer = new Serializer();
        UUID chatMessageId = UUID.randomUUID();
        serializer.registerSerializer(chatMessageId, ChatMessage.class);

        assertEquals(MessageUtils.MinMessageBytes, MessageUtils.bytesLength(serializer, (Message) null), "The length of a written null networkable should be the minimum amount.");
        assertEquals(expectedMessageSize, MessageUtils.bytesLength(serializer, chatMessage));
        assertEquals(4 * expectedMessageSize, MessageUtils.bytesLength(serializer, chatMessage, chatMessage, chatMessage, chatMessage));

        assertEquals(MessageUtils.UuidBytes, MessageUtils.bytesLength(serializer, UUID.randomUUID()));
        assertEquals(MessageUtils.EnumBytes, MessageUtils.bytesLength(serializer, StandardCopyOption.ATOMIC_MOVE));
    }
}