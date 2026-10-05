package uz.nchat.common.department;

import java.util.List;

public class MessagingDepartment extends Department {

    private static final List<Position> POSITIONS = List.of(
            new Position("Product Lead", "Decides which chat features ship and when"),
            new Position("Backend Developer", "Server delivery, chats and history"),
            new Position("Client Developer", "The JavaFX desktop app"),
            new Position("Moderator", "Handles reports and keeps groups safe"));

    @Override
    public String getName() {
        return "Messaging";
    }

    @Override
    public String getMission() {
        return "Private and group chats that deliver every message instantly.";
    }

    @Override
    public List<Position> getPositions() {
        return POSITIONS;
    }

    @Override
    public String getCodePackage() {
        return "uz.nchat.common.model";
    }
}
