package uz.nixchat.common.games;

/**
 * Any game that can be played inside a chat.
 */
public interface Game {

    String getName();

    int getPlayerCount();

    boolean isFinished();

    /** The game state as text, shown in the chat after every move. */
    String render();

    /** Short status line: whose turn it is, or who won. */
    default String status() {
        return isFinished() ? getName() + " is over" : getName() + " in progress";
    }
}
