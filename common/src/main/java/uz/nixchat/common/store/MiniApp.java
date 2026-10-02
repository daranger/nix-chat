package uz.nixchat.common.store;

import uz.nixchat.common.model.User;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * A mini-app or bot published in NixStore. Each user can rate it once (1–5 stars); a new rating replaces the old one.
 */
public class MiniApp {

    private final String id;
    private final String name;
    private final String developer;
    private final AppCategory category;
    private final String description;
    private final Map<User, Integer> ratings = new HashMap<>();

    public MiniApp(String id, String name, String developer, AppCategory category, String description) {
        if (id == null || !id.matches("[a-z0-9-]{3,32}")) {
            throw new IllegalArgumentException("App id must be 3-32 characters: a-z, 0-9 and -");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("App name is required");
        }
        this.id = id;
        this.name = name.strip();
        this.developer = Objects.requireNonNullElse(developer, "Unknown");
        this.category = Objects.requireNonNull(category, "category");
        this.description = Objects.requireNonNullElse(description, "");
    }

    void rate(User user, int stars) {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Rating must be 1 to 5 stars");
        }
        ratings.put(user, stars);
    }

    public double averageRating() {
        if (ratings.isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (int stars : ratings.values()) {
            sum += stars;
        }
        return (double) sum / ratings.size();
    }

    public int ratingCount() {
        return ratings.size();
    }

    public boolean matches(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        return name.toLowerCase(Locale.ROOT).contains(q) || description.toLowerCase(Locale.ROOT).contains(q);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDeveloper() {
        return developer;
    }

    public AppCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        String rating = ratings.isEmpty() ? "no ratings" : String.format(Locale.ROOT, "%.1f★ (%d)", averageRating(), ratingCount());
        return name + " · " + category.getLabel() + " · " + rating;
    }
}
