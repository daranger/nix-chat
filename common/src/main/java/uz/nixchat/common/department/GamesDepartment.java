package uz.nixchat.common.department;

import java.util.List;

public class GamesDepartment extends Department {

    private static final List<Position> POSITIONS = List.of(
            new Position("Game Designer", "Invents the rules of chat games"),
            new Position("Game Developer", "Implements games and their logic"),
            new Position("QA Tester", "Plays every game before release"),
            new Position("Community Manager", "Runs tournaments and leaderboards"));

    @Override
    public String getName() {
        return "Games";
    }

    @Override
    public String getMission() {
        return "Quick games friends play right inside a chat.";
    }

    @Override
    public List<Position> getPositions() {
        return POSITIONS;
    }

    @Override
    public String getCodePackage() {
        return "uz.nixchat.common.games";
    }
}
