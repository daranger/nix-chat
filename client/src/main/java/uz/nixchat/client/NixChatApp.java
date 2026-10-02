package uz.nixchat.client;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import uz.nixchat.client.net.ApiClient;
import uz.nixchat.client.net.ApiClient.Session;
import uz.nixchat.client.net.AuthException;
import uz.nixchat.client.net.ChatConnection;
import uz.nixchat.client.net.WireFormat;
import uz.nixchat.client.storage.AppSettings;
import uz.nixchat.client.storage.ContactStore;
import uz.nixchat.client.storage.FileMessageStorage;
import uz.nixchat.client.ui.ChatView;
import uz.nixchat.client.ui.LoginView;
import uz.nixchat.common.Protocol;
import uz.nixchat.common.model.User;
import uz.nixchat.common.model.message.Message;
import uz.nixchat.common.model.message.SystemMessage;
import uz.nixchat.common.model.message.TextMessage;
import uz.nixchat.common.storage.MessageStorage;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Entry point of the desktop client: sign in (or reuse a saved token), then open the chat.
 * <p>
 * Command-line options: {@code --profile=alice} (separate settings, login and history, handy for running two
 * clients on one computer), {@code --host=...}, {@code --port=...}.
 */
public class NixChatApp extends Application {

    /** The shared room — the only chat until private and group chats reach the server. */
    private static final String GENERAL_CHAT = "general";
    private static final int HISTORY_SIZE = 100;

    private final StackPane root = new StackPane();
    private Path profileDir;
    private AppSettings settings;
    private ApiClient api;

    private User me;
    private MessageStorage history;
    private ContactStore contacts;
    private ChatView view;
    private ChatConnection connection;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Map<String, String> args = getParameters().getNamed();
        profileDir = Path.of(System.getProperty("user.home"), ".nixchat", args.getOrDefault("profile", "default"));
        settings = AppSettings.load(profileDir);
        applyCommandLine(settings, args);
        api = new ApiClient(Protocol.httpUrl(settings.getServerHost(), settings.getServerPort()));

        stage.setTitle("NixChat");
        stage.setScene(new Scene(root, 760, 560));
        stage.show();

        String savedToken = settings.getAuthToken();
        if (savedToken == null) {
            showLogin(null);
        } else {
            resumeSession(savedToken);
        }
    }

    // ---------------------------------------------------------------- login

    private void showLogin(String message) {
        closeConnection();
        LoginView login = new LoginView(settings.getServerHost() + ":" + settings.getServerPort(), new LoginView.Listener() {
            @Override
            public void onSignIn(String username, String password) {
                authenticate(null, () -> api.login(username, password));
            }

            @Override
            public void onRegister(String username, String displayName, String password) {
                authenticate(null, () -> api.register(username, displayName, password));
            }
        });
        if (message != null) {
            login.showError(message);
        }
        root.getChildren().setAll(login);
    }

    /** A saved token: check it with the server before opening the chat. */
    private void resumeSession(String token) {
        authenticate(token, () -> api.me(token));
    }

    @FunctionalInterface
    private interface AuthCall {
        Session call() throws IOException, InterruptedException, AuthException;
    }

    /** Runs a login call in the background and switches screens with the result. */
    private void authenticate(String savedToken, AuthCall call) {
        LoginView login = root.getChildren().isEmpty() ? null
                : (root.getChildren().get(0) instanceof LoginView l ? l : null);
        if (login != null) {
            login.setBusy(true);
        }
        CompletableFuture.runAsync(() -> {
            try {
                Session session = call.call();
                Platform.runLater(() -> onSignedIn(session));
            } catch (AuthException e) {
                Platform.runLater(() -> {
                    if (savedToken != null && e.isUnauthorized()) {
                        settings.setAuthToken(null);
                        settings.save();
                        showLogin("Your session has expired, please sign in again");
                    } else {
                        showLoginError(e.getMessage());
                    }
                });
            } catch (ConnectException e) {
                Platform.runLater(() -> showLoginError("Server is not reachable. Is it running?"));
            } catch (IOException e) {
                Platform.runLater(() -> showLoginError("Network error: " + e.getMessage()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void showLoginError(String message) {
        if (!root.getChildren().isEmpty() && root.getChildren().get(0) instanceof LoginView login) {
            login.setBusy(false);
            login.showError(message);
        } else {
            showLogin(message);
        }
    }

    private void onSignedIn(Session session) {
        settings.setCurrentUser(session.username(), session.displayName());
        settings.setAuthToken(session.token());
        settings.save();
        openChat(session.token());
    }

    private void signOut() {
        settings.setAuthToken(null);
        settings.save();
        showLogin(null);
    }

    // ---------------------------------------------------------------- chat

    private void openChat(String token) {
        me = settings.getCurrentUser();
        ((Stage) root.getScene().getWindow()).setTitle("NixChat — " + me);

        // Polymorphism: the app only knows the MessageStorage interface, not the file-based implementation
        history = new FileMessageStorage(profileDir, settings.createCacheEncryptor());
        contacts = new ContactStore(profileDir);

        view = new ChatView(me, "General", this::send, this::signOut);
        for (Message message : history.findLast(GENERAL_CHAT, HISTORY_SIZE)) {
            view.showMessage(message);
        }
        view.showContacts(contacts.getAll());
        root.getChildren().setAll(view);

        connect(token);
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

    private void connect(String token) {
        URI uri;
        // try-catch #3: URISyntaxException — the host from the settings may not form a valid address
        try {
            uri = new URI(Protocol.wsUrl(settings.getServerHost(), settings.getServerPort()));
        } catch (URISyntaxException e) {
            view.showMessage(new SystemMessage(GENERAL_CHAT, "Invalid server address: " + e.getInput()));
            view.setStatus("Offline");
            return;
        }

        connection = new ChatConnection(uri, token,
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
        if (text.length() > Protocol.MAX_MESSAGE_LENGTH) {
            view.showMessage(new SystemMessage(GENERAL_CHAT,
                    "Message is longer than " + Protocol.MAX_MESSAGE_LENGTH + " characters"));
            return;
        }
        // The server adds our verified username and echoes the message back; onIncoming() stores it
        connection.send(text);
    }

    private void closeConnection() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    @Override
    public void stop() {
        closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
