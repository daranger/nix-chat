package uz.nchat.server.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByPhoneNumber(String phoneNumber);
    Optional<UserProfile> findByUsername(String username);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByUsername(String username);
}
