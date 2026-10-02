package uz.nixchat.common.model;

import org.junit.jupiter.api.Test;
import uz.nixchat.common.model.message.FileMessage;
import uz.nixchat.common.model.message.Message;
import uz.nixchat.common.model.message.SystemMessage;
import uz.nixchat.common.model.message.TextMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageTest {

    private final User alice = new User("alice");

    @Test
    void eachMessageTypePreviewsDifferently() {
        List<Message> feed = List.of(
                new TextMessage("general", alice, "Hello!"),
                new FileMessage("general", alice, "photo.png", 1536),
                new SystemMessage("general", "bob joined"));

        assertEquals("Hello!", feed.get(0).preview());
        assertEquals("🖼 photo.png (1.5 KB)", feed.get(1).preview());
        assertEquals("ℹ bob joined", feed.get(2).preview());
    }

    @Test
    void longTextIsShortenedInPreviewButNotInFeed() {
        String text = "a".repeat(100);
        TextMessage message = new TextMessage("general", alice, text);

        assertEquals(41, message.preview().length());
        assertTrue(message.format().endsWith(text));
    }

    @Test
    void humanSizeUsesTheRightUnit() {
        assertEquals("512 B", FileMessage.humanSize(512));
        assertEquals("2.0 MB", FileMessage.humanSize(2L * 1024 * 1024));
    }

    @Test
    void invalidMessagesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TextMessage("general", alice, "   "));
        assertThrows(IllegalArgumentException.class,
                () -> new FileMessage("general", alice, "big.zip", FileMessage.MAX_SIZE_BYTES + 1));
    }

    @Test
    void usernamesAreValidated() {
        assertTrue(User.isValidUsername("guest-1234"));
        assertFalse(User.isValidUsername("ab"));
        assertFalse(User.isValidUsername("bad name"));
    }
}
