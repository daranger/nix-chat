package uz.nchat.server.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import uz.nchat.common.Protocol;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The shared room. Only logged-in users get here (see {@link JwtHandshakeInterceptor});
 * the server puts the verified username in front of every message, so nobody can impersonate anyone.
 * Wire format to clients: {@code "username: text"}; service notices have no prefix.
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
        log.info("{} connected (online: {})", username(session), sessions.size());
        broadcast(new TextMessage(username(session) + " joined the chat"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String text = message.getPayload().strip();
        if (text.isEmpty() || text.length() > Protocol.MAX_MESSAGE_LENGTH) {
            return;
        }
        broadcast(new TextMessage(username(session) + ": " + text));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        if (sessions.remove(session.getId()) != null) {
            log.info("{} disconnected (online: {})", username(session), sessions.size());
            broadcast(new TextMessage(username(session) + " left the chat"));
        }
    }

    private static String username(WebSocketSession session) {
        return String.valueOf(session.getAttributes().get(JwtHandshakeInterceptor.USERNAME_ATTRIBUTE));
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
