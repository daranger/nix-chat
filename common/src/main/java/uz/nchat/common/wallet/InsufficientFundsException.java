package uz.nchat.common.wallet;

/**
 * Thrown when a wallet does not hold enough NCoin for a transfer.
 * A checked exception: the caller must decide what to tell the user.
 */
public class InsufficientFundsException extends Exception {

    private static final long serialVersionUID = 1L;

    private final long balance;
    private final long requested;

    public InsufficientFundsException(long balance, long requested) {
        super("Not enough NCoin: balance " + balance + ", needed " + requested);
        this.balance = balance;
        this.requested = requested;
    }

    public long getBalance() {
        return balance;
    }

    public long getRequested() {
        return requested;
    }

    public long getMissing() {
        return requested - balance;
    }
}
