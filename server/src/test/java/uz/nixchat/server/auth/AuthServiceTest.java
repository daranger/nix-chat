package uz.nixchat.server.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtException;
import uz.nixchat.server.auth.AuthDtos.AuthResponse;
import uz.nixchat.server.auth.AuthExceptions.InvalidCredentialsException;
import uz.nixchat.server.auth.AuthExceptions.UsernameTakenException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserAccountRepository users;

    @Test
    void registerThenLogin() {
        AuthResponse registered = authService.register("Alice_1", "Alice", "correct-horse");
        assertEquals("alice_1", registered.username());
        assertEquals("Alice", registered.displayName());
        assertEquals("alice_1", tokenService.verify(registered.token()));

        AuthResponse loggedIn = authService.login("ALICE_1", "correct-horse");
        assertEquals("alice_1", loggedIn.username());
    }

    @Test
    void passwordIsStoredAsBcryptHash() {
        authService.register("bob_1", null, "secret-password");
        String hash = users.findByUsername("bob_1").orElseThrow().getPasswordHash();
        assertNotEquals("secret-password", hash);
        assertEquals("$2", hash.substring(0, 2));
    }

    @Test
    void duplicateUsernameIsRejected() {
        authService.register("carol_1", "Carol", "password-123");
        assertThrows(UsernameTakenException.class, () -> authService.register("Carol_1", "Other", "password-456"));
    }

    @Test
    void wrongPasswordAndUnknownUserGiveTheSameError() {
        authService.register("dave_1", "Dave", "password-123");
        assertThrows(InvalidCredentialsException.class, () -> authService.login("dave_1", "wrong-password"));
        assertThrows(InvalidCredentialsException.class, () -> authService.login("nobody_1", "password-123"));
    }

    @Test
    void weakInputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> authService.register("ab", null, "password-123"));
        assertThrows(IllegalArgumentException.class, () -> authService.register("erin_1", null, "short"));
    }

    @Test
    void forgedTokenIsRejected() {
        assertThrows(JwtException.class, () -> tokenService.verify("not.a.token"));
    }
}
