package uz.nixchat.common.games;

import uz.nixchat.common.model.User;

/**
 * Classic 3×3 tic-tac-toe. The first player is X, the second is O.
 */
public class TicTacToe extends TurnBasedGame {

    public static final int SIZE = 3;
    private static final char EMPTY = '.';
    private static final char[] MARKS = {'X', 'O'};

    private final char[][] board = new char[SIZE][SIZE];
    private User winner;
    private int movesMade;

    public TicTacToe(User x, User o) {
        super(x, o);
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                board[row][col] = EMPTY;
            }
        }
    }

    /** Places the current player's mark. Rows and columns are numbered 0–2. */
    public void move(User player, int row, int col) {
        checkTurn(player);
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            throw new IllegalArgumentException("Cell must be inside the 3×3 board");
        }
        if (board[row][col] != EMPTY) {
            throw new IllegalArgumentException("Cell " + row + "," + col + " is already taken");
        }
        char mark = MARKS[currentPlayerIndex()];
        board[row][col] = mark;
        movesMade++;
        if (hasLine(mark)) {
            winner = player;
        } else if (!isFinished()) {
            nextTurn();
        }
    }

    private boolean hasLine(char mark) {
        for (int i = 0; i < SIZE; i++) {
            if (board[i][0] == mark && board[i][1] == mark && board[i][2] == mark) {
                return true;
            }
            if (board[0][i] == mark && board[1][i] == mark && board[2][i] == mark) {
                return true;
            }
        }
        boolean diagonal = board[0][0] == mark && board[1][1] == mark && board[2][2] == mark;
        boolean antiDiagonal = board[0][2] == mark && board[1][1] == mark && board[2][0] == mark;
        return diagonal || antiDiagonal;
    }

    public User getWinner() {
        return winner;
    }

    public boolean isDraw() {
        return winner == null && movesMade == SIZE * SIZE;
    }

    @Override
    public boolean isFinished() {
        return winner != null || movesMade == SIZE * SIZE;
    }

    @Override
    public String getName() {
        return "Tic-tac-toe";
    }

    @Override
    public String render() {
        StringBuilder text = new StringBuilder();
        for (char[] row : board) {
            for (int col = 0; col < SIZE; col++) {
                text.append(row[col]);
                if (col < SIZE - 1) {
                    text.append(' ');
                }
            }
            text.append('\n');
        }
        return text.toString();
    }

    @Override
    public String status() {
        if (winner != null) {
            return winner.getUsername() + " wins!";
        }
        if (isDraw()) {
            return "Draw";
        }
        return "Turn: " + currentPlayer().getUsername() + " (" + MARKS[currentPlayerIndex()] + ")";
    }
}
