package uz.nchat.common.games;

import uz.nchat.common.model.User;

/**
 * A game where players move one after another. Keeps the players and whose turn it is.
 */
public abstract class TurnBasedGame implements Game {

    private final User[] players;
    private int turn;

    protected TurnBasedGame(User... players) {
        if (players.length < 2) {
            throw new IllegalArgumentException("A game needs at least two players");
        }
        for (int i = 0; i < players.length; i++) {
            for (int j = i + 1; j < players.length; j++) {
                if (players[i].equals(players[j])) {
                    throw new IllegalArgumentException("Each player can join only once");
                }
            }
        }
        this.players = players.clone();
    }

    public User currentPlayer() {
        return players[turn];
    }

    protected int currentPlayerIndex() {
        return turn;
    }

    protected void nextTurn() {
        turn = (turn + 1) % players.length;
    }

    protected void checkTurn(User player) {
        if (isFinished()) {
            throw new IllegalStateException(getName() + " is already over");
        }
        if (!currentPlayer().equals(player)) {
            throw new IllegalStateException("It is " + currentPlayer().getUsername() + "'s turn");
        }
    }

    public User getPlayer(int index) {
        return players[index];
    }

    @Override
    public int getPlayerCount() {
        return players.length;
    }

    @Override
    public String status() {
        return isFinished() ? Game.super.status() : "Turn: " + currentPlayer().getUsername();
    }
}
