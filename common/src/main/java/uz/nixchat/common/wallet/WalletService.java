package uz.nixchat.common.wallet;

import uz.nixchat.common.model.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NixCoin wallets: a virtual in-app currency with no real-money value.
 * Every new wallet starts with a welcome bonus; users send coins to each other.
 */
public class WalletService {

    public static final long WELCOME_BONUS = 100;
    public static final long MAX_TRANSFER = 10_000;
    private static final int NOTE_MAX_LENGTH = 100;

    private final Map<User, Long> balances = new HashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private long nextId = 1;

    /** Creates a wallet with the welcome bonus. Does nothing if the user already has one. */
    public synchronized void openWallet(User user) {
        if (balances.containsKey(user)) {
            return;
        }
        balances.put(user, WELCOME_BONUS);
        transactions.add(new Transaction(nextId++, null, user, WELCOME_BONUS, "Welcome bonus", Instant.now()));
    }

    public synchronized long getBalance(User user) {
        Long balance = balances.get(user);
        if (balance == null) {
            throw new IllegalStateException(user + " has no wallet");
        }
        return balance;
    }

    /**
     * Sends coins from one user to another.
     *
     * @throws InsufficientFundsException if the sender's balance is too low
     * @throws IllegalArgumentException   for an invalid amount, a transfer to oneself or a missing wallet
     */
    public synchronized Transaction transfer(User from, User to, long amount, String note)
            throws InsufficientFundsException {
        if (from.equals(to)) {
            throw new IllegalArgumentException("You cannot send coins to yourself");
        }
        if (amount <= 0 || amount > MAX_TRANSFER) {
            throw new IllegalArgumentException("Amount must be between 1 and " + MAX_TRANSFER);
        }
        if (!balances.containsKey(to)) {
            throw new IllegalArgumentException(to + " has no wallet");
        }
        long balance = getBalance(from);
        if (balance < amount) {
            throw new InsufficientFundsException(balance, amount);
        }

        String cleanNote = (note == null) ? "" : note.strip();
        if (cleanNote.length() > NOTE_MAX_LENGTH) {
            cleanNote = cleanNote.substring(0, NOTE_MAX_LENGTH);
        }
        balances.put(from, balance - amount);
        balances.put(to, balances.get(to) + amount);
        Transaction transaction = new Transaction(nextId++, from, to, amount, cleanNote, Instant.now());
        transactions.add(transaction);
        return transaction;
    }

    /** All transactions that involve the user, newest first. */
    public synchronized List<Transaction> history(User user) {
        List<Transaction> result = new ArrayList<>();
        for (int i = transactions.size() - 1; i >= 0; i--) {
            Transaction t = transactions.get(i);
            if (t.to().equals(user) || user.equals(t.from())) {
                result.add(t);
            }
        }
        return result;
    }

    /** Total coins in circulation; transfers never change it, only bonuses do. */
    public synchronized long totalSupply() {
        long total = 0;
        for (long balance : balances.values()) {
            total += balance;
        }
        return total;
    }
}
