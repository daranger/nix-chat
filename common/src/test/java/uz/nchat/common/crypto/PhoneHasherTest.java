package uz.nchat.common.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneHasherTest {

    @Test
    void differentWritingsOfOneNumberAreTheSame() {
        assertEquals("+998901234567", PhoneHasher.normalize("+998 (90) 123-45-67"));
        assertEquals("+998901234567", PhoneHasher.normalize("90 123 45 67"));
        assertEquals("+998901234567", PhoneHasher.normalize("00998901234567"));
        assertEquals(PhoneHasher.phoneKey("+998 90 123 45 67"), PhoneHasher.phoneKey("901234567"));
    }

    @Test
    void keyIsHexAndDoesNotContainTheNumber() {
        String key = PhoneHasher.phoneKey("+998901234567");
        assertTrue(PhoneHasher.isPhoneKey(key));
        assertFalse(key.contains("901234567"));
        assertNotEquals(key, PhoneHasher.phoneKey("+998901234568"));
    }

    @Test
    void garbageIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PhoneHasher.normalize("hello"));
        assertThrows(IllegalArgumentException.class, () -> PhoneHasher.normalize("+12"));
        assertThrows(IllegalArgumentException.class, () -> PhoneHasher.normalize(null));
    }

    @Test
    void maskShowsOnlyTheLastDigits() {
        assertEquals("+998 •• ••• 45 67", PhoneHasher.mask("+998901234567"));
    }
}
