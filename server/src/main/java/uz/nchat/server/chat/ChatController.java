package uz.nchat.server.chat;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uz.nchat.server.auth.AuthService;

import java.util.List;

/**
 * REST API for chats. The caller is always taken from the login token (JWT),
 * never from the request, so nobody can read or send messages as another user.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    public record CreateGroupRequest(String title, List<Long> memberIds) {}
    public record SendMessageRequest(String content) {}

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final AuthService authService;

    public ChatController(ConversationService conversationService, MessageService messageService,
                          AuthService authService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.authService = authService;
    }

    /** Opens the private chat with another user, creating it on the first call. */
    @PostMapping("/conversations/private")
    public ConversationDTO privateChat(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(required = false) Long otherUserId,
                                       @RequestParam(required = false) String username) {
        if (username != null) {
            return conversationService.getOrCreatePrivate(me(jwt), username);
        }
        if (otherUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Give otherUserId or username");
        }
        return conversationService.getOrCreatePrivate(me(jwt), otherUserId);
    }

    @PostMapping("/conversations/group")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationDTO createGroup(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateGroupRequest req) {
        List<Long> members = req.memberIds() == null ? List.of() : req.memberIds();
        return conversationService.createGroup(req.title(), me(jwt), members);
    }

    /** Chats the signed-in user is a member of. */
    @GetMapping("/conversations")
    public List<ConversationDTO> conversations(@AuthenticationPrincipal Jwt jwt) {
        return conversationService.getUserConversations(me(jwt));
    }

    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDTO send(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                           @RequestBody SendMessageRequest req) {
        return messageService.send(id, me(jwt), req.content());
    }

    @GetMapping("/conversations/{id}/messages")
    public List<MessageDTO> messages(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return messageService.getMessages(id, me(jwt));
    }

    /** Id of the signed-in account (table users). */
    private Long me(Jwt jwt) {
        return authService.findAccount(jwt.getSubject()).getId();
    }
}
