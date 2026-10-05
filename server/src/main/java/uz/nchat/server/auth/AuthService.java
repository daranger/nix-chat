package uz.nchat.server.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.nchat.common.model.User;
import uz.nchat.server.auth.AuthDtos.AuthResponse;
import uz.nchat.server.auth.AuthExceptions.InvalidCredentialsException;
import uz.nchat.server.auth.AuthExceptions.UsernameTakenException;

import java.util.Locale;

/**
 * Registration and login. Passwords are hashed with BCrypt; plain passwords are never stored or logged.
 */
@Service
public class AuthService {

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 72; // BCrypt only uses the first 72 bytes
    public static final int MAX_DISPLAY_NAME_LENGTH = 64;

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public AuthService(UserAccountRepository users, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResponse register(String username, String displayName, String password) {
        String cleanUsername = normalize(username);
        if (!User.isValidUsername(cleanUsername)) {
            throw new IllegalArgumentException("Username must be 3-32 characters: letters, digits, _ and -");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be " + MIN_PASSWORD_LENGTH + "-" + MAX_PASSWORD_LENGTH + " characters");
        }
        String cleanDisplayName = (displayName == null || displayName.isBlank()) ? cleanUsername : displayName.strip();
        if (cleanDisplayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("Display name must be at most " + MAX_DISPLAY_NAME_LENGTH + " characters");
        }
        if (users.existsByUsername(cleanUsername)) {
            throw new UsernameTakenException(cleanUsername);
        }

        UserAccount account = users.save(
                new UserAccount(cleanUsername, cleanDisplayName, passwordEncoder.encode(password)));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(String username, String password) {
        UserAccount account = users.findByUsername(normalize(username))
                .orElseThrow(InvalidCredentialsException::new);
        if (password == null || !passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public UserAccount findAccount(String username) {
        return users.findByUsername(username).orElseThrow(InvalidCredentialsException::new);
    }

    /** Usernames are case-insensitive: "Roman" and "roman" are the same account. */
    private static String normalize(String username) {
        return username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
    }

    private AuthResponse toResponse(UserAccount account) {
        return new AuthResponse(tokens.issue(account), account.getUsername(), account.getDisplayName());
    }
}
