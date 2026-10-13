package uz.nchat.server.chat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepo;
    private final ConversationMemberRepository memberRepo;

    public ConversationService(ConversationRepository conversationRepo,
                               ConversationMemberRepository memberRepo) {
        this.conversationRepo = conversationRepo;
        this.memberRepo = memberRepo;
    }

    // returns the existing private chat if there is one
    public ConversationDTO getOrCreatePrivate(Long userId, Long otherUserId) {
        return toDTO(conversationRepo.findPrivate(userId, otherUserId).orElseGet(() -> {
            Conversation c = conversationRepo.save(new Conversation(false, null));
            memberRepo.save(new ConversationMember(c.getId(), userId));
            memberRepo.save(new ConversationMember(c.getId(), otherUserId));
            return c;
        }));
    }

    public ConversationDTO createGroup(String title, Long creatorId, List<Long> memberIds) {
        Conversation c = conversationRepo.save(new Conversation(true, title));
        memberRepo.save(new ConversationMember(c.getId(), creatorId));
        memberIds.stream()
            .distinct()
            .filter(id -> !id.equals(creatorId))
            .forEach(id -> memberRepo.save(new ConversationMember(c.getId(), id)));
        return toDTO(c);
    }

    @Transactional(readOnly = true)
    public List<ConversationDTO> getUserConversations(Long userId) {
        List<Long> ids = memberRepo.findByUserId(userId).stream()
            .map(ConversationMember::getConversationId)
            .toList();
        return conversationRepo.findAllById(ids).stream().map(this::toDTO).toList();
    }

    private ConversationDTO toDTO(Conversation c) {
        return new ConversationDTO(c.getId(), c.isGroupChat(), c.getTitle(), c.getCreatedAt());
    }
}
