package uz.nchat.server.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<UserAccount> findByPhoneHash(String phoneHash);

    List<UserAccount> findByPhoneHashIn(Collection<String> phoneHashes);
}
