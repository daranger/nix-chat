package uz.nixchat.common.store;

import uz.nixchat.common.model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * NixStore: the catalog of mini-apps users install into NixChat, like an app store inside the messenger.
 */
public class NixStore {

    private final Map<String, MiniApp> catalog = new LinkedHashMap<>();
    private final Map<User, Set<String>> installed = new HashMap<>();

    public void publish(MiniApp app) {
        if (catalog.containsKey(app.getId())) {
            throw new IllegalArgumentException("An app with id '" + app.getId() + "' already exists");
        }
        catalog.put(app.getId(), app);
    }

    public MiniApp get(String appId) {
        MiniApp app = catalog.get(appId);
        if (app == null) {
            throw new IllegalArgumentException("No app with id '" + appId + "'");
        }
        return app;
    }

    public List<MiniApp> search(String query) {
        List<MiniApp> result = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return result;
        }
        for (MiniApp app : catalog.values()) {
            if (app.matches(query.strip())) {
                result.add(app);
            }
        }
        return result;
    }

    public List<MiniApp> byCategory(AppCategory category) {
        List<MiniApp> result = new ArrayList<>();
        for (MiniApp app : catalog.values()) {
            if (app.getCategory() == category) {
                result.add(app);
            }
        }
        return result;
    }

    /** Returns true if the app was newly installed, false if the user already had it. */
    public boolean install(User user, String appId) {
        get(appId);
        return installed.computeIfAbsent(user, u -> new HashSet<>()).add(appId);
    }

    public boolean uninstall(User user, String appId) {
        Set<String> apps = installed.get(user);
        return apps != null && apps.remove(appId);
    }

    public boolean isInstalled(User user, String appId) {
        Set<String> apps = installed.get(user);
        return apps != null && apps.contains(appId);
    }

    /** Only users who installed an app may rate it. */
    public void rate(User user, String appId, int stars) {
        if (!isInstalled(user, appId)) {
            throw new IllegalStateException("Install the app before rating it");
        }
        get(appId).rate(user, stars);
    }

    /** The best-rated apps first; apps without ratings are left out. */
    public List<MiniApp> topRated(int limit) {
        List<MiniApp> rated = new ArrayList<>();
        for (MiniApp app : catalog.values()) {
            if (app.ratingCount() > 0) {
                rated.add(app);
            }
        }
        rated.sort(Comparator.comparingDouble(MiniApp::averageRating).reversed());
        return rated.subList(0, Math.min(limit, rated.size()));
    }

    public int size() {
        return catalog.size();
    }
}
