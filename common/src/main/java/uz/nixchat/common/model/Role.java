package uz.nixchat.common.model;

/**
 * Role of a participant inside a group chat.
 */
public enum Role {
    OWNER(3),
    ADMIN(2),
    MEMBER(1);

    private final int level;

    Role(int level) {
        this.level = level;
    }

    /** Owners and admins can manage members; an owner outranks an admin. */
    public boolean canManage(Role other) {
        return this != MEMBER && this.level > other.level;
    }

    public boolean canPost() {
        return true;
    }
}
