package uz.nchat.common.store;

public enum AppCategory {
    GAMES("Games"),
    MUSIC("Music"),
    FINANCE("Finance"),
    PRODUCTIVITY("Productivity"),
    BOTS("Bots");

    private final String label;

    AppCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
