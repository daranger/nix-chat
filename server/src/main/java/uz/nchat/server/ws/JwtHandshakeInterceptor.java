package uz.nchat.server.ws;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import uz.nchat.server.auth.TokenService;

import java.util.Map;

/**
 * Only logged-in users may open the WebSocket. The token comes in the {@code Authorization: Bearer ...} header
 * (desktop client) or the {@code ?token=} query parameter (clients that cannot set headers).
 * The verified username is stored in the session, so a client can never send messages under someone else's name.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USERNAME_ATTRIBUTE = "username";
    private static final String BEARER = "Bearer ";

    private final TokenService tokens;

    public JwtHandshakeInterceptor(TokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            attributes.put(USERNAME_ATTRIBUTE, tokens.verify(token));
            return true;
        } catch (JwtException e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // nothing to do
    }

    private static String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst("Authorization");
        if (header != null && header.startsWith(BEARER)) {
            return header.substring(BEARER.length()).strip();
        }
        return UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
    }
}
