package uz.nchat.server.phone;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.nchat.common.crypto.PhoneHasher;
import uz.nchat.server.auth.UserAccount;
import uz.nchat.server.auth.UserAccountRepository;
import uz.nchat.server.phone.PhoneExceptions.PhoneTakenException;
import uz.nchat.server.phone.PhoneExceptions.TooManyRequestsException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Links a phone number to an account and finds friends by phone — without storing any phone number.
 * <ol>
 *   <li>The number arrives once, is used to send the code and is then forgotten.</li>
 *   <li>The database keeps only HMAC(pepper, PBKDF2(number)).</li>
 *   <li>Friend lookup receives only PBKDF2 keys computed on the client.</li>
 * </ol>
 */
@Service
public class PhoneService {

    public static final int MAX_CODE_ATTEMPTS = 5;
    public static final int MAX_CODES_PER_HOUR = 5;
    public static final Duration MIN_CODE_INTERVAL = Duration.ofSeconds(60);
    public static final int MAX_LOOKUP_KEYS = 100;
    public static final int MAX_LOOKUPS_PER_HOUR = 30;

    /** A code that was sent and is waiting to be typed in. Holds a hash, never the number. */
    private record Pending(String phoneHash, String requestId, Instant expiresAt, int attempts) {
        Pending withAttempt() {
            return new Pending(phoneHash, requestId, expiresAt, attempts + 1);
        }
    }

    public record LinkStarted(String maskedPhone, long expiresInSeconds) {
    }

    public record Match(String phoneKey, String username, String displayName) {
    }

    private final UserAccountRepository users;
    private final VerificationSender sender;
    private final PhoneProperties properties;
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> codeLog = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> lookupLog = new ConcurrentHashMap<>();

    public PhoneService(UserAccountRepository users, VerificationSender sender, PhoneProperties properties) {
        this.users = users;
        this.sender = sender;
        this.properties = properties;
    }

    /** Step 1: send a code to the number. */
    @Transactional(readOnly = true)
    public LinkStarted startLink(String username, String phone) {
        String e164 = PhoneHasher.normalize(phone);
        String phoneHash = hash(PhoneHasher.phoneKey(e164));

        users.findByPhoneHash(phoneHash).ifPresent(owner -> {
            if (!owner.getUsername().equals(username)) {
                throw new PhoneTakenException();
            }
        });
        checkCodeRate(username);

        String requestId = sender.send(e164);
        pending.put(username, new Pending(phoneHash, requestId, Instant.now().plus(properties.codeTtl()), 0));
        return new LinkStarted(PhoneHasher.mask(e164), properties.codeTtl().toSeconds());
    }

    /** Step 2: check the code and store the hash. */
    @Transactional
    public void confirmLink(String username, String code) {
        Pending p = pending.get(username);
        if (p == null || Instant.now().isAfter(p.expiresAt())) {
            pending.remove(username);
            throw new IllegalArgumentException("The code has expired, request a new one");
        }
        if (p.attempts() >= MAX_CODE_ATTEMPTS) {
            pending.remove(username);
            throw new TooManyRequestsException("Too many wrong codes, request a new one");
        }
        if (!sender.check(p.requestId(), code)) {
            pending.put(username, p.withAttempt());
            throw new IllegalArgumentException("Wrong code");
        }
        pending.remove(username);

        if (users.findByPhoneHash(p.phoneHash()).filter(a -> !a.getUsername().equals(username)).isPresent()) {
            throw new PhoneTakenException();
        }
        UserAccount account = users.findByUsername(username).orElseThrow();
        account.setPhoneHash(p.phoneHash());
        users.save(account);
    }

    @Transactional
    public void unlink(String username) {
        UserAccount account = users.findByUsername(username).orElseThrow();
        account.setPhoneHash(null);
        users.save(account);
    }

    /**
     * Which of these phone keys belong to Nchat users. The client computes the keys with
     * {@link PhoneHasher#phoneKey(String)}, so the numbers of its contacts never reach the server.
     */
    @Transactional(readOnly = true)
    public List<Match> lookup(String username, List<String> phoneKeys) {
        if (phoneKeys == null || phoneKeys.isEmpty()) {
            return List.of();
        }
        if (phoneKeys.size() > MAX_LOOKUP_KEYS) {
            throw new IllegalArgumentException("At most " + MAX_LOOKUP_KEYS + " numbers per request");
        }
        checkRate(lookupLog, username, MAX_LOOKUPS_PER_HOUR, Duration.ZERO, "Too many lookups, try again later");

        Map<String, String> keyByHash = new LinkedHashMap<>();
        for (String key : phoneKeys) {
            if (!PhoneHasher.isPhoneKey(key)) {
                throw new IllegalArgumentException("Invalid phone key");
            }
            keyByHash.put(hash(key), key);
        }
        List<Match> matches = new ArrayList<>();
        for (UserAccount account : users.findByPhoneHashIn(keyByHash.keySet())) {
            if (!account.getUsername().equals(username)) {
                matches.add(new Match(keyByHash.get(account.getPhoneHash()), account.getUsername(), account.getDisplayName()));
            }
        }
        return matches;
    }

    /** HMAC-SHA256 with the secret pepper: without the pepper the stored values cannot be brute-forced. */
    String hash(String phoneKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.pepper().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(phoneKey.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }

    private void checkCodeRate(String username) {
        checkRate(codeLog, username, MAX_CODES_PER_HOUR, MIN_CODE_INTERVAL,
                "Too many codes requested, try again later");
    }

    /** Sliding one-hour window per user, plus an optional minimum gap between requests. */
    private static void checkRate(Map<String, Deque<Instant>> log, String username, int perHour,
                                  Duration minGap, String message) {
        Instant now = Instant.now();
        Deque<Instant> times = log.computeIfAbsent(username, u -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(now.minus(Duration.ofHours(1)))) {
                times.pollFirst();
            }
            if (times.size() >= perHour) {
                throw new TooManyRequestsException(message);
            }
            if (!times.isEmpty() && times.peekLast().plus(minGap).isAfter(now)) {
                throw new TooManyRequestsException("Please wait a minute before requesting another code");
            }
            times.addLast(now);
        }
    }
}
