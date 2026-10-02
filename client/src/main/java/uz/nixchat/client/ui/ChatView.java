package uz.nixchat.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import uz.nixchat.client.storage.Contact;
import uz.nixchat.common.model.User;
import uz.nixchat.common.model.message.Message;

import java.util.List;
import java.util.function.Consumer;

/**
 * Main window layout: contacts on the left, chat feed in the middle, input bar at the bottom.
 */
public class ChatView extends BorderPane {

    private static final double SIDEBAR_WIDTH = 200;

    private final ListView<Message> feed = new ListView<>();
    private final ListView<Contact> contacts = new ListView<>();
    private final Label status = new Label();

    public ChatView(User currentUser, String chatTitle, Consumer<String> onSend) {
        feed.setCellFactory(list -> new MessageCell(currentUser));
        contacts.setCellFactory(list -> new ContactCell());

        setTop(buildHeader(currentUser, chatTitle));
        setLeft(buildSidebar());
        setCenter(feed);
        setBottom(buildInputBar(onSend));
    }

    private HBox buildHeader(User currentUser, String chatTitle) {
        Label title = new Label(chatTitle);
        title.getStyleClass().add("title-3");
        Label me = new Label(currentUser.toString());
        me.getStyleClass().add("text-muted");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, title, me, spacer, status);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12));
        return header;
    }

    private VBox buildSidebar() {
        Label caption = new Label("Contacts");
        caption.getStyleClass().add("text-muted");
        VBox.setVgrow(contacts, Priority.ALWAYS);

        VBox sidebar = new VBox(8, caption, contacts);
        sidebar.setPadding(new Insets(0, 0, 0, 12));
        sidebar.setPrefWidth(SIDEBAR_WIDTH);
        return sidebar;
    }

    private HBox buildInputBar(Consumer<String> onSend) {
        TextField input = new TextField();
        input.setPromptText("Message");
        HBox.setHgrow(input, Priority.ALWAYS);

        Button send = new Button("Send");
        send.setDefaultButton(true);
        send.setOnAction(event -> {
            String text = input.getText().strip();
            if (!text.isEmpty()) {
                onSend.accept(text);
                input.clear();
            }
        });

        HBox bar = new HBox(8, input, send);
        bar.setPadding(new Insets(12));
        return bar;
    }

    public void showMessage(Message message) {
        feed.getItems().add(message);
        feed.scrollTo(feed.getItems().size() - 1);
    }

    public void showContacts(List<Contact> list) {
        contacts.getItems().setAll(list);
    }

    public void setStatus(String text) {
        status.setText(text);
    }
}
