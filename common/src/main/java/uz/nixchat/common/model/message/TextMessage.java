package uz.nixchat.common.model.message;

import uz.nixchat.common.Protocol;
import uz.nixchat.common.model.User;

import java.time.Instant;

/**
 * A plain text message.
 */
public class TextMessage extends Message {

    private static final int PREVIEW_LENGTH = 40;

    private final String text;

    public TextMessage(String id, String chatId, User sender, Instant sentAt, String text) {
        super(id, chatId, sender, sentAt);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text message cannot be empty");
        }
        if (text.length() > Protocol.MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message is longer than " + Protocol.MAX_MESSAGE_LENGTH + " characters");
        }
        this.text = text;
    }

    public TextMessage(String chatId, User sender, String text) {
        this(null, chatId, sender, null, text);
    }

    public String getText() {
        return text;
    }

    @Override
    public String preview() {
        return text.length() <= PREVIEW_LENGTH ? text : text.substring(0, PREVIEW_LENGTH) + "…";
    }

    @Override
    public String format() {
        // In the feed a text message is shown in full, not shortened like in the preview
        String author = (getSender() == null) ? "NixChat" : getSender().getUsername();
        return "[" + formattedTime() + "] " + author + ": " + text;
    }

    @Override
    public String getType() {
        return "TEXT";
    }
}
