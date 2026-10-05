package uz.nchat.client.storage;

import uz.nchat.common.model.User;

import java.time.Duration;
import java.time.Instant;

/**
 * Someone the current user has talked to, with the time they were last seen.
 */
public record Contact(User user, Instant lastSeen) {

    /** "just now", "5 min ago", "3 h ago", "2 days ago". */
    public String lastSeenText(Instant now) {
        long minutes = Duration.between(lastSeen, now).toMinutes();
        if (minutes < 1) {
            return "just now";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else if (minutes < 24 * 60) {
            return (minutes / 60) + " h ago";
        } else {
            return (minutes / (24 * 60)) + " days ago";
        }
    }

    @Override
    public String toString() {
        return user.getDisplayName();
    }
}
