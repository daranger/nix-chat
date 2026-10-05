package uz.nchat.common.wallet;

import uz.nchat.common.model.User;

import java.time.Instant;

/**
 * One NCoin transfer. {@code from} is null for coins issued by Nchat (the welcome bonus).
 */
public record Transaction(long id, User from, User to, long amount, String note, Instant at) {

    public boolean isBonus() {
        return from == null;
    }

    /** "+50 from @bob" or "−20 to @alice", as seen by the given user. */
    public String describeFor(User viewer) {
        if (to.equals(viewer)) {
            String source = isBonus() ? "Nchat" : from.toString();
            return "+" + amount + " NC from " + source;
        }
        return "−" + amount + " NC to " + to;
    }
}
