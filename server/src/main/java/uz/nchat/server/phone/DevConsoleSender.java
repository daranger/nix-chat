package uz.nchat.server.phone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.nchat.common.crypto.PhoneHasher;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Development mode: instead of sending the code, prints it to the server console.
 * Used when no Telegram Gateway token is configured, so the team can test for free.
 */
public class DevConsoleSender implements VerificationSender {

    private static final Logger log = LoggerFactory.getLogger(DevConsoleSender.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, String> codes = new ConcurrentHashMap<>();
    private volatile String lastCode;

    @Override
    public String send(String e164Phone) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String requestId = UUID.randomUUID().toString();
        codes.put(requestId, code);
        lastCode = code;
        log.warn("[DEV] Verification code for {}: {}", PhoneHasher.mask(e164Phone), code);
        return requestId;
    }

    @Override
    public boolean check(String requestId, String code) {
        String expected = codes.get(requestId);
        if (expected == null || code == null) {
            return false;
        }
        // constant-time comparison
        boolean ok = MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), code.strip().getBytes(StandardCharsets.UTF_8));
        if (ok) {
            codes.remove(requestId);
        }
        return ok;
    }

    /** For tests only. */
    String lastCode() {
        return lastCode;
    }

    @Override
    public String name() {
        return "development console (no Telegram token set)";
    }
}
