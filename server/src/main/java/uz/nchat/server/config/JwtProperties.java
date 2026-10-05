package uz.nchat.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings from {@code nchat.jwt.*} in application.yml.
 *
 * @param secret HMAC key used to sign tokens, at least 32 characters
 * @param ttl    how long a token stays valid, e.g. {@code 7d}
 */
@ConfigurationProperties("nchat.jwt")
public record JwtProperties(String secret, Duration ttl) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("nchat.jwt.secret must be at least 32 characters");
        }
        if (ttl == null) {
            ttl = Duration.ofDays(7);
        }
    }
}
