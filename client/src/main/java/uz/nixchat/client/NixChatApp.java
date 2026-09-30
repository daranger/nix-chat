package uz.nixchat.client;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import uz.nixchat.common.Protocol;

import java.net.URI;
import java.util.concurrent.ThreadLocalRandom;

/**
 * First milestone of the desktop client: one shared room connected to the server over WebSocket.
 */
public class NixChatApp extends Application {

    private final String nickname = "guest-" + ThreadLocalRandom.current().nextInt(1000, 10000);
    private final ListView<String> messages = new ListView<>();
    private final Label status = new Label();
    private ChatConnection connection;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        String host = getParameters().getNamed().getOrDefault("host", "localhost");
        int port = Integer.parseInt(getParameters().getNamed().getOrDefault("port",
                String.valueOf(Protocol.DEFAULT_PORT)));

        connection = new ChatConnection(
                URI.create(Protocol.wsUrl(host, port)),
                text -> Platform.runLater(() -> addMessage(text)),
                text -> Platform.runLater(() -> status.setText(text)));

        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(messages);
        root.setBottom(buildInputBar());

        stage.setTitle("NixChat");
        stage.setScene(new Scene(root, 420, 640));
        stage.show();

        connection.connect();
    }

    @Override
    public void stop() {
        if (connection != null) {
            connection.close();
        }
    }

    private HBox buildHeader() {
        Label title = new Label("NixChat");
        title.getStyleClass().add("title-3");
        Label me = new Label("You: " + nickname);
        me.getStyleClass().add("text-muted");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, title, me, spacer, status);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12));
        return header;
    }

    private HBox buildInputBar() {
        TextField input = new TextField();
        input.setPromptText("Message");
        HBox.setHgrow(input, Priority.ALWAYS);

        Button send = new Button("Send");
        send.setDefaultButton(true);
        send.setOnAction(e -> {
            String text = input.getText().strip();
            if (text.isEmpty() || !connection.isConnected()) {
                return;
            }
            connection.send(nickname + ": " + text);
            input.clear();
        });

        HBox bar = new HBox(8, input, send);
        bar.setPadding(new Insets(12));
        return bar;
    }

    private void addMessage(String text) {
        messages.getItems().add(text);
        messages.scrollTo(messages.getItems().size() - 1);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
