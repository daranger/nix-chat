package uz.nixchat.server.auth;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import uz.nixchat.server.config.JwtProperties;

import java.time.Instant;

/**
 * Issues and checks JWT access tokens. The username is the token's subject.
 */
@Service
public class TokenService {

    public static final String ISSUER = "nixchat";
    public static final String DISPLAY_NAME_CLAIM = "name";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder encoder, JwtDecoder decoder, JwtProperties properties) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.properties = properties;
    }

    public String issue(UserAccount account) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(account.getUsername())
                .issuedAt(now)
                .expiresAt(now.plus(properties.ttl()))
                .claim(DISPLAY_NAME_CLAIM, account.getDisplayName())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Checks the signature and expiry and returns the username.
     *
     * @throws org.springframework.security.oauth2.jwt.JwtException if the token is invalid or expired
     */
    public String verify(String token) {
        Jwt jwt = decoder.decode(token);
        return jwt.getSubject();
    }
}
