package uz.nixchat.common.department;

import uz.nixchat.common.model.User;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One department of the Nchat company. Each subclass defines its name, mission and positions;
 * this class handles staffing: who works in which position.
 */
public abstract class Department {

    private final Map<User, Position> staff = new LinkedHashMap<>();

    /** Short name, e.g. "Wallet". */
    public abstract String getName();

    /** One sentence: what the department is responsible for. */
    public abstract String getMission();

    /** The positions this department has (four per department). */
    public abstract List<Position> getPositions();

    /** Package in the code base where the department's features live. */
    public abstract String getCodePackage();

    /** Puts a user into one of this department's positions. */
    public void assign(User user, String positionTitle) {
        Position position = findPosition(positionTitle);
        if (position == null) {
            throw new IllegalArgumentException(getName() + " has no position '" + positionTitle + "'");
        }
        staff.put(user, position);
    }

    public Position findPosition(String title) {
        for (Position position : getPositions()) {
            if (position.title().equalsIgnoreCase(title)) {
                return position;
            }
        }
        return null;
    }

    public Position getPositionOf(User user) {
        return staff.get(user);
    }

    public Map<User, Position> getStaff() {
        return Collections.unmodifiableMap(staff);
    }

    /** Multi-line description, used in reports and the "About" screen. */
    public String describe() {
        StringBuilder text = new StringBuilder(getName() + " Department — " + getMission());
        int number = 1;
        for (Position position : getPositions()) {
            text.append("\n  ").append(number++).append(". ").append(position.title());
        }
        return text.toString();
    }

    @Override
    public String toString() {
        return getName() + " (" + getPositions().size() + " positions, " + staff.size() + " staff)";
    }
}
