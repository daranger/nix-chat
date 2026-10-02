package uz.nixchat.client.ui;

import javafx.scene.control.ListCell;
import uz.nixchat.common.model.User;
import uz.nixchat.common.model.message.Message;
import uz.nixchat.common.model.message.SystemMessage;

/**
 * Draws one message in the chat feed.
 * The text comes from {@link Message#format()}, which each message type overrides in its own way.
 */
public class MessageCell extends ListCell<Message> {

    private static final String OWN_STYLE = "-fx-font-weight: bold;";
    private static final String SYSTEM_STYLE = "-fx-opacity: 0.6; -fx-font-style: italic;";

    private final User currentUser;

    public MessageCell(User currentUser) {
        this.currentUser = currentUser;
        setWrapText(true);
    }

    @Override
    protected void updateItem(Message message, boolean empty) {
        super.updateItem(message, empty);
        if (empty || message == null) {
            setText(null);
            setStyle("");
            return;
        }
        setText(message.format());
        if (message instanceof SystemMessage) {
            setStyle(SYSTEM_STYLE);
        } else if (message.isFrom(currentUser)) {
            setStyle(OWN_STYLE);
        } else {
            setStyle("");
        }
    }
}
