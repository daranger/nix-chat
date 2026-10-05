package uz.nchat.common.department;

import java.util.List;

public class SecurityDepartment extends Department {

    private static final List<Position> POSITIONS = List.of(
            new Position("Security Lead", "Owns the security plan of the product"),
            new Position("Crypto Engineer", "Encryption of messages and local data"),
            new Position("Risk Analyst", "Reviews accounts, abuse and threats"),
            new Position("Incident Responder", "Reacts when something goes wrong"));

    @Override
    public String getName() {
        return "Security";
    }

    @Override
    public String getMission() {
        return "Encryption, account safety and the response to incidents.";
    }

    @Override
    public List<Position> getPositions() {
        return POSITIONS;
    }

    @Override
    public String getCodePackage() {
        return "uz.nchat.common.crypto";
    }
}
