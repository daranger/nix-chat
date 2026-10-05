package uz.nchat.common.model;

import java.util.Objects;

/**
 * A Nchat user. Two users are equal when their usernames are equal.
 */
public class User {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 32;

    private final String username;
    private String displayName;

    public User(String username, String displayName) {
        if (!isValidUsername(username)) {
            throw new IllegalArgumentException("Invalid username: " + username);
        }
        this.username = username;
        this.displayName = (displayName == null || displayName.isBlank()) ? username : displayName;
    }

    public User(String username) {
        this(username, null);
    }

    /**
     * A username is 3-32 characters long and contains only letters, digits, '_' and '-'.
     */
    public static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        int length = username.length();
        if (length < MIN_USERNAME_LENGTH || length > MAX_USERNAME_LENGTH) {
            return false;
        }
        for (char c : username.toCharArray()) {
            boolean allowed = Character.isLetterOrDigit(c) || c == '_' || c == '-';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof User other && username.equals(other.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return displayName.equals(username) ? "@" + username : displayName + " (@" + username + ")";
    }
}
