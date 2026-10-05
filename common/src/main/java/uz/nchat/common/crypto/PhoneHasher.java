package uz.nchat.common.crypto;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Turns a phone number into a "phone key" without ever storing the number.
 * <p>
 * Step 1 (here, shared by client and server): normalize to E.164 and run PBKDF2 with many iterations.
 * A plain SHA-256 of a phone number is useless as protection: there are only about a billion numbers per country,
 * and a GPU hashes them all in seconds. PBKDF2 makes every guess expensive.
 * <p>
 * Step 2 (server only): HMAC the phone key with a secret pepper kept outside the database,
 * so a leaked database alone cannot be brute-forced at all.
 */
public final class PhoneHasher {

    /** Fixed application salt: everyone's key must be comparable, so the salt cannot be per-user. */
    private static final byte[] SALT = "nixchat-phone-v1".getBytes(StandardCharsets.UTF_8);
    private static final int ITERATIONS = 200_000;
    private static final int KEY_BITS = 256;
    private static final String DEFAULT_COUNTRY_CODE = "998"; // Uzbekistan

    private PhoneHasher() {
    }

    /**
     * Normalizes a phone number to E.164, e.g. {@code "90 123-45-67"} → {@code "+998901234567"}.
     *
     * @throws IllegalArgumentException if it does not look like a phone number
     */
    public static String normalize(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Phone number is required");
        }
        StringBuilder digits = new StringBuilder();
        boolean plus = false;
        for (char c : input.strip().toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (c == '+' && digits.isEmpty()) {
                plus = true;
            } else if (c != ' ' && c != '-' && c != '(' && c != ')') {
                throw new IllegalArgumentException("Phone number may contain only digits, spaces, -, ( ) and a leading +");
            }
        }
        String number = digits.toString();
        if (!plus) {
            if (number.length() == 9) {
                number = DEFAULT_COUNTRY_CODE + number; // local Uzbek format: 90 123 45 67
            } else if (number.startsWith("00")) {
                number = number.substring(2);
            }
        }
        if (number.length() < 8 || number.length() > 15 || number.charAt(0) == '0') {
            throw new IllegalArgumentException("Enter the number in international format, e.g. +998 90 123 45 67");
        }
        return "+" + number;
    }

    /** PBKDF2-HMAC-SHA256 of the normalized number, as 64 hex characters. Takes about a tenth of a second. */
    public static String phoneKey(String phoneNumber) {
        String e164 = normalize(phoneNumber);
        try {
            PBEKeySpec spec = new PBEKeySpec(e164.toCharArray(), SALT, ITERATIONS, KEY_BITS);
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            spec.clearPassword();
            return HexFormat.of().formatHex(key);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available", e);
        }
    }

    /** True for a well-formed phone key (64 lowercase hex characters). */
    public static boolean isPhoneKey(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    /** "+998901234567" → "+998 •• ••• 45 67", for showing which number is linked without revealing it. */
    public static String mask(String phoneNumber) {
        String e164 = normalize(phoneNumber);
        return e164.substring(0, 4) + " •• ••• " + e164.substring(e164.length() - 4, e164.length() - 2)
                + " " + e164.substring(e164.length() - 2);
    }
}
