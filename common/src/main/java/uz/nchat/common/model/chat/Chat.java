package uz.nchat.common.model.chat;

import uz.nchat.common.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Base class for a conversation. Subclasses define who may write ({@link #canWrite(User)})
 * and how the chat is titled for a given viewer ({@link #getTitle(User)}).
 */
public abstract class Chat {

    private final String id;
    private final List<User> participants = new ArrayList<>();

    protected Chat(String id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    /** Can this user post messages in the chat? */
    public abstract boolean canWrite(User user);

    /** Title shown to the given viewer in the chat list. */
    public abstract String getTitle(User viewer);

    /** Maximum number of participants for this kind of chat. */
    public abstract int getMaxParticipants();

    public boolean addParticipant(User user) {
        if (user == null || isParticipant(user)) {
            return false;
        }
        if (participants.size() >= getMaxParticipants()) {
            throw new IllegalStateException("Chat " + id + " is full (" + getMaxParticipants() + " participants)");
        }
        participants.add(user);
        return true;
    }

    public boolean removeParticipant(User user) {
        return participants.remove(user);
    }

    public boolean isParticipant(User user) {
        return participants.contains(user);
    }

    public List<User> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", participants=" + participants.size() + "}";
    }
}
