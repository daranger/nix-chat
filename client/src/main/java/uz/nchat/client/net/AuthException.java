package uz.nchat.client.net;

/**
 * The server refused a login or registration (wrong password, username taken, invalid input).
 * The message is the server's own explanation and can be shown to the user as is.
 */
public class AuthException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public AuthException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /** 401: the token expired or the password was wrong. */
    public boolean isUnauthorized() {
        return statusCode == 401;
    }
}
