package uz.nchat.server.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.nchat.server.auth.AuthDtos.AuthResponse;
import uz.nchat.server.auth.AuthDtos.LoginRequest;
import uz.nchat.server.auth.AuthDtos.MeResponse;
import uz.nchat.server.auth.AuthDtos.RegisterRequest;

/**
 * REST API for accounts.
 * <ul>
 *   <li>{@code POST /api/auth/register} — create an account, returns a token</li>
 *   <li>{@code POST /api/auth/login} — returns a token</li>
 *   <li>{@code GET /api/me} — who am I (requires the token)</li>
 * </ul>
 */
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Time complexity: O(log n), where n is the number of users. The controller only passes
    // the three fields on, and AuthService.register() does the work: one indexed check and
    // one insert, both O(log n) (see the explanation at the end of AuthService).
    // Space complexity: O(1). The request holds three short strings with fixed maximum
    // lengths, and the response holds one token and two names.
    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request.username(), request.displayName(), request.password());
    }

    // Time complexity: O(log n). One indexed lookup by username plus a BCrypt check,
    // which takes the same time for any number of users, so it counts as O(1).
    // Space complexity: O(1). One request and one response object.
    @PostMapping("/api/auth/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    // Time complexity: O(log n). The token is checked by Spring Security before this method
    // runs (a signature check on a short token, O(1)), then we find the account by username
    // through the index in O(log n).
    // Space complexity: O(1). We hold one account and create one small response.
    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        UserAccount account = authService.findAccount(jwt.getSubject());
        return new MeResponse(account.getUsername(), account.getDisplayName(), account.getPhoneHash() != null);
    }

    /*
     * HOW WE DERIVED THE TIME COMPLEXITY
     *
     * Let n be the number of users in the database.
     *
     * A controller does not do any heavy work itself. Spring reads the JSON body into a
     * request object, which is O(1) because every field has a fixed maximum length
     * (username 32, display name 64, password 72). Then each method calls AuthService
     * exactly once, so the cost of the controller is the cost of that service method.
     *
     * register(): AuthService.register() is O(log n), so this method is O(log n).
     * login():    AuthService.login() is O(log n), so this method is O(log n).
     * me():       verifying the token is O(1), and findAccount() is O(log n), so the total
     *             is O(1 + log n) = O(log n).
     *
     * HOW WE DERIVED THE SPACE COMPLEXITY
     *
     * Each method creates one request object and one response object, and both have a fixed
     * number of short fields. Nothing grows with n, so all three methods use O(1) space.
     */
}
