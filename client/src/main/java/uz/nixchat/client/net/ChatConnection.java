package uz.nixchat.client.net;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * WebSocket connection to the NixChat server, built on the JDK's own {@link java.net.http.WebSocket}.
 * Callbacks are invoked on a background thread: the UI must hop to the FX thread itself.
 */
public class ChatConnection {

    private final URI uri;
    private final String token;
    private final Consumer<String> onMessage;
    private final Consumer<String> onStatus;
    private volatile WebSocket socket;

    public ChatConnection(URI uri, String token, Consumer<String> onMessage, Consumer<String> onStatus) {
        this.uri = uri;
        this.token = token;
        this.onMessage = onMessage;
        this.onStatus = onStatus;
    }

    public void connect() {
        onStatus.accept("Connecting…");
        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .header("Authorization", "Bearer " + token)
                .buildAsync(uri, new Listener())
                .whenComplete((ws, error) -> {
                    if (error != null) {
                        onStatus.accept("Offline: " + rootMessage(error));
                    } else {
                        socket = ws;
                        onStatus.accept("Online");
                    }
                });
    }

    public boolean isConnected() {
        WebSocket ws = socket;
        return ws != null && !ws.isOutputClosed();
    }

    public void send(String text) {
        WebSocket ws = socket;
        if (ws != null) {
            ws.sendText(text, true);
        }
    }

    public void close() {
        WebSocket ws = socket;
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        }
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }

    private final class Listener implements WebSocket.Listener {

        // A message may arrive in several frames; collect them until "last" is true
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                onMessage.accept(buffer.toString());
                buffer.setLength(0);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            onStatus.accept("Disconnected");
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            onStatus.accept("Error: " + rootMessage(error));
        }
    }
}
