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

    public UserDTO create(UserDTO dto) {
        if (userRepo.existsByPhoneNumber(dto.phoneNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already registered");
        }
        if (dto.username() != null && userRepo.existsByUsername(dto.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        UserProfile user = new UserProfile(dto.phoneNumber(), dto.displayName());
        user.setUsername(dto.username());
        user.setBio(dto.bio());
        user.setAvatarUrl(dto.avatarUrl());
        return toDTO(userRepo.save(user));
    }

    @Transactional(readOnly = true)
    public UserDTO getById(Long id) {
        return toDTO(find(id));
    }

    @Transactional(readOnly = true)
    public UserDTO getByPhone(String phone) {
        return userRepo.findByPhoneNumber(phone)
            .map(this::toDTO)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserDTO update(Long id, UserDTO dto) {
        UserProfile user = find(id);
        user.setUsername(dto.username());
        user.setDisplayName(dto.displayName());
        user.setBio(dto.bio());
        user.setAvatarUrl(dto.avatarUrl());
        return toDTO(user);
    }

    public void addContact(Long ownerId, Long contactUserId, String alias) {
        find(ownerId);
        find(contactUserId);
        if (contactRepo.existsByOwnerIdAndContactUserId(ownerId, contactUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contact already added");
        }
        contactRepo.save(new Contact(ownerId, contactUserId, alias));
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getContacts(Long ownerId) {
        return contactRepo.findByOwnerId(ownerId).stream()
            .map(c -> toDTO(find(c.getContactUserId())))
            .toList();
    }

    private UserProfile find(Long id) {
        return userRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserDTO toDTO(UserProfile u) {
        return new UserDTO(u.getId(), u.getPhoneNumber(), u.getUsername(),
            u.getDisplayName(), u.getBio(), u.getAvatarUrl());
    }
}
