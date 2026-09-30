package uz.nixchat.common;

import java.time.Instant;
import java.util.Objects;

/**
 * A chat message as it travels between client and server.
 * The wire format (JSON) will be defined on top of this record.
 *
 * @param id       unique message id, assigned by the server
 * @param chatId   chat the message belongs to
 * @param senderId author of the message
 * @param text     message body (ciphertext once E2E encryption is in place)
 * @param sentAt   server timestamp
 */
public record ChatMessage(String id, String chatId, String senderId, String text, Instant sentAt) {

    public ChatMessage {
        Objects.requireNonNull(chatId, "chatId");
        Objects.requireNonNull(senderId, "senderId");
        Objects.requireNonNull(text, "text");
        if (text.length() > Protocol.MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message is longer than " + Protocol.MAX_MESSAGE_LENGTH + " characters");
        }
    }
}
