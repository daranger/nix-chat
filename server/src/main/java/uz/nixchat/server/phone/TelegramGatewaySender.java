package uz.nixchat.server.phone;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uz.nixchat.server.phone.PhoneExceptions.DeliveryFailedException;

import java.util.Map;

/**
 * Sends verification codes as Telegram messages through the Telegram Gateway API
 * (https://core.telegram.org/gateway). Costs $0.01 per delivered code; codes sent to your own number are free.
 * The recipient needs a Telegram account on that number.
 */
public class TelegramGatewaySender implements VerificationSender {

    private static final String BASE_URL = "https://gatewayapi.telegram.org";
    private static final int CODE_LENGTH = 6;

    private final RestClient http;
    private final int ttlSeconds;

    public TelegramGatewaySender(String accessToken, int ttlSeconds) {
        this.http = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public String send(String e164Phone) {
        Map<String, Object> result = call("/sendVerificationMessage", Map.of(
                "phone_number", e164Phone,
                "code_length", CODE_LENGTH,
                "ttl", ttlSeconds));
        Object requestId = result.get("request_id");
        if (requestId == null) {
            throw new DeliveryFailedException("Telegram Gateway returned no request_id", null);
        }
        return requestId.toString();
    }

    @Override
    public boolean check(String requestId, String code) {
        Map<String, Object> result = call("/checkVerificationStatus", Map.of(
                "request_id", requestId,
                "code", code == null ? "" : code.strip()));
        if (result.get("verification_status") instanceof Map<?, ?> status) {
            return "code_valid".equals(status.get("status"));
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(String method, Map<String, Object> body) {
        Map<String, Object> response;
        try {
            response = http.post()
                    .uri(method)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            throw new DeliveryFailedException("Telegram Gateway is not reachable", e);
        }
        if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
            Object error = response == null ? "empty response" : response.get("error");
            throw new DeliveryFailedException("Telegram could not deliver the code: " + error, null);
        }
        return (Map<String, Object>) response.get("result");
    }

    @Override
    public String name() {
        return "Telegram Gateway";
    }
}
