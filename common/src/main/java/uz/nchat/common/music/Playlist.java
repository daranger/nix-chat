package uz.nchat.common.music;

import uz.nchat.common.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * An ordered list of tracks. The owner and the collaborators they invite can edit it.
 */
public class Playlist {

    public static final int MAX_TRACKS = 500;

    private final String name;
    private final User owner;
    private final List<User> collaborators = new ArrayList<>();
    private final List<Track> tracks = new ArrayList<>();

    public Playlist(String name, User owner) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Playlist name is required");
        }
        this.name = name.strip();
        this.owner = owner;
    }

    public void invite(User collaborator) {
        if (!collaborator.equals(owner) && !collaborators.contains(collaborator)) {
            collaborators.add(collaborator);
        }
    }

    public boolean canEdit(User user) {
        return owner.equals(user) || collaborators.contains(user);
    }

    public void add(User editor, Track track) {
        checkCanEdit(editor);
        if (tracks.size() >= MAX_TRACKS) {
            throw new IllegalStateException("A playlist holds at most " + MAX_TRACKS + " tracks");
        }
        tracks.add(track);
    }

    public boolean remove(User editor, Track track) {
        checkCanEdit(editor);
        return tracks.remove(track);
    }

    /** Returns a new, shuffled play order; the playlist itself keeps its order. */
    public List<Track> shuffled(long seed) {
        List<Track> copy = new ArrayList<>(tracks);
        Collections.shuffle(copy, new Random(seed));
        return copy;
    }

    public int totalDurationSeconds() {
        int total = 0;
        for (Track track : tracks) {
            total += track.durationSeconds();
        }
        return total;
    }

    private void checkCanEdit(User user) {
        if (!canEdit(user)) {
            throw new SecurityException(user.getUsername() + " cannot edit playlist '" + name + "'");
        }
    }

    public String getName() {
        return name;
    }

    public User getOwner() {
        return owner;
    }

    public List<Track> getTracks() {
        return Collections.unmodifiableList(tracks);
    }

    @Override
    public String toString() {
        return name + " · " + tracks.size() + " tracks · " + Track.formatDuration(totalDurationSeconds());
    }
}
