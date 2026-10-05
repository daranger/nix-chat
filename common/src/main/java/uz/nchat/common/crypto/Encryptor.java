package uz.nchat.common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * Encrypts and decrypts message payloads.
 * Implementations: {@link AesGcmEncryptor} (real encryption) and {@link NoOpEncryptor} (plain text, for debugging).
 */
public interface Encryptor {

    byte[] encrypt(byte[] plaintext) throws GeneralSecurityException;

    byte[] decrypt(byte[] ciphertext) throws GeneralSecurityException;

    /** Human-readable name of the algorithm, shown in the settings screen. */
    String algorithmName();

    /** Encrypts text and returns it as Base64, ready to be stored in a text file or sent over the network. */
    default String encryptText(String text) throws GeneralSecurityException {
        byte[] encrypted = encrypt(text.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /** Reverse of {@link #encryptText(String)}. */
    default String decryptText(String base64) throws GeneralSecurityException {
        byte[] decrypted = decrypt(Base64.getDecoder().decode(base64));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
