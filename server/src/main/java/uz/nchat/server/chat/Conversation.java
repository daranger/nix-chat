package uz.nchat.server.chat;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean groupChat;

    private String title; // only for groups

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Conversation() {}

    public Conversation(boolean groupChat, String title) {
        this.groupChat = groupChat;
        this.title = title;
    }

    public Long getId() { return id; }
    public boolean isGroupChat() { return groupChat; }
    public String getTitle() { return title; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
