package uz.nixchat.server.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import uz.nixchat.common.Protocol;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * First milestone: a single shared room. Every text message is broadcast to all connected clients.
 * Will be replaced by per-chat routing, authentication and persistence.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT = 512 * 1024;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // The decorator makes concurrent sends to one session thread-safe
        sessions.put(session.getId(),
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT));
        log.info("Client connected: {} (online: {})", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String text = message.getPayload().strip();
        if (text.isEmpty() || text.length() > Protocol.MAX_MESSAGE_LENGTH) {
            return;
        }
        broadcast(new TextMessage(text));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("Client disconnected: {} (online: {})", session.getId(), sessions.size());
    }

    private void broadcast(TextMessage message) {
        for (WebSocketSession s : sessions.values()) {
            if (!s.isOpen()) {
                continue;
            }
            try {
                s.sendMessage(message);
            } catch (IOException e) {
                log.warn("Failed to deliver message to {}", s.getId(), e);
            }
        }
    }
}
