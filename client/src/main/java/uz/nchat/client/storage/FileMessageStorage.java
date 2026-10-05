package uz.nchat.client.storage;

import uz.nchat.common.crypto.Encryptor;
import uz.nchat.common.model.User;
import uz.nchat.common.model.message.FileMessage;
import uz.nchat.common.model.message.Message;
import uz.nchat.common.model.message.SystemMessage;
import uz.nchat.common.model.message.TextMessage;
import uz.nchat.common.model.message.TrackMessage;
import uz.nchat.common.music.Track;
import uz.nchat.common.storage.MessageStorage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * File 3 of 3: local message cache, stored in {@code messages-cache.tsv}.
 * Entity: {@link Message}. Message contents are encrypted with the {@link Encryptor} from the settings.
 * <p>
 * Line format (tab-separated): {@code type, id, chatId, sender, sentAt, payload[, fileSize | trackDuration]}
 */
public class FileMessageStorage implements MessageStorage {

    public static final String FILE_NAME = "messages-cache.tsv";
    private static final String SEPARATOR = "\t";
    private static final String NO_SENDER = "-";

    private final Path file;
    private final Encryptor encryptor;
    private final List<Message> messages = new ArrayList<>();
    private int skippedLines;

    public FileMessageStorage(Path profileDir, Encryptor encryptor) {
        this.file = profileDir.resolve(FILE_NAME);
        this.encryptor = encryptor;
        load();
    }

    /**
     * Reads the whole cache file.
     * try-catch with multiple catches and finally.
     */
    private void load() {
        int lineNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                Message message = decodeLine(line, lineNumber);
                if (message != null) {
                    messages.add(message);
                }
            }
        } catch (NoSuchFileException e) {
            // First launch: there is no cache yet — that's fine
        } catch (IOException e) {
            System.err.println("Could not read the message cache: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Unexpected error in the message cache at line " + lineNumber + ": " + e);
        } finally {
            System.out.println("Message cache: loaded " + messages.size() + " message(s), skipped " + skippedLines
                    + " broken line(s), encryption: " + encryptor.algorithmName());
        }
    }

    private Message decodeLine(String line, int lineNumber) {
        String[] parts = line.split(SEPARATOR);
        if (parts.length < 6) {
            skippedLines++;
            return null;
        }
        try {
            String type = parts[0];
            String id = parts[1];
            String chatId = parts[2];
            User sender = NO_SENDER.equals(parts[3]) ? null : new User(parts[3]);
            Instant sentAt = Instant.parse(parts[4]);

            // try-catch #4: GeneralSecurityException — wrong key or a modified file
            String payload = encryptor.decryptText(parts[5]);

            return switch (type) {
                case "TEXT" -> new TextMessage(id, chatId, sender, sentAt, payload);
                case "FILE" -> new FileMessage(id, chatId, sender, sentAt, payload, Long.parseLong(parts[6]));
                case "SYSTEM" -> new SystemMessage(id, chatId, sentAt, payload);
                case "TRACK" -> {
                    String[] artistAndTitle = payload.split("\n", 2);
                    Track track = new Track(artistAndTitle[0], artistAndTitle[1], Integer.parseInt(parts[6]));
                    yield new TrackMessage(id, chatId, sender, sentAt, track);
                }
                default -> throw new IllegalArgumentException("Unknown message type " + type);
            };
        } catch (GeneralSecurityException e) {
            System.err.println("Line " + lineNumber + " cannot be decrypted (wrong key or tampered file)");
        } catch (DateTimeParseException | IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Line " + lineNumber + " is broken: " + e.getMessage());
        }
        skippedLines++;
        return null;
    }

    private String encodeLine(Message message) throws GeneralSecurityException {
        String sender = (message.getSender() == null) ? NO_SENDER : message.getSender().getUsername();
        String payload;
        String extra = "";
        // Each subclass stores a different payload
        if (message instanceof TextMessage text) {
            payload = text.getText();
        } else if (message instanceof FileMessage fileMessage) {
            payload = fileMessage.getFileName();
            extra = SEPARATOR + fileMessage.getSizeBytes();
        } else if (message instanceof SystemMessage system) {
            payload = system.getNotice();
        } else if (message instanceof TrackMessage trackMessage) {
            Track track = trackMessage.getTrack();
            payload = track.artist() + "\n" + track.title();
            extra = SEPARATOR + track.durationSeconds();
        } else {
            throw new IllegalArgumentException("Unsupported message " + message.getClass().getSimpleName());
        }
        return String.join(SEPARATOR, message.getType(), message.getId(), message.getChatId(), sender,
                message.getSentAt().toString(), encryptor.encryptText(payload)) + extra;
    }

    @Override
    public synchronized void save(Message message) {
        messages.add(message);
        try {
            String line = encodeLine(message);
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(line);
                writer.newLine();
            }
        } catch (GeneralSecurityException e) {
            System.err.println("Could not encrypt message " + message.getId() + ": " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Could not write the message cache: " + e.getMessage());
        }
    }

    @Override
    public synchronized List<Message> findByChat(String chatId) {
        List<Message> result = new ArrayList<>();
        for (Message message : messages) {
            if (message.getChatId().equals(chatId)) {
                result.add(message);
            }
        }
        return result;
    }

    @Override
    public synchronized int count() {
        return messages.size();
    }

    public int getSkippedLines() {
        return skippedLines;
    }
}
