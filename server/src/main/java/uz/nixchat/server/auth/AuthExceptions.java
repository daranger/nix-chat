package uz.nixchat.server.auth;

/**
 * Errors of the login and registration flow; the API maps each one to its own HTTP status.
 */
public final class AuthExceptions {

    private AuthExceptions() {
    }

    /** 409 Conflict. */
    public static class UsernameTakenException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public UsernameTakenException(String username) {
            super("Username '" + username + "' is already taken");
        }
    }

    /** 401 Unauthorized. The message never says which part was wrong, so usernames cannot be guessed. */
    public static class InvalidCredentialsException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public InvalidCredentialsException() {
            super("Invalid username or password");
        }
    }
}
