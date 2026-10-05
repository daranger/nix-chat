package uz.nchat.client.storage;

import uz.nchat.common.Protocol;
import uz.nchat.common.crypto.AesGcmEncryptor;
import uz.nchat.common.crypto.Encryptor;
import uz.nchat.common.crypto.NoOpEncryptor;
import uz.nchat.common.model.User;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

/**
 * File 1 of 3: user settings, stored in {@code settings.properties}.
 * Entity: the current {@link User} plus connection, login and encryption preferences.
 */
public class AppSettings {

    public static final String FILE_NAME = "settings.properties";

    private static final String KEY_USERNAME = "user.username";
    private static final String KEY_DISPLAY_NAME = "user.displayName";
    private static final String KEY_HOST = "server.host";
    private static final String KEY_PORT = "server.port";
    private static final String KEY_CACHE_ENCRYPTED = "cache.encrypted";
    private static final String KEY_CACHE_KEY = "cache.key";
    private static final String KEY_AUTH_TOKEN = "auth.token";

    private final Path file;
    private final Properties properties = new Properties();

    private AppSettings(Path file) {
        this.file = file;
    }

    /** Loads settings from the profile folder; missing values are filled with defaults and saved. */
    public static AppSettings load(Path profileDir) {
        AppSettings settings = new AppSettings(profileDir.resolve(FILE_NAME));

        // try-catch #1: IOException — the settings file may be missing or unreadable
        if (Files.exists(settings.file)) {
            try (Reader reader = Files.newBufferedReader(settings.file, StandardCharsets.UTF_8)) {
                settings.properties.load(reader);
            } catch (IOException e) {
                System.err.println("Could not read " + settings.file + ", using defaults: " + e.getMessage());
            }
        }

        boolean changed = settings.fillDefaults();
        if (changed) {
            settings.save();
        }
        return settings;
    }

    private boolean fillDefaults() {
        boolean changed = false;
        String username = properties.getProperty(KEY_USERNAME);
        if (!User.isValidUsername(username)) {
            properties.setProperty(KEY_USERNAME, "guest-" + ThreadLocalRandom.current().nextInt(1000, 10000));
            changed = true;
        }
        if (properties.getProperty(KEY_HOST) == null) {
            properties.setProperty(KEY_HOST, "localhost");
            changed = true;
        }
        if (properties.getProperty(KEY_PORT) == null) {
            properties.setProperty(KEY_PORT, String.valueOf(Protocol.DEFAULT_PORT));
            changed = true;
        }
        if (properties.getProperty(KEY_CACHE_ENCRYPTED) == null) {
            properties.setProperty(KEY_CACHE_ENCRYPTED, "true");
            changed = true;
        }
        if (isCacheEncrypted() && properties.getProperty(KEY_CACHE_KEY) == null) {
            try {
                properties.setProperty(KEY_CACHE_KEY, AesGcmEncryptor.generateBase64Key());
                changed = true;
            } catch (GeneralSecurityException e) {
                System.err.println("AES is not available, the cache will be stored unencrypted: " + e.getMessage());
                properties.setProperty(KEY_CACHE_ENCRYPTED, "false");
                changed = true;
            }
        }
        return changed;
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                properties.store(writer, "Nchat client settings");
            }
        } catch (IOException e) {
            System.err.println("Could not save " + file + ": " + e.getMessage());
        }
    }

    public User getCurrentUser() {
        return new User(properties.getProperty(KEY_USERNAME), properties.getProperty(KEY_DISPLAY_NAME));
    }

    /** Remembers who is signed in on this profile (after a successful login). */
    public void setCurrentUser(String username, String displayName) {
        properties.setProperty(KEY_USERNAME, username);
        if (displayName == null || displayName.isBlank()) {
            properties.remove(KEY_DISPLAY_NAME);
        } else {
            properties.setProperty(KEY_DISPLAY_NAME, displayName);
        }
    }

    /** The saved login token, or null if the user is signed out. */
    public String getAuthToken() {
        String token = properties.getProperty(KEY_AUTH_TOKEN);
        return (token == null || token.isBlank()) ? null : token;
    }

    public void setAuthToken(String token) {
        if (token == null) {
            properties.remove(KEY_AUTH_TOKEN);
        } else {
            properties.setProperty(KEY_AUTH_TOKEN, token);
        }
    }

    public String getServerHost() {
        return properties.getProperty(KEY_HOST, "localhost");
    }

    public int getServerPort() {
        String value = properties.getProperty(KEY_PORT);
        // try-catch #2: NumberFormatException — the port in the file may have been edited by hand
        try {
            int port = Integer.parseInt(value.strip());
            return (port > 0 && port <= 65_535) ? port : Protocol.DEFAULT_PORT;
        } catch (NumberFormatException e) {
            System.err.println("Invalid port '" + value + "' in settings, using " + Protocol.DEFAULT_PORT);
            return Protocol.DEFAULT_PORT;
        }
    }

    public void setServer(String host, int port) {
        properties.setProperty(KEY_HOST, host);
        properties.setProperty(KEY_PORT, String.valueOf(port));
    }

    public boolean isCacheEncrypted() {
        return Boolean.parseBoolean(properties.getProperty(KEY_CACHE_ENCRYPTED, "true"));
    }

    /**
     * Chooses the encryptor for the local message cache.
     * Polymorphism: the caller works with the {@link Encryptor} interface and does not know which one it got.
     */
    public Encryptor createCacheEncryptor() {
        String key = properties.getProperty(KEY_CACHE_KEY);
        if (isCacheEncrypted() && key != null) {
            return AesGcmEncryptor.fromBase64Key(key);
        }
        return new NoOpEncryptor();
    }

    public Path getFile() {
        return file;
    }
}
