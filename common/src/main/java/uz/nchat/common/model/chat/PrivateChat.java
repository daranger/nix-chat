package uz.nchat.common.model.chat;

import uz.nchat.common.model.User;

/**
 * A one-to-one conversation between exactly two users.
 */
public class PrivateChat extends Chat {

    private boolean blocked;

    public PrivateChat(User first, User second) {
        super(idFor(first, second));
        if (first.equals(second)) {
            throw new IllegalArgumentException("A private chat needs two different users");
        }
        addParticipant(first);
        addParticipant(second);
    }

    /** The same pair of users always gets the same chat id, whatever the order. */
    public static String idFor(User a, User b) {
        String x = a.getUsername();
        String y = b.getUsername();
        return x.compareTo(y) < 0 ? "pm:" + x + ":" + y : "pm:" + y + ":" + x;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isBlocked() {
        return blocked;
    }

    /** The other participant, as seen by the viewer. */
    public User getPartner(User viewer) {
        for (User participant : getParticipants()) {
            if (!participant.equals(viewer)) {
                return participant;
            }
        }
        throw new IllegalArgumentException(viewer + " is not in this chat");
    }

    @Override
    public boolean canWrite(User user) {
        return !blocked && isParticipant(user);
    }

    @Override
    public String getTitle(User viewer) {
        return getPartner(viewer).getDisplayName();
    }

    @Override
    public int getMaxParticipants() {
        return 2;
    }
}
