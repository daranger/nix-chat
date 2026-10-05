package uz.nchat.common.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256 in GCM mode: encrypts and authenticates the data, so any tampering is detected on decryption.
 * Output layout: [12-byte random IV][ciphertext + 16-byte tag].
 */
public class AesGcmEncryptor implements Encryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_BITS = 256;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey key;

    public AesGcmEncryptor(SecretKey key) {
        this.key = key;
    }

    /** Creates an encryptor from a Base64-encoded 256-bit key. */
    public static AesGcmEncryptor fromBase64Key(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != KEY_BITS / 8) {
            throw new IllegalArgumentException("Key must be " + KEY_BITS + " bits");
        }
        return new AesGcmEncryptor(new SecretKeySpec(keyBytes, "AES"));
    }

    /** Generates a new random 256-bit key, encoded as Base64. */
    public static String generateBase64Key() throws GeneralSecurityException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(KEY_BITS, RANDOM);
        return Base64.getEncoder().encodeToString(generator.generateKey().getEncoded());
    }

    @Override
    public byte[] encrypt(byte[] plaintext) throws GeneralSecurityException {
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        byte[] ciphertext = cipher.doFinal(plaintext);

        byte[] result = new byte[IV_BYTES + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, IV_BYTES);
        System.arraycopy(ciphertext, 0, result, IV_BYTES, ciphertext.length);
        return result;
    }

    @Override
    public byte[] decrypt(byte[] data) throws GeneralSecurityException {
        if (data.length <= IV_BYTES) {
            throw new GeneralSecurityException("Encrypted data is too short");
        }
        byte[] iv = Arrays.copyOfRange(data, 0, IV_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(data, IV_BYTES, data.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        return cipher.doFinal(ciphertext); // throws AEADBadTagException if the data was modified
    }

    @Override
    public String algorithmName() {
        return "AES-256-GCM";
    }
}
