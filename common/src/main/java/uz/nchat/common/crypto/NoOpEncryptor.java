package uz.nchat.common.crypto;

/**
 * Leaves data unchanged. Used when encryption is switched off in the settings (debugging, demos).
 */
public class NoOpEncryptor implements Encryptor {

    @Override
    public byte[] encrypt(byte[] plaintext) {
        return plaintext.clone();
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) {
        return ciphertext.clone();
    }

    @Override
    public String algorithmName() {
        return "None (plain text)";
    }
}
