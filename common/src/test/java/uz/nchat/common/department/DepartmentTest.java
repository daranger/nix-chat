package uz.nchat.common.department;

import org.junit.jupiter.api.Test;
import uz.nchat.common.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepartmentTest {

    @Test
    void companyHasSixDepartmentsWithFourPositionsEach() {
        assertEquals(6, NchatCompany.getDepartments().size());
        for (Department department : NchatCompany.getDepartments()) {
            assertEquals(4, department.getPositions().size(), department.getName());
        }
        assertEquals(24, NchatCompany.totalPositions());
    }

    @Test
    void departmentsAreFoundByName() {
        assertNotNull(NchatCompany.find("wallet"));
        assertNull(NchatCompany.find("Logistics"));
    }

    @Test
    void staffCanOnlyTakeExistingPositions() {
        Department wallet = new WalletDepartment();
        User roman = new User("roman");

        wallet.assign(roman, "Payments Developer");
        assertEquals("Payments Developer", wallet.getPositionOf(roman).title());
        assertThrows(IllegalArgumentException.class, () -> wallet.assign(roman, "Pilot"));
    }

    @Test
    void describeListsEveryPosition() {
        String text = new GamesDepartment().describe();
        assertTrue(text.startsWith("Games Department"));
        assertTrue(text.contains("4. Community Manager"));
    }
}
