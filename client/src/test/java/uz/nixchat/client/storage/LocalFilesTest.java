package uz.nixchat.client.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uz.nixchat.common.crypto.Encryptor;
import uz.nixchat.common.model.User;
import uz.nixchat.common.model.message.FileMessage;
import uz.nixchat.common.model.message.Message;
import uz.nixchat.common.model.message.SystemMessage;
import uz.nixchat.common.model.message.TextMessage;
import uz.nixchat.common.model.message.TrackMessage;
import uz.nixchat.common.music.Track;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFilesTest {

    @TempDir
    Path dir;

    @Test
    void settingsAreCreatedOnFirstLaunchAndReused() {
        AppSettings first = AppSettings.load(dir);
        AppSettings second = AppSettings.load(dir);

        assertTrue(Files.exists(dir.resolve(AppSettings.FILE_NAME)));
        assertEquals(first.getCurrentUser(), second.getCurrentUser());
        assertEquals(8080, second.getServerPort());
    }

    @Test
    void brokenPortFallsBackToDefault() throws IOException {
        Files.writeString(dir.resolve(AppSettings.FILE_NAME), "server.port=abc\n");
        assertEquals(8080, AppSettings.load(dir).getServerPort());
    }

    @Test
    void messagesSurviveRestartAndAreEncryptedOnDisk() throws IOException {
        Encryptor encryptor = AppSettings.load(dir).createCacheEncryptor();
        User alice = new User("alice");

        FileMessageStorage storage = new FileMessageStorage(dir, encryptor);
        storage.save(new TextMessage("general", alice, "top secret"));
        storage.save(new FileMessage("general", alice, "notes.pdf", 2048));
        storage.save(new SystemMessage("general", "bob joined"));
        storage.save(new TrackMessage("general", alice, new Track("Artist A", "Song 1", 225)));

        String onDisk = Files.readString(dir.resolve(FileMessageStorage.FILE_NAME));
        assertFalse(onDisk.contains("top secret"));

        List<Message> reloaded = new FileMessageStorage(dir, encryptor).findByChat("general");
        assertEquals(4, reloaded.size());
        assertInstanceOf(TextMessage.class, reloaded.get(0));
        assertInstanceOf(FileMessage.class, reloaded.get(1));
        assertInstanceOf(SystemMessage.class, reloaded.get(2));
        assertEquals("♪ Artist A — Song 1 (3:45)", reloaded.get(3).preview());
        assertEquals("top secret", ((TextMessage) reloaded.get(0)).getText());
    }

    @Test
    void brokenCacheLinesAreSkipped() throws IOException {
        Encryptor encryptor = AppSettings.load(dir).createCacheEncryptor();
        new FileMessageStorage(dir, encryptor).save(new TextMessage("general", new User("alice"), "ok"));
        Files.writeString(dir.resolve(FileMessageStorage.FILE_NAME), "garbage line\n",
                StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        FileMessageStorage reloaded = new FileMessageStorage(dir, encryptor);
        assertEquals(1, reloaded.count());
        assertEquals(1, reloaded.getSkippedLines());
    }

    @Test
    void contactsAreSortedByLastSeen() {
        ContactStore store = new ContactStore(dir);
        store.touch(new User("bob"), Instant.parse("2026-10-01T10:00:00Z"));
        store.touch(new User("carol"), Instant.parse("2026-10-02T10:00:00Z"));

        List<Contact> contacts = new ContactStore(dir).getAll();
        assertEquals("carol", contacts.get(0).user().getUsername());
        assertEquals(2, contacts.size());
    }
}
