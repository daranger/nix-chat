package uz.nixchat.common.music;

/**
 * A music track that can be shared in a chat or added to a playlist.
 */
public record Track(String artist, String title, int durationSeconds) {

    public static final int MAX_DURATION_SECONDS = 60 * 60;

    public Track {
        if (artist == null || artist.isBlank() || title == null || title.isBlank()) {
            throw new IllegalArgumentException("Artist and title are required");
        }
        if (durationSeconds <= 0 || durationSeconds > MAX_DURATION_SECONDS) {
            throw new IllegalArgumentException("Duration must be between 1 second and 1 hour");
        }
    }

    /** 225 seconds -> "3:45". */
    public static String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int rest = seconds % 60;
        return hours > 0
                ? String.format("%d:%02d:%02d", hours, minutes, rest)
                : String.format("%d:%02d", minutes, rest);
    }

    public String formattedDuration() {
        return formatDuration(durationSeconds);
    }

    @Override
    public String toString() {
        return artist + " — " + title + " (" + formattedDuration() + ")";
    }
}
