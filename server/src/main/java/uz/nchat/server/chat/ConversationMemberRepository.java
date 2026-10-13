package uz.nchat.server.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {
    List<ConversationMember> findByUserId(Long userId);
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);
}
