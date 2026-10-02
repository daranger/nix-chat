package uz.nixchat.client;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import uz.nixchat.client.net.ChatConnection;
import uz.nixchat.client.net.WireFormat;
import uz.nixchat.client.storage.AppSettings;
import uz.nixchat.client.storage.ContactStore;
import uz.nixchat.client.storage.FileMessageStorage;
import uz.nixchat.client.ui.ChatView;
import uz.nixchat.common.Protocol;
import uz.nixchat.common.model.User;
import uz.nixchat.common.model.message.Message;
import uz.nixchat.common.model.message.SystemMessage;
import uz.nixchat.common.model.message.TextMessage;
import uz.nixchat.common.storage.MessageStorage;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

/**
 * Entry point of the desktop client. Wires together settings, local storage, the network connection and the UI.
 * <p>
 * Command-line options: {@code --profile=alice} (separate settings and history, handy for running two clients
 * on one computer), {@code --host=...}, {@code --port=...}.
 */
public class NixChatApp extends Application {

    /** The shared room — the only chat until private and group chats reach the server. */
    private static final String GENERAL_CHAT = "general";
    private static final int HISTORY_SIZE = 100;

    private User me;
    private MessageStorage history;
    private ContactStore contacts;
    private ChatView view;
    private ChatConnection connection;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Map<String, String> args = getParameters().getNamed();
        Path profileDir = Path.of(System.getProperty("user.home"), ".nixchat", args.getOrDefault("profile", "default"));

        AppSettings settings = AppSettings.load(profileDir);
        applyCommandLine(settings, args);
        me = settings.getCurrentUser();

        // Polymorphism: the app only knows the MessageStorage interface, not the file-based implementation
        history = new FileMessageStorage(profileDir, settings.createCacheEncryptor());
        contacts = new ContactStore(profileDir);

        view = new ChatView(me, "General", this::send);
        for (Message message : history.findLast(GENERAL_CHAT, HISTORY_SIZE)) {
            view.showMessage(message);
        }
        view.showContacts(contacts.getAll());

        stage.setTitle("NixChat — " + me);
        stage.setScene(new Scene(view, 760, 560));
        stage.show();

        connect(settings.getServerHost(), settings.getServerPort());
    }

    private void applyCommandLine(AppSettings settings, Map<String, String> args) {
        String host = args.get("host");
        String port = args.get("port");
        if (host == null && port == null) {
            return;
        }
        int portNumber = settings.getServerPort();
        if (port != null) {
            try {
                portNumber = Integer.parseInt(port);
            } catch (NumberFormatException e) {
                System.err.println("--port must be a number, got '" + port + "'");
            }
        }
        settings.setServer(host != null ? host : settings.getServerHost(), portNumber);
        settings.save();
    }

    private void connect(String host, int port) {
        URI uri;
        // try-catch #3: URISyntaxException — the host from the settings may not form a valid address
        try {
            uri = new URI(Protocol.wsUrl(host, port));
        } catch (URISyntaxException e) {
            view.showMessage(new SystemMessage(GENERAL_CHAT, "Invalid server address: " + e.getInput()));
            view.setStatus("Offline");
            return;
        }

        connection = new ChatConnection(uri,
                raw -> Platform.runLater(() -> onIncoming(raw)),
                text -> Platform.runLater(() -> view.setStatus(text)));
        connection.connect();
    }

    private void onIncoming(String raw) {
        Message message = WireFormat.decode(raw, GENERAL_CHAT);
        view.showMessage(message);

        if (message instanceof TextMessage) {
            history.save(message);
            User sender = message.getSender();
            if (!message.isFrom(me)) {
                contacts.touch(sender, Instant.now());
                view.showContacts(contacts.getAll());
            }
        }
    }

    private void send(String text) {
        if (connection == null || !connection.isConnected()) {
            view.showMessage(new SystemMessage(GENERAL_CHAT, "Not connected to the server"));
            return;
        }
        try {
            TextMessage message = new TextMessage(GENERAL_CHAT, me, text);
            connection.send(WireFormat.encode(message));
            // Not saved here: the server echoes the message back, and onIncoming() stores it
        } catch (IllegalArgumentException e) {
            view.showMessage(new SystemMessage(GENERAL_CHAT, e.getMessage()));
        }
    }

    @Override
    public void stop() {
        if (connection != null) {
            connection.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
