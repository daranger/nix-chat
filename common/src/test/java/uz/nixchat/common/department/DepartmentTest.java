package uz.nixchat.common.department;

import org.junit.jupiter.api.Test;
import uz.nixchat.common.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepartmentTest {

    @Test
    void companyHasSixDepartmentsWithFourPositionsEach() {
        assertEquals(6, NixChatCompany.getDepartments().size());
        for (Department department : NixChatCompany.getDepartments()) {
            assertEquals(4, department.getPositions().size(), department.getName());
        }
        assertEquals(24, NixChatCompany.totalPositions());
    }

    @Test
    void departmentsAreFoundByName() {
        assertNotNull(NixChatCompany.find("wallet"));
        assertNull(NixChatCompany.find("Logistics"));
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
