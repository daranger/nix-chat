package uz.nchat.server.chat;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    public record CreateGroupRequest(String title, Long creatorId, List<Long> memberIds) {}
    public record SendMessageRequest(Long senderId, String content) {}

    private final ConversationService conversationService;
    private final MessageService messageService;

    public ChatController(ConversationService conversationService, MessageService messageService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @PostMapping("/conversations/private")
    public ConversationDTO privateChat(@RequestParam Long userId, @RequestParam Long otherUserId) {
        return conversationService.getOrCreatePrivate(userId, otherUserId);
    }

    @PostMapping("/conversations/group")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationDTO createGroup(@RequestBody CreateGroupRequest req) {
        return conversationService.createGroup(req.title(), req.creatorId(), req.memberIds());
    }

    @GetMapping("/users/{userId}/conversations")
    public List<ConversationDTO> conversations(@PathVariable Long userId) {
        return conversationService.getUserConversations(userId);
    }

    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDTO send(@PathVariable Long id, @RequestBody SendMessageRequest req) {
        return messageService.send(id, req.senderId(), req.content());
    }

    @GetMapping("/conversations/{id}/messages")
    public List<MessageDTO> messages(@PathVariable Long id, @RequestParam Long userId) {
        return messageService.getMessages(id, userId);
    }
}
