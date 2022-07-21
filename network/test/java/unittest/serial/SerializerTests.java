package unittest.serial;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import mock.ChatMessage;
import org.junit.jupiter.api.Test;
import tech.fastj.network.serial.Message;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.util.MessageUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SerializerTests {

    @Test
    void checkCreateSerializer_withMessageMapping() {
        UUID chatMessageId = UUID.randomUUID();
        Map<UUID, Class<? extends Message>> networkableTypes = Map.of(chatMessageId, ChatMessage.class);
        Serializer serializer = new Serializer(networkableTypes);

        assertNotNull(
                serializer.getSerializer(ChatMessage.class),
                "After constructing a serializer with a map of a networkable and id, the networkable should be registered in the serializer."
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
                "The length of the written networkable should only involve the networkable data."
        );

        ChatMessage messageIn = (ChatMessage) serializer.readMessage(new ByteArrayInputStream(data), ChatMessage.class);
        assertEquals(messageOut, messageIn, "The networkable read in should match the networkable written out.");
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
                "The length of the written networkable should only involve the networkable data."
        );

        ChatMessage messageIn = (ChatMessage) serializer.readMessage(data, ChatMessage.class);
        assertEquals(messageOut, messageIn, "The networkable read in should match the networkable written out.");
    }
}
