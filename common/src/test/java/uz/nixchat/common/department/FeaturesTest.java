package uz.nixchat.common.department;

import org.junit.jupiter.api.Test;
import uz.nixchat.common.games.Game;
import uz.nixchat.common.games.TicTacToe;
import uz.nixchat.common.model.User;
import uz.nixchat.common.model.message.TrackMessage;
import uz.nixchat.common.music.Playlist;
import uz.nixchat.common.music.Track;
import uz.nixchat.common.store.AppCategory;
import uz.nixchat.common.store.MiniApp;
import uz.nixchat.common.store.NixStore;
import uz.nixchat.common.wallet.InsufficientFundsException;
import uz.nixchat.common.wallet.Transaction;
import uz.nixchat.common.wallet.WalletService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** One feature per department: Wallet, Music, Games, Store. */
class FeaturesTest {

    private final User alice = new User("alice");
    private final User bob = new User("bob");

    // ---------- Wallet ----------

    @Test
    void transferMovesCoinsAndKeepsTotalSupply() throws InsufficientFundsException {
        WalletService wallets = new WalletService();
        wallets.openWallet(alice);
        wallets.openWallet(bob);

        Transaction t = wallets.transfer(alice, bob, 30, "for pizza");

        assertEquals(70, wallets.getBalance(alice));
        assertEquals(130, wallets.getBalance(bob));
        assertEquals(200, wallets.totalSupply());
        assertEquals("+30 NC from @alice", t.describeFor(bob));
        assertEquals(2, wallets.history(alice).size());
    }

    @Test
    void transferBeyondBalanceIsRefused() {
        WalletService wallets = new WalletService();
        wallets.openWallet(alice);
        wallets.openWallet(bob);

        InsufficientFundsException e = assertThrows(InsufficientFundsException.class,
                () -> wallets.transfer(alice, bob, 150, ""));
        assertEquals(50, e.getMissing());
        assertEquals(100, wallets.getBalance(alice));
        assertThrows(IllegalArgumentException.class, () -> wallets.transfer(alice, alice, 10, ""));
        assertThrows(IllegalArgumentException.class, () -> wallets.transfer(alice, bob, 0, ""));
    }

    // ---------- Music ----------

    @Test
    void playlistTracksDurationAndPermissions() {
        Playlist playlist = new Playlist("Road trip", alice);
        playlist.add(alice, new Track("Artist A", "Song 1", 225));
        playlist.add(alice, new Track("Artist B", "Song 2", 180));

        assertEquals("6:45", Track.formatDuration(playlist.totalDurationSeconds()));
        assertThrows(SecurityException.class, () -> playlist.add(bob, new Track("C", "D", 60)));
        playlist.invite(bob);
        playlist.add(bob, new Track("C", "D", 60));
        assertEquals(3, playlist.getTracks().size());
        assertEquals(3, playlist.shuffled(42).size());
    }

    @Test
    void trackMessagePreview() {
        TrackMessage message = new TrackMessage("general", alice, new Track("Artist A", "Song 1", 225));
        assertEquals("♪ Artist A — Song 1 (3:45)", message.preview());
        assertEquals("TRACK", message.getType());
    }

    // ---------- Games ----------

    @Test
    void ticTacToeDetectsWinner() {
        TicTacToe game = new TicTacToe(alice, bob);
        game.move(alice, 0, 0);
        game.move(bob, 1, 0);
        game.move(alice, 0, 1);
        game.move(bob, 1, 1);
        assertThrows(IllegalStateException.class, () -> game.move(bob, 2, 2));
        game.move(alice, 0, 2);

        assertTrue(game.isFinished());
        assertEquals(alice, game.getWinner());
        assertEquals("alice wins!", game.status());
        assertEquals("X X X\nO O .\n. . .\n", game.render());
    }

    @Test
    void ticTacToeDraw() {
        Game game = new TicTacToe(alice, bob);
        TicTacToe t = (TicTacToe) game;
        int[][] moves = {{0, 0}, {0, 1}, {0, 2}, {1, 1}, {1, 0}, {1, 2}, {2, 1}, {2, 0}, {2, 2}};
        for (int i = 0; i < moves.length; i++) {
            t.move(i % 2 == 0 ? alice : bob, moves[i][0], moves[i][1]);
        }
        assertTrue(t.isDraw());
        assertNull(t.getWinner());
        assertEquals("Draw", game.status());
    }

    // ---------- Store ----------

    @Test
    void storeInstallRateAndSearch() {
        NixStore store = new NixStore();
        store.publish(new MiniApp("ttt", "Tic-tac-toe", "NixChat Games", AppCategory.GAMES, "Play X and O in any chat"));
        store.publish(new MiniApp("coin-bot", "Coin Bot", "NixChat Wallet", AppCategory.FINANCE, "Splits bills in NixCoin"));

        assertThrows(IllegalStateException.class, () -> store.rate(alice, "ttt", 5));
        assertTrue(store.install(alice, "ttt"));
        assertFalse(store.install(alice, "ttt"));
        store.rate(alice, "ttt", 5);
        store.install(bob, "ttt");
        store.rate(bob, "ttt", 4);

        assertEquals(4.5, store.get("ttt").averageRating());
        assertEquals(List.of(store.get("ttt")), store.topRated(3));
        assertEquals(1, store.search("bills").size());
        assertEquals(1, store.byCategory(AppCategory.FINANCE).size());
        assertThrows(IllegalArgumentException.class, () -> store.rate(alice, "ttt", 6));
    }
}
