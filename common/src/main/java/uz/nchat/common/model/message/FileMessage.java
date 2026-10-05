package uz.nchat.common.model.message;

import uz.nchat.common.model.User;

import java.time.Instant;
import java.util.Locale;

/**
 * A message carrying a file (photo, document, voice note…).
 */
public class FileMessage extends Message {

    /** Max file size: 50 MB. */
    public static final long MAX_SIZE_BYTES = 50L * 1024 * 1024;

    private static final String[] SIZE_UNITS = {"B", "KB", "MB", "GB"};
    private static final String[] IMAGE_EXTENSIONS = {"png", "jpg", "jpeg", "gif", "webp"};

    private final String fileName;
    private final long sizeBytes;

    public FileMessage(String id, String chatId, User sender, Instant sentAt, String fileName, long sizeBytes) {
        super(id, chatId, sender, sentAt);
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }
        if (sizeBytes < 0 || sizeBytes > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size must be between 0 and " + humanSize(MAX_SIZE_BYTES));
        }
        this.fileName = fileName;
        this.sizeBytes = sizeBytes;
    }

    public FileMessage(String chatId, User sender, String fileName, long sizeBytes) {
        this(null, chatId, sender, null, fileName, sizeBytes);
    }

    /** Converts bytes to a readable size, e.g. 1536 -> "1.5 KB". */
    public static String humanSize(long bytes) {
        double size = bytes;
        int unit = 0;
        while (size >= 1024 && unit < SIZE_UNITS.length - 1) {
            size /= 1024;
            unit++;
        }
        return unit == 0
                ? bytes + " B"
                : String.format(Locale.ROOT, "%.1f %s", size, SIZE_UNITS[unit]);
    }

    public boolean isImage() {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        for (String imageExtension : IMAGE_EXTENSIONS) {
            if (imageExtension.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    @Override
    public String preview() {
        String icon = isImage() ? "🖼" : "📎";
        return icon + " " + fileName + " (" + humanSize(sizeBytes) + ")";
    }

    @Override
    public String getType() {
        return "FILE";
    }
}
