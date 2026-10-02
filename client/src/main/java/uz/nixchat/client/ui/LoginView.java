package uz.nixchat.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Sign-in and registration screen. One view, two modes: the link at the bottom switches between them.
 */
public class LoginView extends VBox {

    /** Called when the user presses the main button. */
    public interface Listener {
        void onSignIn(String username, String password);

        void onRegister(String username, String displayName, String password);
    }

    private final Label title = new Label();
    private final TextField username = new TextField();
    private final TextField displayName = new TextField();
    private final PasswordField password = new PasswordField();
    private final PasswordField confirm = new PasswordField();
    private final Label error = new Label();
    private final Button submit = new Button();
    private final Hyperlink switchMode = new Hyperlink();
    private final Label server = new Label();
    private boolean registerMode;

    public LoginView(String serverAddress, Listener listener) {
        setAlignment(Pos.CENTER);
        setSpacing(12);
        setPadding(new Insets(32));
        setMaxWidth(380);

        title.getStyleClass().add("title-2");
        username.setPromptText("Username");
        displayName.setPromptText("Display name (optional)");
        password.setPromptText("Password");
        confirm.setPromptText("Confirm password");
        error.setStyle("-fx-text-fill: -color-danger-fg;");
        error.setWrapText(true);
        submit.setDefaultButton(true);
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.getStyleClass().add("accent");
        server.getStyleClass().add("text-muted");
        server.setText("Server: " + serverAddress);

        submit.setOnAction(e -> {
            error.setText("");
            String user = username.getText().strip();
            if (user.isEmpty() || password.getText().isEmpty()) {
                showError("Enter your username and password");
                return;
            }
            if (registerMode) {
                if (!password.getText().equals(confirm.getText())) {
                    showError("Passwords do not match");
                    return;
                }
                listener.onRegister(user, displayName.getText().strip(), password.getText());
            } else {
                listener.onSignIn(user, password.getText());
            }
        });
        switchMode.setOnAction(e -> setRegisterMode(!registerMode));

        setRegisterMode(false);
    }

    private void setRegisterMode(boolean register) {
        registerMode = register;
        error.setText("");
        title.setText(register ? "Create your NixChat account" : "Sign in to NixChat");
        submit.setText(register ? "Create account" : "Sign in");
        switchMode.setText(register ? "Already have an account? Sign in" : "New to NixChat? Create an account");
        if (register) {
            getChildren().setAll(title, username, displayName, password, confirm, error, submit, switchMode, server);
        } else {
            getChildren().setAll(title, username, password, error, submit, switchMode, server);
        }
    }

    public void showError(String message) {
        error.setText(message);
    }

    public void setBusy(boolean busy) {
        submit.setDisable(busy);
        switchMode.setDisable(busy);
        submit.setText(busy ? "Please wait…" : (registerMode ? "Create account" : "Sign in"));
    }

    public void prefillUsername(String value) {
        username.setText(value);
    }
}
