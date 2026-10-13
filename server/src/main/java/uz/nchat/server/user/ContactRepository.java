package uz.nchat.server.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByOwnerId(Long ownerId);
    boolean existsByOwnerIdAndContactUserId(Long ownerId, Long contactUserId);
}
