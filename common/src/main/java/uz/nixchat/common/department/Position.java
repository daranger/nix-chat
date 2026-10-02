package uz.nixchat.common.department;

/**
 * A job position inside a department, e.g. "Payments Developer".
 */
public record Position(String title, String responsibility) {

    public Position {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Position title is required");
        }
        responsibility = (responsibility == null) ? "" : responsibility;
    }
}
