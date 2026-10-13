package uz.nchat.server.chat;

import java.time.LocalDateTime;

public record MessageDTO(Long id, Long conversationId, Long senderId, String content, LocalDateTime sentAt) {}
