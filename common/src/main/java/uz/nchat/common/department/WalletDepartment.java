package uz.nchat.common.department;

import java.util.List;

public class WalletDepartment extends Department {

    private static final List<Position> POSITIONS = List.of(
            new Position("Wallet Manager", "Owns NCoin rules and limits"),
            new Position("Payments Developer", "Transfers and transaction history"),
            new Position("Fraud Analyst", "Spots suspicious transfers"),
            new Position("Support Agent", "Helps users with wallet questions"));

    @Override
    public String getName() {
        return "Wallet";
    }

    @Override
    public String getMission() {
        return "NCoin, the in-app virtual currency: balances and transfers between friends.";
    }

    @Override
    public List<Position> getPositions() {
        return POSITIONS;
    }

    @Override
    public String getCodePackage() {
        return "uz.nchat.common.wallet";
    }
}
