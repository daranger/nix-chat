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

    // Time complexity: O(log n). Validation is cheap, and the two database calls
    // (existsByUsername and save) each use the username index.
    // Space complexity: O(1). We only create a few short strings (at most 32, 64 and 72
    // characters) and one account object, so memory does not grow with the number of users.
    @Transactional
    public AuthResponse register(String username, String displayName, String password) {
        // normalize reads the username once, so it takes time proportional to its length.
        String cleanUsername = normalize(username);

        // The validity check looks at each character once. The username is at most 32 characters,
        // so this is effectively constant time.
        if (!User.isValidUsername(cleanUsername)) {
            throw new IllegalArgumentException("Username must be 3-32 characters: letters, digits, _ and -");
        }

        // String.length() just returns a stored number, so these checks are O(1).
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                "Password must be " + MIN_PASSWORD_LENGTH + "-" + MAX_PASSWORD_LENGTH + " characters");
        }

        // strip() reads the display name once. It is capped at 64 characters, so this is effectively constant.
        String cleanDisplayName = (displayName == null || displayName.isBlank()) ? cleanUsername : displayName.strip();
        if (cleanDisplayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("Display name must be at most " + MAX_DISPLAY_NAME_LENGTH + " characters");
        }

        // The database searches the username index (a B-tree), which takes O(log n)
        // instead of scanning all n users.
        if (users.existsByUsername(cleanUsername)) {
            throw new UsernameTakenException(cleanUsername);
        }

        // Saving inserts a row and updates the username index, which is O(log n).
        // BCrypt hashing is O(1) in Big-O terms, because the password is at most 72 bytes
        // and the number of rounds is a fixed setting that does not depend on n.
        // The BCrypt hash is always a fixed length (60 characters), so it uses O(1) space.
        UserAccount account = users.save(
            new UserAccount(cleanUsername, cleanDisplayName, passwordEncoder.encode(password)));
        return toResponse(account); // O(1) time and space
    }

    // Time complexity: O(log n). One indexed lookup, then one fixed-cost password check.
    // Space complexity: O(1). We hold one account object and one response object.
    @Transactional(readOnly = true)
    public AuthResponse login(String username, String password) {
        // Normalizing is proportional to the username length (at most 32), then the
        // database finds the user through the index in O(log n).
        UserAccount account = users.findByUsername(normalize(username))
            .orElseThrow(InvalidCredentialsException::new);

        // BCrypt compares the password in fixed time no matter how many users exist: O(1).
        if (password == null || !passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return toResponse(account); // O(1)
    }

    // Time complexity: O(log n). It does a single indexed lookup by username.
    // Space complexity: O(1). We only hold one account object.
    @Transactional(readOnly = true)
    public UserAccount findAccount(String username) {
        return users.findByUsername(username).orElseThrow(InvalidCredentialsException::new);
    }

    // Time complexity: O(L), where L is the username length. strip() and toLowerCase()
    // each go through the string once. Since L is at most 32, this is effectively constant.
    // Space complexity: O(L). Strings are immutable, so we create new strings of length L.
    // Since L is capped at 32, this is effectively O(1).
    /** Usernames are case-insensitive: "Roman" and "roman" are the same account. */
    private static String normalize(String username) {
        return username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
    }

    // Time complexity: O(1). tokens.issue() builds and signs a token from a fixed number of
    // bounded claims (see TokenService), and the other two calls are plain getters.
    // Space complexity: O(1). We create one response object holding three values.
    private AuthResponse toResponse(UserAccount account) {
        return new AuthResponse(tokens.issue(account), account.getUsername(), account.getDisplayName());
    }

    /*
     * HOW WE DERIVED THE TIME COMPLEXITY
     *
     * Let n be the number of users in the database and L the length of the username.
     *
     * register():
     * First we checked the validation steps. Each one reads a short string once or checks a
     * length, so together they cost O(L). The two database calls (existsByUsername and save)
     * use the index on the username column. An index is a B-tree, so each call takes O(log n)
     * instead of scanning every user. BCrypt hashing takes the same time no matter how many
     * users exist, so it counts as O(1). Creating the token in toResponse() is also O(1),
     * because TokenService.issue() works on a fixed number of bounded claims and makes no
     * database call. Adding everything gives O(L + log n). Because L is limited to 32
     * characters, it is a constant, so the final answer is O(log n).
     *
     * login():
     * It normalizes the username (O(L)), finds the user by index (O(log n)), and checks
     * the password with BCrypt (O(1)). The total is O(L + log n), which simplifies to O(log n).
     *
     * findAccount():
     * It only does one indexed lookup, so it is O(log n).
     *
     * Important assumption: these O(log n) results are only true because the username column
     * has a unique index. Without an index, the database would have to scan all users and
     * the cost would be O(n).
     *
     * Note: BCrypt is O(1) in Big-O, but it is deliberately slow in real time (about 100 ms)
     * to make password guessing hard.
     *
     * HOW WE DERIVED THE SPACE COMPLEXITY
     *
     * We counted the extra memory each method creates, not the database storage.
     * Every string here has a fixed maximum length (username 32, display name 64,
     * password 72), and the BCrypt hash is always 60 characters. We also never build a
     * list or collection that grows with n. Because of this, register(), login(),
     * findAccount() and toResponse() all use O(1) space. normalize() creates new strings of
     * length L, which is O(L), but L is capped at 32, so it is also effectively O(1).
     */
}
