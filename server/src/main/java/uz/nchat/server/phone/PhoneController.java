package uz.nchat.server.phone;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.nchat.server.phone.PhoneService.LinkStarted;
import uz.nchat.server.phone.PhoneService.Match;

import java.util.List;

/**
 * <ul>
 *   <li>{@code POST /api/phone/start {phone}} — send a code (Telegram or console in dev mode)</li>
 *   <li>{@code POST /api/phone/verify {code}} — link the number (only its hash is stored)</li>
 *   <li>{@code DELETE /api/phone} — unlink</li>
 *   <li>{@code POST /api/contacts/lookup {phoneKeys}} — which contacts use Nchat</li>
 * </ul>
 */
@RestController
public class PhoneController {

    public record StartRequest(String phone) {
    }

    public record VerifyRequest(String code) {
    }

    public record LookupRequest(List<String> phoneKeys) {
    }

    private final PhoneService phoneService;

    public PhoneController(PhoneService phoneService) {
        this.phoneService = phoneService;
    }

    @PostMapping("/api/phone/start")
    public LinkStarted start(@AuthenticationPrincipal Jwt jwt, @RequestBody StartRequest request) {
        return phoneService.startLink(jwt.getSubject(), request.phone());
    }

    @PostMapping("/api/phone/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@AuthenticationPrincipal Jwt jwt, @RequestBody VerifyRequest request) {
        phoneService.confirmLink(jwt.getSubject(), request.code());
    }

    @DeleteMapping("/api/phone")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@AuthenticationPrincipal Jwt jwt) {
        phoneService.unlink(jwt.getSubject());
    }

    @PostMapping("/api/contacts/lookup")
    public List<Match> lookup(@AuthenticationPrincipal Jwt jwt, @RequestBody LookupRequest request) {
        return phoneService.lookup(jwt.getSubject(), request.phoneKeys());
    }
}
