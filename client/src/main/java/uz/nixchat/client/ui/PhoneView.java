package uz.nixchat.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Link a phone number (only its hash is stored on the server) and find friends by their number.
 */
public class PhoneView extends VBox {

    public interface Listener {
        void onSendCode(String phone);

        void onConfirmCode(String code);

        void onUnlink();

        void onFindFriend(String phone);

        void onBack();
    }

    private final Label linkStatus = new Label();
    private final TextField phone = new TextField();
    private final Button sendCode = new Button("Send code");
    private final TextField code = new TextField();
    private final Button confirm = new Button("Confirm");
    private final Button unlink = new Button("Unlink number");
    private final Label message = new Label();
    private final TextField friendPhone = new TextField();
    private final Button find = new Button("Find");
    private final Label findResult = new Label();

    public PhoneView(Listener listener) {
        setSpacing(12);
        setPadding(new Insets(24));
        setMaxWidth(520);
        setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Phone number");
        title.getStyleClass().add("title-2");
        Label privacy = new Label("Your number is never stored. The server keeps only a one-way keyed hash, "
                + "so friends who have your number can find you, and nobody can read it back.");
        privacy.setWrapText(true);
        privacy.getStyleClass().add("text-muted");

        phone.setPromptText("+998 90 123 45 67");
        HBox.setHgrow(phone, Priority.ALWAYS);
        code.setPromptText("Code from Telegram");
        HBox.setHgrow(code, Priority.ALWAYS);
        code.setDisable(true);
        confirm.setDisable(true);
        sendCode.getStyleClass().add("accent");
        unlink.getStyleClass().add("danger");
        message.setWrapText(true);

        sendCode.setOnAction(e -> listener.onSendCode(phone.getText()));
        confirm.setOnAction(e -> listener.onConfirmCode(code.getText()));
        unlink.setOnAction(e -> listener.onUnlink());

        Label findTitle = new Label("Find a friend by phone");
        findTitle.getStyleClass().add("title-4");
        Label findNote = new Label("The number is hashed on this computer; only the hash is sent.");
        findNote.getStyleClass().add("text-muted");
        friendPhone.setPromptText("Friend's number");
        HBox.setHgrow(friendPhone, Priority.ALWAYS);
        find.setOnAction(e -> listener.onFindFriend(friendPhone.getText()));
        findResult.setWrapText(true);

        Button back = new Button("Back to chat");
        back.setOnAction(e -> listener.onBack());

        getChildren().addAll(
                back, title, privacy, linkStatus,
                new HBox(8, phone, sendCode),
                new HBox(8, code, confirm),
                message, unlink,
                new Separator(),
                findTitle, findNote,
                new HBox(8, friendPhone, find),
                findResult);
        setLinked(false);
    }

    public void setLinked(boolean linked) {
        linkStatus.setText(linked ? "Status: a number is linked to your account" : "Status: no number linked");
        unlink.setDisable(!linked);
    }

    public void showCodeSent(String maskedPhone, long expiresInSeconds) {
        code.setDisable(false);
        confirm.setDisable(false);
        code.clear();
        code.requestFocus();
        showMessage("Code sent to " + maskedPhone + ". It is valid for " + (expiresInSeconds / 60) + " min.", false);
    }

    public void showLinked() {
        code.clear();
        code.setDisable(true);
        confirm.setDisable(true);
        phone.clear();
        setLinked(true);
        showMessage("Number linked. Friends who have it can now find you.", false);
    }

    public void showMessage(String text, boolean error) {
        message.setText(text);
        message.setStyle(error ? "-fx-text-fill: -color-danger-fg;" : "-fx-text-fill: -color-success-fg;");
    }

    public void showFindResult(String text) {
        findResult.setText(text);
    }

    public void setBusy(boolean busy) {
        sendCode.setDisable(busy);
        find.setDisable(busy);
        if (busy) {
            confirm.setDisable(true);
        } else {
            confirm.setDisable(code.isDisabled());
        }
    }
}
