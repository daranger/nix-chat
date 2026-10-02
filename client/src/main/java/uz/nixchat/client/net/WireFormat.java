package uz.nixchat.client.net;

import uz.nixchat.common.model.User;
import uz.nixchat.common.model.message.Message;
import uz.nixchat.common.model.message.SystemMessage;
import uz.nixchat.common.model.message.TextMessage;

/**
 * Reads frames from the server. The client sends plain text; the server adds the verified sender:
 * {@code "username: text"}. Frames without that prefix ("alice joined the chat") are system notices.
 * Will be replaced by JSON once the protocol is finalised.
 */
public final class WireFormat {

    private static final String SEPARATOR = ": ";

    private WireFormat() {
    }

    /** Turns a raw frame from the server into a message; anything unrecognised becomes a system notice. */
    public static Message decode(String raw, String chatId) {
        int separator = raw.indexOf(SEPARATOR);
        if (separator > 0) {
            String username = raw.substring(0, separator);
            String text = raw.substring(separator + SEPARATOR.length());
            if (User.isValidUsername(username) && !text.isBlank()) {
                return new TextMessage(chatId, new User(username), text);
            }
        }
        return new SystemMessage(chatId, raw);
    }
}
