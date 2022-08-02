package unittest.serial;

import tech.fastj.network.serial.Message;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.util.MessageUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import mock.ChatMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SerializerTests {

    @Test
    void checkCreateSerializer_withMessageMapping() {
        UUID chatMessageId = UUID.randomUUID();
        Map<UUID, Class<? extends Message>> messageTypes = Map.of(chatMessageId, ChatMessage.class);
        Serializer serializer = new Serializer(messageTypes);

        assertNotNull(
            serializer.getSerializer(ChatMessage.class),
            "After constructing a serializer with a map of a message and id, the message should be registered in the serializer."
        );
    }

    @Test
    void checkReadAndWriteMessage() throws IOException {
        Serializer serializer = new Serializer();

        UUID chatMessageId = UUID.randomUUID();
        serializer.registerSerializer(chatMessageId, ChatMessage.class);

        ChatMessage messageOut = new ChatMessage("lucasstarsz", System.currentTimeMillis(), "Hello world!");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        serializer.writeMessage(outputStream, messageOut);
        byte[] data = outputStream.toByteArray();

        assertEquals(
            MessageUtils.bytesLength(serializer, messageOut), data.length,
            "The length of the written message should only involve the message data."
        );

        ChatMessage messageIn = (ChatMessage) serializer.readMessage(new ByteArrayInputStream(data), ChatMessage.class);
        assertEquals(messageOut, messageIn, "The message read in should match the message written out.");
    }

    @Test
    void checkReadAndWriteMessage_noStreamSpecified() throws IOException {
        Serializer serializer = new Serializer();

        UUID chatMessageId = UUID.randomUUID();
        serializer.registerSerializer(chatMessageId, ChatMessage.class);

        ChatMessage messageOut = new ChatMessage("lucasstarsz", System.currentTimeMillis(), "Hello world!");

        byte[] data = serializer.writeMessage(messageOut);

        assertEquals(
            MessageUtils.bytesLength(serializer, messageOut), data.length,
            "The length of the written message should only involve the message data."
        );

        ChatMessage messageIn = (ChatMessage) serializer.readMessage(data, ChatMessage.class);
        assertEquals(messageOut, messageIn, "The message read in should match the message written out.");
    }
}
