package uz.nchat.server.chat;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class MessageService {

    private final MessageRepository messageRepo;
    private final ConversationMemberRepository memberRepo;

    public MessageService(MessageRepository messageRepo, ConversationMemberRepository memberRepo) {
        this.messageRepo = messageRepo;
        this.memberRepo = memberRepo;
    }

    public MessageDTO send(Long conversationId, Long senderId, String content) {
        checkMember(conversationId, senderId);
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is empty");
        }
        return toDTO(messageRepo.save(new Message(conversationId, senderId, content)));
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getMessages(Long conversationId, Long userId) {
        checkMember(conversationId, userId);
        return messageRepo.findByConversationIdOrderByIdAsc(conversationId)
            .stream().map(this::toDTO).toList();
    }

    private void checkMember(Long conversationId, Long userId) {
        if (!memberRepo.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this conversation");
        }
    }

    private MessageDTO toDTO(Message m) {
        return new MessageDTO(m.getId(), m.getConversationId(), m.getSenderId(),
            m.getContent(), m.getSentAt());
    }
}
