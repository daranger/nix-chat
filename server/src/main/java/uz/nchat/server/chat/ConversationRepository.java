package uz.nchat.server.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("""
        select c from Conversation c
        where c.groupChat = false
          and exists (select 1 from ConversationMember m where m.conversationId = c.id and m.userId = :a)
          and exists (select 1 from ConversationMember m where m.conversationId = c.id and m.userId = :b)
        """)
    Optional<Conversation> findPrivate(@Param("a") Long a, @Param("b") Long b);
}
