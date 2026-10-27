// UserService.java
package uz.nchat.server.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserProfileRepository userRepo;
    private final ContactRepository contactRepo;

    public UserService(UserProfileRepository userRepo, ContactRepository contactRepo) {
        this.userRepo = userRepo;
        this.contactRepo = contactRepo;
    }

    // Time complexity: O(log n). We do two indexed existence checks and one insert,
    // and each of them costs O(log n). Copying the fields is O(1).
    // Space complexity: O(1). We create one UserProfile and one DTO, no matter how many users exist.
    public UserDTO create(UserDTO dto) {
        // Search on the phone number index: O(log n).
        if (userRepo.existsByPhoneNumber(dto.phoneNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already registered");
        }
        // Search on the username index: O(log n).
        if (dto.username() != null && userRepo.existsByUsername(dto.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        // Creating the object and calling the setters only copies a fixed number of fields: O(1).
        UserProfile user = new UserProfile(dto.phoneNumber(), dto.displayName());
        user.setUsername(dto.username());
        user.setBio(dto.bio());
        user.setAvatarUrl(dto.avatarUrl());
        // Saving inserts one row and updates the indexes: O(log n).
        return toDTO(userRepo.save(user));
    }

    // Time complexity: O(log n). One lookup by primary key.
    // Space complexity: O(1). We hold one entity and one DTO.
    @Transactional(readOnly = true)
    public UserDTO getById(Long id) {
        return toDTO(find(id));
    }

    // Time complexity: O(log n). One lookup on the phone number index.
    // Space complexity: O(1). We hold one entity and one DTO.
    @Transactional(readOnly = true)
    public UserDTO getByPhone(String phone) {
        return userRepo.findByPhoneNumber(phone) // O(log n) with the index
            .map(this::toDTO)                    // O(1)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    // Time complexity: O(log n). We find the user by primary key, and the setters are O(1).
    // There is no save() call, because Hibernate detects the changes (dirty checking) and
    // updates the row when the transaction ends, which also costs O(log n).
    // Space complexity: O(1). We only hold one entity and one DTO.
    public UserDTO update(Long id, UserDTO dto) {
        UserProfile user = find(id); // O(log n)
        user.setUsername(dto.username());
        user.setDisplayName(dto.displayName());
        user.setBio(dto.bio());
        user.setAvatarUrl(dto.avatarUrl());
        return toDTO(user); // O(1)
    }

    // Time complexity: O(log n). We do two primary key lookups, one indexed existence
    // check, and one insert. That is 4 * O(log n), which simplifies to O(log n).
    // Space complexity: O(1). We create one Contact object.
    public void addContact(Long ownerId, Long contactUserId, String alias) {
        find(ownerId);        // O(log n)
        find(contactUserId);  // O(log n)
        // Search on the (owner_id, contact_user_id) index: O(log n).
        if (contactRepo.existsByOwnerIdAndContactUserId(ownerId, contactUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contact already added");
        }
        // Insert one row and update the index: O(log n).
        contactRepo.save(new Contact(ownerId, contactUserId, alias));
    }

    // Time complexity: O(k log n), where k is the number of contacts of the owner.
    // We load k contact rows, then do one primary key lookup for each contact.
    // Space complexity: O(k). We hold k contacts and then a list of k DTOs.
    @Transactional(readOnly = true)
    public List<UserDTO> getContacts(Long ownerId) {
        // findByOwnerId uses the owner index: O(log n + k) to find and read k rows.
        return contactRepo.findByOwnerId(ownerId).stream()
            // For each of the k contacts we call find(), which costs O(log n).
            // k lookups * O(log n) = O(k log n). This is the most expensive part.
            .map(c -> toDTO(find(c.getContactUserId())))
            .toList();
    }

    // Time complexity: O(log n). findById searches the primary key index.
    // Space complexity: O(1). We only hold one entity.
    private UserProfile find(Long id) {
        return userRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    // Time complexity: O(1). We read six fields and create one object.
    // Space complexity: O(1). We create a single DTO.
    private UserDTO toDTO(UserProfile u) {
        return new UserDTO(u.getId(), u.getPhoneNumber(), u.getUsername(),
            u.getDisplayName(), u.getBio(), u.getAvatarUrl());
    }

    /*
     * HOW WE DERIVED THE TIME COMPLEXITY
     *
     * Let n be the number of rows in the user table and k the number of contacts of one user.
     * The strings (phone, username, bio) have a limited length, so we treat copying them as O(1).
     *
     * find():
     * It calls findById, which searches the primary key index (a B-tree), so it is O(log n).
     * Most methods below are built from this one.
     *
     * create():
     * Two existence checks on indexed columns (O(log n) each) and one save (O(log n)).
     * Together that is 3 * O(log n), which simplifies to O(log n) because constants are dropped.
     *
     * getById() and getByPhone():
     * Each does one indexed lookup and one O(1) conversion, so both are O(log n).
     *
     * update():
     * One find (O(log n)) and four setters (O(1)). Hibernate writes the change when the
     * transaction ends, which is another O(log n). The total is O(log n).
     *
     * addContact():
     * Two find() calls, one existence check and one save. That is 4 * O(log n) = O(log n).
     *
     * getContacts():
     * First we read k contact rows in O(log n + k). Then for every contact we call find(),
     * which is O(log n), so this part is k * O(log n) = O(k log n). We add the parts:
     * O(log n + k + k log n). The largest term wins, so the result is O(k log n).
     *
     * toDTO():
     * It reads six fields and builds one object, so it is O(1). It is called k times in
     * getContacts(), which we already counted above.
     *
     * Assumption: the O(log n) results are only true if phone_number, username and
     * (owner_id, contact_user_id) are indexed (unique constraints create indexes).
     * Without indexes, each search would scan the whole table and cost O(n).
     *
     * Note: getContacts() runs one query for the contacts and then k more queries, one per
     * contact. This is known as the N+1 query problem. It could be reduced by loading all
     * users with a single findAllById call, but the code stays unchanged here.
     *
     * HOW WE DERIVED THE SPACE COMPLEXITY
     *
     * We counted the extra memory each method creates while it runs, not the database storage.
     *
     * create(), getById(), getByPhone(), update(), addContact(), find() and toDTO():
     * They only create a fixed number of objects (one entity, one DTO, one contact), so
     * the memory does not grow with n. Each of them is O(1).
     *
     * getContacts():
     * We keep a list of k Contact rows and then build a list of k DTOs. That is
     * 2 * O(k), which simplifies to O(k) because constants are dropped.
     */
}
