package uz.nchat.common.department;

import java.util.List;

public class MusicDepartment extends Department {

    private static final List<Position> POSITIONS = List.of(
            new Position("Music Curator", "Picks featured tracks and playlists"),
            new Position("Audio Developer", "Track sharing and the mini player"),
            new Position("Licensing Manager", "Makes sure shared music may be shared"),
            new Position("Playlist Editor", "Builds and maintains shared playlists"));

    @Override
    public String getName() {
        return "Music";
    }

    @Override
    public String getMission() {
        return "Sharing tracks in chats and building playlists together.";
    }

    @Override
    public List<Position> getPositions() {
        return POSITIONS;
    }

    @Override
    public String getCodePackage() {
        return "uz.nchat.common.music";
    }
}
