package uz.nixchat.server.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import uz.nixchat.common.Protocol;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatHandler;
    private final JwtHandshakeInterceptor jwtInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatHandler, JwtHandshakeInterceptor jwtInterceptor) {
        this.chatHandler = chatHandler;
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler, Protocol.WS_PATH)
                .addInterceptors(jwtInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
