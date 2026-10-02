package uz.nixchat.common.model.message;

import uz.nixchat.common.model.User;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for everything that can appear in a chat feed.
 * Subclasses decide how the message is previewed in the chat list ({@link #preview()})
 * and what type tag it is stored with ({@link #getType()}).
 */
public abstract class Message {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final String id;
    private final String chatId;
    private final User sender;
    private final Instant sentAt;

    protected Message(String id, String chatId, User sender, Instant sentAt) {
        this.id = (id == null) ? UUID.randomUUID().toString() : id;
        this.chatId = Objects.requireNonNull(chatId, "chatId");
        this.sender = sender;
        this.sentAt = (sentAt == null) ? Instant.now() : sentAt;
    }

    /** Short text shown in the chat list, e.g. "Hello!" or "📎 report.pdf". */
    public abstract String preview();

    /** Type tag used when the message is saved to a file. */
    public abstract String getType();

    /** Full line for the chat feed: "[12:30] alice: Hello". */
    public String format() {
        String author = (sender == null) ? "NixChat" : sender.getUsername();
        return "[" + formattedTime() + "] " + author + ": " + preview();
    }

    public String formattedTime() {
        return TIME_FORMAT.format(sentAt);
    }

    public boolean isFrom(User user) {
        return sender != null && sender.equals(user);
    }

    public String getId() {
        return id;
    }

    public String getChatId() {
        return chatId;
    }

    public User getSender() {
        return sender;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    @Override
    public String toString() {
        return getType() + "{" + format() + "}";
    }
}
