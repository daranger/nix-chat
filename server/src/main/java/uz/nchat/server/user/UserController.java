// UserController.java
package uz.nchat.server.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDTO create(@RequestBody UserDTO dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    public UserDTO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/phone/{phone}")
    public UserDTO getByPhone(@PathVariable String phone) {
        return service.getByPhone(phone);
    }

    @PutMapping("/{id}")
    public UserDTO update(@PathVariable Long id, @RequestBody UserDTO dto) {
        return service.update(id, dto);
    }

    @PostMapping("/{id}/contacts/{contactUserId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void addContact(@PathVariable Long id,
                           @PathVariable Long contactUserId,
                           @RequestParam(required = false) String alias) {
        service.addContact(id, contactUserId, alias);
    }

    @GetMapping("/{id}/contacts")
    public List<UserDTO> getContacts(@PathVariable Long id) {
        return service.getContacts(id);
    }
}
