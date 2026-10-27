package uz.nchat.server.auth;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import uz.nchat.server.config.JwtProperties;

import java.time.Instant;

/**
 * Issues and checks JWT access tokens. The username is the token's subject.
 */
@Service
public class TokenService {

    public static final String ISSUER = "nchat";
    public static final String DISPLAY_NAME_CLAIM = "name";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder encoder, JwtDecoder decoder, JwtProperties properties) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.properties = properties;
    }

    // Time complexity: O(1). We build a token from a fixed number of claims, and the biggest
    // values (username 32 and display name 64 characters) have a limited length.
    // We never touch the database, so the cost does not depend on the number of users.
    // Space complexity: O(1). The claims object, the header and the token string have a
    // bounded size, no matter how many users exist.
    public String issue(UserAccount account) {
        // Reading the clock and adding the time-to-live are single operations: O(1).
        Instant now = Instant.now();
        // We set five claims (issuer, subject, issued at, expires at, name). It is a fixed
        // number of claims, so building the set is O(1).
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject(account.getUsername())
            .issuedAt(now)
            .expiresAt(now.plus(properties.ttl()))
            .claim(DISPLAY_NAME_CLAIM, account.getDisplayName())
            .build();
        // The header only names the algorithm: O(1).
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        // encode() writes the claims as JSON, encodes them in Base64 and signs them with
        // HMAC-SHA256. Each step makes one pass over a short, bounded input, so it is O(1).
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    // Time complexity: O(T), where T is the length of the token. We decode the Base64 parts,
    // parse the JSON and recompute the signature, and each of these reads the token once.
    // The tokens we issue have a bounded length, so for them this is effectively O(1).
    // Space complexity: O(T). We create a decoded copy of the token parts and a Jwt object.
    /**
     * Checks the signature and expiry and returns the username.
     *
     * @throws org.springframework.security.oauth2.jwt.JwtException if the token is invalid or expired
     */
    public String verify(String token) {
        // decode() checks the signature and the expiry. It does not query the database,
        // so the cost does not depend on n (the number of users).
        Jwt jwt = decoder.decode(token);
        // Reading one claim from the map of claims: O(1).
        return jwt.getSubject();
    }

    /*
     * HOW WE DERIVED THE TIME COMPLEXITY
     *
     * Let T be the length of a token. For tokens that we issue, T is bounded, because the
     * username is at most 32 characters, the display name at most 64, and the other claims
     * have a fixed size.
     *
     * issue():
     * We create a fixed number of claims, one header, and then encode and sign them. Encoding
     * and HMAC-SHA256 make one pass over a short input, so the work is bounded by a constant
     * and the method is O(1). It does not depend on the number of users, because no database
     * call is made. This confirms the O(1) assumption we made in AuthService.toResponse().
     *
     * verify():
     * The decoder splits the token, decodes it, parses the JSON and recomputes the signature.
     * Each step reads the token once, so the cost is O(T). For tokens we issued, T is
     * bounded, so it is effectively O(1). If someone sends a very long fake token, the cost
     * grows with T, which is why we state O(T) and not O(1) for this method.
     *
     * HOW WE DERIVED THE SPACE COMPLEXITY
     *
     * We counted the extra memory each method creates while it runs.
     *
     * issue():
     * We create one claims object, one header and one token string, all of bounded size,
     * so the space is O(1).
     *
     * verify():
     * The decoder creates decoded copies of the token parts and one Jwt object. Their size
     * is proportional to the token, so the space is O(T), which is O(1) for our own tokens.
     */
}
