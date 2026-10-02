package uz.nixchat.server.auth;

/**
 * JSON bodies of the /api/auth endpoints.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(String username, String displayName, String password) {
    }

    public record LoginRequest(String username, String password) {
    }

    /** Returned after a successful registration or login. */
    public record AuthResponse(String token, String username, String displayName) {
    }

    public record MeResponse(String username, String displayName) {
    }

    public record ErrorResponse(String error) {
    }
}
