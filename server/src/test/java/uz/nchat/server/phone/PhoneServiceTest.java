package uz.nchat.server.phone;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uz.nchat.common.crypto.PhoneHasher;
import uz.nchat.server.auth.AuthService;
import uz.nchat.server.auth.UserAccountRepository;
import uz.nchat.server.phone.PhoneExceptions.PhoneTakenException;
import uz.nchat.server.phone.PhoneService.Match;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PhoneServiceTest {

    @Autowired
    private PhoneService phoneService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private VerificationSender sender;

    private String lastCode() {
        return ((DevConsoleSender) sender).lastCode();
    }

    @Test
    void linkStoresOnlyAHashAndFriendsCanFindTheUser() {
        authService.register("phone_alice", "Alice", "password-123");
        authService.register("phone_bob", "Bob", "password-123");

        phoneService.startLink("phone_alice", "+998 90 111 22 33");
        phoneService.confirmLink("phone_alice", lastCode());

        String stored = users.findByUsername("phone_alice").orElseThrow().getPhoneHash();
        assertEquals(64, stored.length());
        assertFalse(stored.contains("901112233"));
        assertFalse(stored.equals(PhoneHasher.phoneKey("+998901112233")), "stored value must be keyed with the pepper");

        // Bob has Alice's number in his contacts: his client sends only the PBKDF2 key
        List<Match> matches = phoneService.lookup("phone_bob",
                List.of(PhoneHasher.phoneKey("90 111 22 33"), PhoneHasher.phoneKey("+998900000000")));
        assertEquals(1, matches.size());
        assertEquals("phone_alice", matches.get(0).username());
    }

    @Test
    void wrongCodeIsRejectedAndNumberCannotBeTakenTwice() {
        authService.register("phone_carol", "Carol", "password-123");
        authService.register("phone_dave", "Dave", "password-123");

        phoneService.startLink("phone_carol", "+998 91 444 55 66");
        assertThrows(IllegalArgumentException.class, () -> phoneService.confirmLink("phone_carol", "000000x"));
        phoneService.confirmLink("phone_carol", lastCode());

        assertThrows(PhoneTakenException.class, () -> phoneService.startLink("phone_dave", "+998914445566"));
    }

    @Test
    void unlinkRemovesTheHash() {
        authService.register("phone_erin", "Erin", "password-123");
        phoneService.startLink("phone_erin", "+998 93 777 88 99");
        phoneService.confirmLink("phone_erin", lastCode());

        phoneService.unlink("phone_erin");
        assertNull(users.findByUsername("phone_erin").orElseThrow().getPhoneHash());
        assertTrue(phoneService.lookup("phone_dave_x", List.of(PhoneHasher.phoneKey("+998937778899"))).isEmpty());
    }
}
