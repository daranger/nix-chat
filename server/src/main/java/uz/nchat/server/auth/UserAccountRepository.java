package uz.nchat.server.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Database access for accounts (table {@code users}). Spring Data writes the SQL for each
 * method from its name, for example findByUsername becomes "SELECT ... WHERE username = ?".
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    // Time complexity: O(log n). The username column is UNIQUE, so the database keeps a
    // B-tree index on it and finds the row without scanning all n users.
    // Space complexity: O(1). At most one account is returned.
    Optional<UserAccount> findByUsername(String username);

    // Time complexity: O(log n). Same index as above; the database stops at the first match.
    // Space complexity: O(1). It returns only true or false.
    boolean existsByUsername(String username);

    // Time complexity: O(log n). phone_hash is also UNIQUE, so it has its own index.
    // Space complexity: O(1). At most one account is returned.
    Optional<UserAccount> findByPhoneHash(String phoneHash);

    // Time complexity: O(k log n), where k is the number of hashes we search for.
    // The database does one index lookup per hash.
    // Space complexity: O(k). In the worst case every hash matches and we get k accounts.
    List<UserAccount> findByPhoneHashIn(Collection<String> phoneHashes);

    /*
     * HOW WE DERIVED THE COMPLEXITY
     *
     * Let n be the number of users and k the number of phone hashes in one request.
     *
     * Every column we search by (username and phone_hash) has a UNIQUE constraint, and the
     * database automatically builds a B-tree index for it. A B-tree has height about log n,
     * so finding one row takes O(log n) instead of checking all n rows. findByPhoneHashIn()
     * repeats this for each of the k hashes, which gives O(k log n).
     *
     * For space, the first three methods return at most one row (or a boolean), so they are
     * O(1). Only findByPhoneHashIn() returns a list, and that list has at most k accounts,
     * so it is O(k).
     *
     * Without these indexes every search would scan the whole table and cost O(n).
     */
}
