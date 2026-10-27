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
        // Time complexity: O(1). Space complexity: O(1). We copy four fields into one new record.
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
    // ConcurrentHashMap: get, put and remove are O(1) on average.
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    // Each deque holds at most MAX_CODES_PER_HOUR (5) or MAX_LOOKUPS_PER_HOUR (30) timestamps per user.
    private final Map<String, Deque<Instant>> codeLog = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> lookupLog = new ConcurrentHashMap<>();

    public PhoneService(UserAccountRepository users, VerificationSender sender, PhoneProperties properties) {
        this.users = users;
        this.sender = sender;
        this.properties = properties;
    }

    // Time complexity: O(log n). We do one hash, one indexed lookup and a few O(1) map operations.
    // Space complexity: O(1). We create a few short strings and one Pending record.
    /** Step 1: send a code to the number. */
    @Transactional(readOnly = true)
    public LinkStarted startLink(String username, String phone) {
        // normalize, phoneKey and hash each read the phone number once. Its length is limited,
        // so together they are effectively O(1).
        String e164 = PhoneHasher.normalize(phone);
        String phoneHash = hash(PhoneHasher.phoneKey(e164));

        // Search on the phone_hash index: O(log n). The lambda itself is O(1).
        users.findByPhoneHash(phoneHash).ifPresent(owner -> {
            if (!owner.getUsername().equals(username)) {
                throw new PhoneTakenException();
            }
        });
        // Rate check on a deque of at most 5 timestamps: O(1), see checkRate.
        checkCodeRate(username);

        // The SMS provider call does not depend on how many users we have, so we count it as O(1).
        String requestId = sender.send(e164);
        // Putting one entry in the hash map: O(1) on average.
        pending.put(username, new Pending(phoneHash, requestId, Instant.now().plus(properties.codeTtl()), 0));
        return new LinkStarted(PhoneHasher.mask(e164), properties.codeTtl().toSeconds());
    }

    // Time complexity: O(log n). We do O(1) map and code checks, then two indexed lookups and one save.
    // Space complexity: O(1). We only hold one Pending record and one account object.
    /** Step 2: check the code and store the hash. */
    @Transactional
    public void confirmLink(String username, String code) {
        // Map lookups, removes and the time comparisons are all O(1).
        Pending p = pending.get(username);
        if (p == null || Instant.now().isAfter(p.expiresAt())) {
            pending.remove(username);
            throw new IllegalArgumentException("The code has expired, request a new one");
        }
        if (p.attempts() >= MAX_CODE_ATTEMPTS) {
            pending.remove(username);
            throw new TooManyRequestsException("Too many wrong codes, request a new one");
        }
        // Code check with the SMS provider: O(1) for us. withAttempt() and put are O(1).
        if (!sender.check(p.requestId(), code)) {
            pending.put(username, p.withAttempt());
            throw new IllegalArgumentException("Wrong code");
        }
        pending.remove(username);

        // Indexed search on phone_hash: O(log n). The filter is O(1).
        if (users.findByPhoneHash(p.phoneHash()).filter(a -> !a.getUsername().equals(username)).isPresent()) {
            throw new PhoneTakenException();
        }
        // Search on the username index: O(log n).
        UserAccount account = users.findByUsername(username).orElseThrow();
        account.setPhoneHash(p.phoneHash());
        // Updating one row and its index: O(log n).
        users.save(account);
    }

    // Time complexity: O(log n). One indexed lookup and one update.
    // Space complexity: O(1). We only hold one account object.
    @Transactional
    public void unlink(String username) {
        UserAccount account = users.findByUsername(username).orElseThrow(); // O(log n)
        account.setPhoneHash(null);
        users.save(account); // O(log n)
    }

    // Time complexity: O(K log n), where K is the number of phone keys we receive (at most 100).
    // We hash K keys, then the database does K index lookups through one IN query.
    // Space complexity: O(K). We hold a map of K entries and a list of at most K matches.
    /**
     * Which of these phone keys belong to Nchat users. The client computes the keys with
     * {@link PhoneHasher#phoneKey(String)}, so the numbers of its contacts never reach the server.
     */
    @Transactional(readOnly = true)
    public List<Match> lookup(String username, List<String> phoneKeys) {
        // These checks are O(1). size() just returns a stored number.
        if (phoneKeys == null || phoneKeys.isEmpty()) {
            return List.of();
        }
        if (phoneKeys.size() > MAX_LOOKUP_KEYS) {
            throw new IllegalArgumentException("At most " + MAX_LOOKUP_KEYS + " numbers per request");
        }
        // Rate check on a deque of at most 30 timestamps: O(1).
        checkRate(lookupLog, username, MAX_LOOKUPS_PER_HOUR, Duration.ZERO, "Too many lookups, try again later");

        Map<String, String> keyByHash = new LinkedHashMap<>();
        // We go through the K keys once. For each key, isPhoneKey and hash read a short string,
        // and the map put is O(1) on average, so this loop is O(K).
        for (String key : phoneKeys) {
            if (!PhoneHasher.isPhoneKey(key)) {
                throw new IllegalArgumentException("Invalid phone key");
            }
            keyByHash.put(hash(key), key);
        }
        List<Match> matches = new ArrayList<>();
        // findByPhoneHashIn does up to K searches on the phone_hash index, each O(log n),
        // so the query costs O(K log n). It returns at most K accounts.
        for (UserAccount account : users.findByPhoneHashIn(keyByHash.keySet())) {
            // For each returned account, the comparison and map get are O(1), so this loop is O(K).
            if (!account.getUsername().equals(username)) {
                matches.add(new Match(keyByHash.get(account.getPhoneHash()), account.getUsername(), account.getDisplayName()));
            }
        }
        return matches;
    }

    // Time complexity: O(1). HMAC-SHA256 reads the key and the pepper once, and both have a limited length.
    // Space complexity: O(1). The digest is always 32 bytes, which is 64 hex characters.
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

    // Time complexity: O(1). It only calls checkRate.
    // Space complexity: O(1).
    private void checkCodeRate(String username) {
        checkRate(codeLog, username, MAX_CODES_PER_HOUR, MIN_CODE_INTERVAL,
            "Too many codes requested, try again later");
    }

    // Time complexity: O(w), where w is the number of timestamps stored for this user.
    // We reject the request when w reaches perHour (5 or 30), so w is a small constant and this is O(1).
    // Space complexity: O(1) per call. Over time, each user's deque holds at most perHour timestamps.
    /** Sliding one-hour window per user, plus an optional minimum gap between requests. */
    private static void checkRate(Map<String, Deque<Instant>> log, String username, int perHour,
                                  Duration minGap, String message) {
        Instant now = Instant.now();
        // Finding or creating the deque in the hash map: O(1) on average.
        Deque<Instant> times = log.computeIfAbsent(username, u -> new ArrayDeque<>());
        synchronized (times) {
            // We remove old timestamps from the front. Each timestamp is added once and removed
            // once, so this loop costs O(w) at worst and O(1) amortized per request.
            while (!times.isEmpty() && times.peekFirst().isBefore(now.minus(Duration.ofHours(1)))) {
                times.pollFirst();
            }
            // size(), peekLast() and addLast() are all O(1) on an ArrayDeque.
            if (times.size() >= perHour) {
                throw new TooManyRequestsException(message);
            }
            if (!times.isEmpty() && times.peekLast().plus(minGap).isAfter(now)) {
                throw new TooManyRequestsException("Please wait a minute before requesting another code");
            }
            times.addLast(now);
        }
    }

    /*
     * HOW WE DERIVED THE TIME COMPLEXITY
     *
     * Let n be the number of users in the database, K the number of phone keys sent to
     * lookup() (at most 100), and w the number of timestamps in one user's rate-limit deque
     * (at most 5 or 30). Phone numbers and keys have a limited length, so reading them is O(1).
     *
     * checkRate() and checkCodeRate():
     * The deque never holds more than perHour entries, because we throw before adding more.
     * The cleanup loop removes each timestamp once, and all other deque operations are O(1).
     * The map access is O(1) on average, so the whole check is O(1).
     *
     * hash():
     * HMAC-SHA256 makes one pass over a short input, so it is O(1).
     *
     * startLink():
     * We hash the number (O(1)), search the phone_hash index (O(log n)), check the rate (O(1)),
     * call the SMS provider (O(1) for us) and put one map entry (O(1)).
     * The total is O(log n).
     *
     * confirmLink():
     * The map operations and checks are O(1). Then findByPhoneHash, findByUsername and save
     * each take O(log n). That is 3 * O(log n), which simplifies to O(log n).
     *
     * unlink():
     * One lookup and one save, so 2 * O(log n) = O(log n).
     *
     * lookup():
     * The first checks and the rate check are O(1). The first loop hashes K keys, so it is O(K).
     * The IN query searches the index once for each key, which is K * O(log n) = O(K log n).
     * The second loop goes over at most K accounts with O(1) work each, so it is O(K).
     * We add them: O(K + K log n + K). The largest term wins, so the result is O(K log n).
     * Because K is capped at 100, it is a constant, so in practice this is also O(log n).
     *
     * Assumption: phone_hash and username must be indexed. Without an index, every search
     * would scan the whole table and cost O(n), and lookup() would cost O(K n).
     *
     * HOW WE DERIVED THE SPACE COMPLEXITY
     *
     * We first counted the extra memory each method creates while it runs.
     *
     * startLink(), confirmLink(), unlink(), hash(), checkRate() and withAttempt():
     * They create a fixed number of short strings and objects, so each is O(1).
     *
     * lookup():
     * We keep a LinkedHashMap with up to K entries and a list with up to K matches.
     * That is 2 * O(K), which simplifies to O(K).
     *
     * Memory that stays between calls:
     * The pending, codeLog and lookupLog maps live as long as the application. Each user
     * has at most one Pending record and two deques of at most 5 and 30 timestamps. If U is
     * the number of users who requested a code or did a lookup, this memory is O(U).
     * Note: we never remove a user's entry from codeLog or lookupLog, and a Pending record is
     * only removed when the user confirms or tries again, so this memory grows with U.
     * A scheduled cleanup would keep it smaller, but the code stays unchanged here.
     */
}
