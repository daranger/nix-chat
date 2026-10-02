package uz.nixchat.client.ui;

import javafx.scene.control.ListCell;
import uz.nixchat.client.storage.Contact;

import java.time.Instant;

/**
 * Draws one contact in the sidebar: name and "last seen" time.
 */
public class ContactCell extends ListCell<Contact> {

    @Override
    protected void updateItem(Contact contact, boolean empty) {
        super.updateItem(contact, empty);
        if (empty || contact == null) {
            setText(null);
            return;
        }
        setText(contact.user().getDisplayName() + "\n" + contact.lastSeenText(Instant.now()));
    }
}
