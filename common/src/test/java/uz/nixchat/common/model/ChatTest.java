package uz.nixchat.common.model;

import org.junit.jupiter.api.Test;
import uz.nixchat.common.model.chat.Chat;
import uz.nixchat.common.model.chat.GroupChat;
import uz.nixchat.common.model.chat.PrivateChat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatTest {

    private final User alice = new User("alice", "Alice");
    private final User bob = new User("bob", "Bob");
    private final User carol = new User("carol");

    @Test
    void privateChatIdDoesNotDependOnOrder() {
        assertEquals(new PrivateChat(alice, bob).getId(), new PrivateChat(bob, alice).getId());
    }

    @Test
    void privateChatTitleIsThePartnerName() {
        Chat chat = new PrivateChat(alice, bob);
        assertEquals("Bob", chat.getTitle(alice));
        assertEquals("Alice", chat.getTitle(bob));
    }

    @Test
    void privateChatIsLimitedToTwoUsers() {
        Chat chat = new PrivateChat(alice, bob);
        assertThrows(IllegalStateException.class, () -> chat.addParticipant(carol));
    }

    @Test
    void blockedPrivateChatCannotBeWrittenTo() {
        PrivateChat chat = new PrivateChat(alice, bob);
        chat.setBlocked(true);
        assertFalse(chat.canWrite(alice));
    }

    @Test
    void groupRolesControlWhoCanPost() {
        GroupChat group = new GroupChat("g1", "Team", alice);
        group.addMember(alice, bob);
        group.addMember(alice, carol);
        group.promote(alice, bob);

        assertTrue(group.canWrite(carol));
        group.setReadOnly(true);
        assertFalse(group.canWrite(carol));
        assertTrue(group.canWrite(bob));
    }

    @Test
    void membersCannotKickAnyone() {
        GroupChat group = new GroupChat("g1", "Team", alice);
        group.addMember(alice, bob);
        group.addMember(alice, carol);

        assertThrows(SecurityException.class, () -> group.kick(bob, carol));
        group.kick(alice, carol);
        assertFalse(group.isParticipant(carol));
    }

    @Test
    void canWriteIsPolymorphic() {
        Chat[] chats = {new PrivateChat(alice, bob), new GroupChat("g1", "Team", alice)};
        for (Chat chat : chats) {
            assertTrue(chat.canWrite(alice));
            assertFalse(chat.canWrite(carol));
        }
    }
}
