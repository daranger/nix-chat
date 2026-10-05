package uz.nchat.server.phone;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings from {@code nchat.phone.*}.
 *
 * @param pepper        secret for the HMAC of phone keys; kept outside the database (environment variable)
 * @param telegramToken Telegram Gateway access token; empty = development mode, codes are printed to the console
 * @param codeTtl       how long a verification code is valid
 */
@ConfigurationProperties("nchat.phone")
public record PhoneProperties(String pepper, String telegramToken, Duration codeTtl) {

    public PhoneProperties {
        if (pepper == null || pepper.length() < 32) {
            throw new IllegalArgumentException("nchat.phone.pepper must be at least 32 characters");
        }
        if (codeTtl == null) {
            codeTtl = Duration.ofMinutes(5);
        }
    }

    public boolean telegramEnabled() {
        return telegramToken != null && !telegramToken.isBlank();
    }
}
