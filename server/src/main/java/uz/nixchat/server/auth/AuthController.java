package uz.nixchat.server.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.nixchat.server.auth.AuthDtos.AuthResponse;
import uz.nixchat.server.auth.AuthDtos.LoginRequest;
import uz.nixchat.server.auth.AuthDtos.MeResponse;
import uz.nixchat.server.auth.AuthDtos.RegisterRequest;

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

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request.username(), request.displayName(), request.password());
    }

    @PostMapping("/api/auth/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        UserAccount account = authService.findAccount(jwt.getSubject());
        return new MeResponse(account.getUsername(), account.getDisplayName(), account.getPhoneHash() != null);
    }
}
