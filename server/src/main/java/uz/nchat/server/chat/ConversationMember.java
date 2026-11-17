package uz.nchat.server.chat;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "conversation_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"})
)
public class ConversationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    public ConversationMember(Long conversationId, Long userId) {
        this.conversationId = conversationId;
        this.userId = userId;
    }

    protected ConversationMember() {
    }

    public Long getId() { return id; }
    public Long getConversationId() { return conversationId; }
    public Long getUserId() { return userId; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
