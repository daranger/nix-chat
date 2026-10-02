package uz.nixchat.server.phone;

public final class PhoneExceptions {

    private PhoneExceptions() {
    }

    /** 409: the number is already linked to another account. */
    public static class PhoneTakenException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public PhoneTakenException() {
            super("This phone number is already linked to another account");
        }
    }

    /** 429: too many codes or lookups in a short time. */
    public static class TooManyRequestsException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TooManyRequestsException(String message) {
            super(message);
        }
    }

    /** 503: the code could not be delivered (Telegram Gateway unreachable or refused). */
    public static class DeliveryFailedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public DeliveryFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
