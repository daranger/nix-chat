package uz.nixchat.client.storage;

import uz.nixchat.common.model.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * File 2 of 3: contacts, stored in {@code contacts.csv}.
 * Entity: {@link Contact}. One line per contact: {@code username,lastSeen,displayName}.
 */
public class ContactStore {

    public static final String FILE_NAME = "contacts.csv";
    private static final String HEADER = "username,lastSeen,displayName";

    private final Path file;
    private final Map<String, Contact> contacts = new LinkedHashMap<>();

    public ContactStore(Path profileDir) {
        this.file = profileDir.resolve(FILE_NAME);
        load();
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not read " + file + ": " + e.getMessage());
            return;
        }
        for (int i = 1; i < lines.size(); i++) { // line 0 is the header
            String[] parts = lines.get(i).split(",", 3);
            if (parts.length < 3) {
                continue;
            }
            try {
                User user = new User(parts[0], parts[2]);
                contacts.put(user.getUsername(), new Contact(user, Instant.parse(parts[1])));
            } catch (DateTimeParseException | IllegalArgumentException e) {
                System.err.println("Skipping broken line " + (i + 1) + " in " + FILE_NAME + ": " + e.getMessage());
            }
        }
    }

    /** Adds the user or updates their "last seen" time. Returns true if the contact is new. */
    public synchronized boolean touch(User user, Instant seenAt) {
        boolean isNew = !contacts.containsKey(user.getUsername());
        contacts.put(user.getUsername(), new Contact(user, seenAt));
        save();
        return isNew;
    }

    /** Contacts sorted by last activity, most recent first. */
    public synchronized List<Contact> getAll() {
        List<Contact> sorted = new ArrayList<>(contacts.values());
        sorted.sort(Comparator.comparing(Contact::lastSeen).reversed());
        return sorted;
    }

    private void save() {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Contact contact : contacts.values()) {
            String displayName = contact.user().getDisplayName().replace(",", " ");
            lines.add(contact.user().getUsername() + "," + contact.lastSeen() + "," + displayName);
        }
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not save " + file + ": " + e.getMessage());
        }
    }
}
