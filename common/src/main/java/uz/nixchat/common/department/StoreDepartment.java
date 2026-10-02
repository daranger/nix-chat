package uz.nixchat.common.department;

import java.util.List;

public class StoreDepartment extends Department {

    private static final List<Position> POSITIONS = List.of(
            new Position("Store Manager", "Runs the catalog and its categories"),
            new Position("App Reviewer", "Checks every mini-app before it is published"),
            new Position("Platform Developer", "The catalog, installs and ratings"),
            new Position("Developer Relations", "Helps outside developers publish apps"));

    @Override
    public String getName() {
        return "Store";
    }

    @Override
    public String getMission() {
        return "NixStore, a catalog of mini-apps and bots users install into NixChat.";
    }

    @Override
    public List<Position> getPositions() {
        return POSITIONS;
    }

    @Override
    public String getCodePackage() {
        return "uz.nixchat.common.store";
    }
}
