package uz.nchat.server.phone;

/**
 * Delivers a one-time code to a phone number and later checks what the user typed.
 * Implementations: {@link TelegramGatewaySender} (real codes in Telegram) and {@link DevConsoleSender} (development).
 */
public interface VerificationSender {

    /**
     * Sends a code to the number (E.164). The number is only used for this call and is not kept.
     *
     * @return an id of this verification request, used to check the code
     */
    String send(String e164Phone);

    /** True if the code matches the request. */
    boolean check(String requestId, String code);

    /** Shown in logs at startup. */
    String name();
}
