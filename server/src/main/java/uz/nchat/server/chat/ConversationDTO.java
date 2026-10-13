package uz.nchat.server.chat;

import java.time.LocalDateTime;

public record ConversationDTO(Long id, boolean groupChat, String title, LocalDateTime createdAt) {}
