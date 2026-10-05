package uz.nchat.common.model.chat;

import uz.nchat.common.model.Role;
import uz.nchat.common.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * A group conversation with a name, an owner and roles for every member.
 */
public class GroupChat extends Chat {

    public static final int MAX_MEMBERS = 200;

    private String name;
    private boolean readOnly;
    private final Map<User, Role> roles = new HashMap<>();

    public GroupChat(String id, String name, User owner) {
        super(id);
        rename(name);
        addParticipant(owner);
        roles.put(owner, Role.OWNER);
    }

    public void addMember(User actor, User newMember) {
        Role actorRole = getRole(actor);
        if (actorRole == null || actorRole == Role.MEMBER) {
            throw new SecurityException(actor.getUsername() + " cannot add members");
        }
        if (addParticipant(newMember)) {
            roles.put(newMember, Role.MEMBER);
        }
    }

    public void kick(User actor, User target) {
        Role actorRole = getRole(actor);
        Role targetRole = getRole(target);
        if (actorRole == null || targetRole == null || !actorRole.canManage(targetRole)) {
            throw new SecurityException(actor.getUsername() + " cannot remove " + target.getUsername());
        }
        removeParticipant(target);
        roles.remove(target);
    }

    public void promote(User owner, User member) {
        if (getRole(owner) != Role.OWNER) {
            throw new SecurityException("Only the owner can appoint admins");
        }
        if (getRole(member) == Role.MEMBER) {
            roles.put(member, Role.ADMIN);
        }
    }

    public Role getRole(User user) {
        return roles.get(user);
    }

    public void rename(String newName) {
        if (newName == null || newName.isBlank() || newName.length() > 64) {
            throw new IllegalArgumentException("Group name must be 1-64 characters");
        }
        this.name = newName.strip();
    }

    /** In read-only mode (like a channel) only the owner and admins can post. */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean canWrite(User user) {
        Role role = getRole(user);
        if (role == null) {
            return false;
        }
        return readOnly ? role != Role.MEMBER : role.canPost();
    }

    @Override
    public String getTitle(User viewer) {
        return name + " · " + getParticipants().size() + " members";
    }

    @Override
    public int getMaxParticipants() {
        return MAX_MEMBERS;
    }
}
