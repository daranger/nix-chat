package uz.nixchat.common.model.message;

import uz.nixchat.common.model.User;
import uz.nixchat.common.music.Track;

import java.time.Instant;
import java.util.Objects;

/**
 * A music track shared in a chat (Music department).
 */
public class TrackMessage extends Message {

    private final Track track;

    public TrackMessage(String id, String chatId, User sender, Instant sentAt, Track track) {
        super(id, chatId, sender, sentAt);
        this.track = Objects.requireNonNull(track, "track");
    }

    public TrackMessage(String chatId, User sender, Track track) {
        this(null, chatId, sender, null, track);
    }

    public Track getTrack() {
        return track;
    }

    @Override
    public String preview() {
        return "♪ " + track;
    }

    @Override
    public String getType() {
        return "TRACK";
    }
}
