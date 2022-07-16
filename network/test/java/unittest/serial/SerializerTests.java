package unittest.serial;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import mock.ChatMessage;
import org.junit.jupiter.api.Test;
import tech.fastj.network.serial.Networkable;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.util.NetworkableUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SerializerTests {

    @Test
    void checkCreateSerializer_withNetworkableMapping() {
        byte chatMessageId = (byte) ThreadLocalRandom.current().nextInt(0, Byte.MAX_VALUE);
        Map<Byte, Class<? extends Networkable>> networkableTypes = Map.of(chatMessageId, ChatMessage.class);
        Serializer serializer = new Serializer(networkableTypes);

        assertNotNull(
                serializer.getSerializer(ChatMessage.class),
                "After constructing a serializer with a map of a networkable and id, the networkable should be registered in the serializer."
        );
    }

    @Test
    void checkReadAndWriteMessage() throws IOException {
        Serializer serializer = new Serializer();

        byte chatMessageId = (byte) ThreadLocalRandom.current().nextInt(0, Byte.MAX_VALUE);
        serializer.registerSerializer(chatMessageId, ChatMessage.class);

        ChatMessage messageOut = new ChatMessage("lucasstarsz", System.currentTimeMillis(), "Hello world!");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        serializer.writeNetworkable(outputStream, messageOut);
        byte[] data = outputStream.toByteArray();

        assertEquals(
                NetworkableUtils.bytesLength(serializer, messageOut), data.length,
                "The length of the written networkable should only involve the networkable data."
        );

        ChatMessage messageIn = (ChatMessage) serializer.readNetworkable(new ByteArrayInputStream(data), ChatMessage.class);
        assertEquals(messageOut, messageIn, "The networkable read in should match the networkable written out.");
    }

    @Test
    void checkReadAndWriteMessage_noStreamSpecified() throws IOException {
        Serializer serializer = new Serializer();

        byte chatMessageId = (byte) ThreadLocalRandom.current().nextInt(0, Byte.MAX_VALUE);
        serializer.registerSerializer(chatMessageId, ChatMessage.class);

        ChatMessage messageOut = new ChatMessage("lucasstarsz", System.currentTimeMillis(), "Hello world!");

        byte[] data = serializer.writeNetworkable(messageOut);

        assertEquals(
                NetworkableUtils.bytesLength(serializer, messageOut), data.length,
                "The length of the written networkable should only involve the networkable data."
        );

        ChatMessage messageIn = (ChatMessage) serializer.readNetworkable(data, ChatMessage.class);
        assertEquals(messageOut, messageIn, "The networkable read in should match the networkable written out.");
    }
}
