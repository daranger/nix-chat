package uz.nixchat.server.phone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PhoneProperties.class)
public class PhoneConfig {

    private static final Logger log = LoggerFactory.getLogger(PhoneConfig.class);

    /** Polymorphism: the rest of the code only knows {@link VerificationSender}. */
    @Bean
    public VerificationSender verificationSender(PhoneProperties properties) {
        VerificationSender sender = properties.telegramEnabled()
                ? new TelegramGatewaySender(properties.telegramToken(), (int) properties.codeTtl().toSeconds())
                : new DevConsoleSender();
        log.info("Phone verification codes are delivered via {}", sender.name());
        return sender;
    }
}
