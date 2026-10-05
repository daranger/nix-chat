package uz.nchat.common;

/**
 * Constants shared by the server and the client.
 * Anything both sides must agree on (endpoints, ports, limits) lives here.
 */
public final class Protocol {

    /** Default port of the Nchat server. */
    public static final int DEFAULT_PORT = 8080;

    /** WebSocket endpoint for real-time messaging. */
    public static final String WS_PATH = "/ws";

    /** Maximum length of a single text message, in characters. */
    public static final int MAX_MESSAGE_LENGTH = 4096;

    private Protocol() {
    }

    /** Builds the base URL of the REST API, e.g. {@code http://localhost:8080}. */
    public static String httpUrl(String host, int port) {
        return "http://" + host + ":" + port;
    }

    /** Builds the WebSocket URL for a given host, e.g. {@code ws://localhost:8080/ws}. */
    public static String wsUrl(String host, int port) {
        return "ws://" + host + ":" + port + WS_PATH;
    }
}
