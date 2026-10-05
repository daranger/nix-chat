package uz.nixchat.common.department;

import java.util.List;

/**
 * The company behind Nchat and its six departments.
 */
public final class NixChatCompany {

    private static final List<Department> DEPARTMENTS = List.of(
            new MessagingDepartment(),
            new WalletDepartment(),
            new MusicDepartment(),
            new GamesDepartment(),
            new StoreDepartment(),
            new SecurityDepartment());

    private NixChatCompany() {
    }

    public static List<Department> getDepartments() {
        return DEPARTMENTS;
    }

    /** Finds a department by name, ignoring case; null if there is none. */
    public static Department find(String name) {
        for (Department department : DEPARTMENTS) {
            if (department.getName().equalsIgnoreCase(name)) {
                return department;
            }
        }
        return null;
    }

    public static int totalPositions() {
        int total = 0;
        for (Department department : DEPARTMENTS) {
            total += department.getPositions().size();
        }
        return total;
    }
}
