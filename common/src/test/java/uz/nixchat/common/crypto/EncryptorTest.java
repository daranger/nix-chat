package uz.nixchat.common.crypto;

import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EncryptorTest {

    @Test
    void everyEncryptorRoundTrips() throws GeneralSecurityException {
        Encryptor[] encryptors = {
                AesGcmEncryptor.fromBase64Key(AesGcmEncryptor.generateBase64Key()),
                new NoOpEncryptor()
        };
        for (Encryptor encryptor : encryptors) {
            String encrypted = encryptor.encryptText("Привет, Nchat!");
            assertEquals("Привет, Nchat!", encryptor.decryptText(encrypted), encryptor.algorithmName());
        }
    }

    @Test
    void sameTextEncryptsDifferentlyEachTime() throws GeneralSecurityException {
        Encryptor aes = AesGcmEncryptor.fromBase64Key(AesGcmEncryptor.generateBase64Key());
        assertNotEquals(aes.encryptText("hi"), aes.encryptText("hi"));
    }

    @Test
    void wrongKeyIsDetected() throws GeneralSecurityException {
        Encryptor first = AesGcmEncryptor.fromBase64Key(AesGcmEncryptor.generateBase64Key());
        Encryptor second = AesGcmEncryptor.fromBase64Key(AesGcmEncryptor.generateBase64Key());
        String encrypted = first.encryptText("secret");
        assertThrows(GeneralSecurityException.class, () -> second.decryptText(encrypted));
    }

    @Test
    void tamperingIsDetected() throws GeneralSecurityException {
        Encryptor aes = AesGcmEncryptor.fromBase64Key(AesGcmEncryptor.generateBase64Key());
        byte[] data = Base64.getDecoder().decode(aes.encryptText("secret"));
        data[data.length - 1] ^= 1;
        assertThrows(GeneralSecurityException.class, () -> aes.decrypt(data));
    }
}
