package explodingkittens;

import explodingkittens.cards.AttackCard;
import explodingkittens.cards.Card;
import explodingkittens.cards.NopeCard;
import explodingkittens.cards.SkipCard;
import explodingkittens.game.Action;
import explodingkittens.game.Deck;
import explodingkittens.game.Game;
import explodingkittens.game.GameContext;
import explodingkittens.online.Player;

import java.util.List;

import static explodingkittens.Assert.*;

/**
 * REQ 12: Deterministic tests for the 5-second Nope interrupt window.
 * A fake clock ({@link ManualClock}) is injected into {@link Game} so the
 * window can be opened, probed and expired without real waiting.
 *
 * <p>The deferred-effect vehicle is a tiny test-local {@link FlagCard} whose
 * effect sets an observable flag - every non-turn-ending card takes the same
 * deferred path in {@code Game.processTurn}.</p>
 */
public class NopeInterruptWindowTest {

    /** Test vehicle: a card whose deferred effect sets an observable flag. */
    static class FlagCard extends Card {
        boolean applied;
        FlagCard() { super("FlagCard"); }
        @Override
        public void executeEffect(GameContext context, Player actor, Player target) {
            applied = true;
        }
    }

    private static Player p(int id) { return new Player(id, false, null); }

    /** Creates a started game with the given players, active player 0, using the fake clock. */
    private static Game startedGameAt(ManualClock clock, Player... ps) {
        List<Player> players = List.of(ps);
        Game game = new Game(players, new Deck());
        game.setClock(clock);
        game.start();
        game.setActivePlayerIndex(0);
        return game;
    }

    /** REQ 12: A Nope inside the window stops a deferred card effect. */
    private void testNopeInsideWindowStopsDeferredEffect() {
        Player p1 = p(0);
        Player p2 = p(1);
        FlagCard flag = new FlagCard();
        NopeCard nope = new NopeCard();
        p1.addCard(flag);
        p2.addCard(nope);

        ManualClock clock = new ManualClock();
        Game game = startedGameAt(clock, p1, p2);

        // p1 plays the card - the effect is DEFERRED while the window is open
        game.processTurn(p1, Action.play(flag));
        assertTrue(game.hasPendingEffect(), "Card effect should be pending while window is open");
        assertFalse(flag.applied, "Deferred effect must not run before the window closes");

        // p2 plays Nope 1 second later - inside the window
        clock.advance(1000);
        game.processTurn(p2, Action.play(nope));

        assertFalse(flag.applied, "Nope must stop the deferred card effect");
        assertTrue(game.hasPendingEffect(), "The Nope chain stays open so further Nopes can toggle it (REQ 12a)");
        assertEquals(1, game.getDiscardPile().getNopeCount(), "Nope should be in the discard pile");

        // Window expires with an odd number of Nopes - resolution keeps the effect stopped
        clock.advance(4000);
        game.resolvePendingEffect();
        assertFalse(flag.applied, "Resolution with a single Nope must keep the effect stopped");
        assertFalse(game.hasPendingEffect(), "Chain should be closed after resolution");
    }

    /** REQ 12: A Nope played after the 5-second window closed is rejected. */
    private void testNopeOutsideWindowRejected() {
        Player p1 = p(0);
        FlagCard flag = new FlagCard();
        NopeCard nope = new NopeCard();
        p1.addCard(flag);
        p1.addCard(nope);

        ManualClock clock = new ManualClock();
        Game game = startedGameAt(clock, p1, p(1));

        game.processTurn(p1, Action.play(flag));
        clock.advance(5001); // the 5000 ms window has expired

        game.processTurn(p1, Action.play(nope));

        assertTrue(p1.getHand().contains(nope), "Nope outside the window must stay in hand");
        assertEquals(0, game.getDiscardPile().getNopeCount(), "Rejected Nope must not reach the discard pile");
    }

    /** REQ 12: The interrupt window is open for exactly 5 seconds. */
    private void testWindowClosesAfterFiveSeconds() {
        Player p1 = p(0);
        FlagCard flag = new FlagCard();
        p1.addCard(flag);

        ManualClock clock = new ManualClock();
        Game game = startedGameAt(clock, p1, p(1));

        assertFalse(game.isInterruptWindowOpen(), "No window before any card play");

        game.processTurn(p1, Action.play(flag));
        assertTrue(game.isInterruptWindowOpen(), "Window should be open right after a card play");
        assertEquals(5000, game.getInterruptWindowRemainingMs(), "Full 5 seconds remaining at t=0");

        clock.advance(4999);
        assertTrue(game.isInterruptWindowOpen(), "Window still open at 4999 ms");
        assertEquals(1, game.getInterruptWindowRemainingMs(), "1 ms remaining at 4999 ms");

        clock.advance(1);
        assertTrue(game.isInterruptWindowOpen(), "Window open at exactly 5000 ms (boundary)");

        clock.advance(1);
        assertFalse(game.isInterruptWindowOpen(), "Window closed at 5001 ms");
        assertEquals(0, game.getInterruptWindowRemainingMs(), "0 ms remaining after expiry");
    }

    /** REQ 12: A deferred effect applies once the window has expired without a Nope. */
    private void testDeferredEffectAppliesWhenWindowExpires() {
        Player p1 = p(0);
        FlagCard flag = new FlagCard();
        p1.addCard(flag);

        ManualClock clock = new ManualClock();
        Game game = startedGameAt(clock, p1, p(1));

        game.processTurn(p1, Action.play(flag));
        clock.advance(5001); // nobody played Nope - window expires

        // The next action resolves the pending effect first
        game.processTurn(p1, null);

        assertTrue(flag.applied, "Deferred effect should apply after the window expires");
        assertFalse(game.hasPendingEffect(), "Pending effect should be resolved");
    }

    /** REQ 12: An in-window Nope undoes an already-applied Attack. */
    private void testNopeUndoesAppliedAttack() {
        Player p1 = p(0);
        Player p2 = p(1);
        AttackCard attack = new AttackCard();
        NopeCard nope = new NopeCard();
        p1.addCard(attack);
        p2.addCard(nope);

        ManualClock clock = new ManualClock();
        Game game = startedGameAt(clock, p1, p2);

        // Attack applies immediately (endTurn needs its state)
        game.processTurn(p1, Action.play(attack));
        assertEquals(1, game.getActivePlayerIndex(), "Attack should advance to the next player");
        assertEquals(2, game.getTurnsToTake(), "Attacked player should take 2 turns");

        // Nope 2 seconds later undoes the Attack
        clock.advance(2000);
        game.processTurn(p2, Action.play(nope));

        assertEquals(1, game.getTurnsToTake(), "Nope must cancel the extra turns granted by Attack");
        assertEquals(1, game.getDiscardPile().getNopeCount(), "Nope should be in the discard pile");
    }

    /** REQ 12a: A second Nope inside the window alternates to Yup and restores the effect. */
    private void testNopeOnNopeRestoresBlockedEffect() {
        Player p1 = p(0);
        Player p2 = p(1);
        FlagCard flag = new FlagCard();
        NopeCard n1 = new NopeCard();
        NopeCard n2 = new NopeCard();
        p1.addCard(flag);
        p2.addCard(n1);
        p2.addCard(n2);

        ManualClock clock = new ManualClock();
        Game game = startedGameAt(clock, p1, p2);

        game.processTurn(p1, Action.play(flag));

        clock.advance(1000);
        game.processTurn(p2, Action.play(n1)); // Nope - blocks the effect
        assertFalse(flag.applied, "First Nope should block the deferred effect");

        clock.advance(1000);
        game.processTurn(p2, Action.play(n2)); // Nope on top of Nope - becomes Yup
        assertFalse(flag.applied, "The effect is still deferred while the chain is open");

        // Window expires with an even number of Nopes - resolution applies the effect
        clock.advance(3000);
        game.resolvePendingEffect();
        assertTrue(flag.applied, "Second Nope (Yup) should restore the blocked effect at resolution");
        assertEquals(2, game.getDiscardPile().getNopeCount(), "Both Nopes should be in the discard pile");
    }

    /** REQ 12a: A third Nope inside the window alternates back to Nope and blocks again. */
    private void testThirdNopeBlocksAgain() {
        Player p1 = p(0);
        Player p2 = p(1);
        FlagCard flag = new FlagCard();
        NopeCard n1 = new NopeCard();
        NopeCard n2 = new NopeCard();
        NopeCard n3 = new NopeCard();
        p1.addCard(flag);
        p2.addCard(n1);
        p2.addCard(n2);
        p2.addCard(n3);

        ManualClock clock = new ManualClock();
        Game game = startedGameAt(clock, p1, p2);

        game.processTurn(p1, Action.play(flag));
        clock.advance(1000);
        game.processTurn(p2, Action.play(n1)); // Nope - blocks the effect
        clock.advance(1000);
        game.processTurn(p2, Action.play(n2)); // Yup - restores it
        clock.advance(1000);
        game.processTurn(p2, Action.play(n3)); // Nope - blocks it again

        assertFalse(flag.applied, "Effect must stay deferred while the chain is open");
        assertTrue(game.hasPendingEffect(), "Chain must still be open after a third Nope (REQ 12a)");

        clock.advance(1000);
        game.resolvePendingEffect();
        assertFalse(flag.applied, "Odd number of Nopes (3) must keep the effect stopped");
        assertEquals(3, game.getDiscardPile().getNopeCount(), "All three Nopes should be in the discard pile");
    }

    /** REQ 12a: A fourth Nope inside the window alternates back to Yup and restores again. */
    private void testFourthNopeRestoresAgain() {
        Player p1 = p(0);
        Player p2 = p(1);
        FlagCard flag = new FlagCard();
        NopeCard n1 = new NopeCard();
        NopeCard n2 = new NopeCard();
        NopeCard n3 = new NopeCard();
        NopeCard n4 = new NopeCard();
        p1.addCard(flag);
        p2.addCard(n1);
        p2.addCard(n2);
        p2.addCard(n3);
        p2.addCard(n4);

        ManualClock clock = new ManualClock();
        Game game = startedGameAt(clock, p1, p2);

        game.processTurn(p1, Action.play(flag));
        clock.advance(1000);
        game.processTurn(p2, Action.play(n1)); // Nope - blocks
        clock.advance(1000);
        game.processTurn(p2, Action.play(n2)); // Yup - restores
        clock.advance(1000);
        game.processTurn(p2, Action.play(n3)); // Nope - blocks
        clock.advance(1000);
        game.processTurn(p2, Action.play(n4)); // Yup - restores

        assertFalse(flag.applied, "Effect must stay deferred while the chain is open");

        clock.advance(1000);
        game.resolvePendingEffect();
        assertTrue(flag.applied, "Even number of Nopes (4) must let the effect take place");
        assertEquals(4, game.getDiscardPile().getNopeCount(), "All four Nopes should be in the discard pile");
    }

    /** REQ 12a: An Attack's granted turns alternate on/off with each in-window Nope. */
    private void testAttackNopeChainAlternatesIndefinitely() {
        Player p1 = p(0);
        Player p2 = p(1);
        AttackCard attack = new AttackCard();
        NopeCard n1 = new NopeCard();
        NopeCard n2 = new NopeCard();
        NopeCard n3 = new NopeCard();
        p1.addCard(attack);
        p2.addCard(n1);
        p2.addCard(n2);
        p2.addCard(n3);

        ManualClock clock = new ManualClock();
        Game game = startedGameAt(clock, p1, p2);

        game.processTurn(p1, Action.play(attack));
        assertEquals(1, game.getActivePlayerIndex(), "Attack should advance to the next player");
        assertEquals(2, game.getTurnsToTake(), "Attacked player should take 2 turns");

        clock.advance(1000);
        game.processTurn(p2, Action.play(n1)); // Nope - stops the Attack
        assertEquals(1, game.getTurnsToTake(), "First Nope must cancel the extra turns");

        clock.advance(1000);
        game.processTurn(p2, Action.play(n2)); // Yup - restores the Attack
        assertEquals(2, game.getTurnsToTake(), "Second Nope (Yup) must restore the extra turns");

        clock.advance(1000);
        game.processTurn(p2, Action.play(n3)); // Nope - stops it again
        assertEquals(1, game.getTurnsToTake(), "Third Nope must cancel the extra turns again");

        assertEquals(1, game.getActivePlayerIndex(), "The attacked player stays active throughout the chain");
        assertEquals(3, game.getDiscardPile().getNopeCount(), "All three Nopes should be in the discard pile");
    }

    /** REQ 12a: A Skip toggles between players with each Nope; a blocked Skip settles like a pass. */
    private void testSkipNopeChainAlternatesAndSettles() {
        Player p1 = p(0);
        Player p2 = p(1);
        SkipCard skip = new SkipCard();
        NopeCard n1 = new NopeCard();
        NopeCard n2 = new NopeCard();
        NopeCard n3 = new NopeCard();
        Card drawable = new NopeCard(); // card drawn by the skipper when the Skip settles
        p1.addCard(skip);
        p2.addCard(n1);
        p2.addCard(n2);
        p2.addCard(n3);

        ManualClock clock = new ManualClock();
        Deck deck = new Deck();
        deck.add(drawable);
        Game game = new Game(List.of(p1, p2), deck);
        game.setClock(clock);
        game.start();
        game.setActivePlayerIndex(0);

        game.processTurn(p1, Action.play(skip));
        int p1HandAfterSkip = p1.getHand().size();
        assertEquals(1, game.getActivePlayerIndex(), "Skip should advance to the next player");

        clock.advance(1000);
        game.processTurn(p2, Action.play(n1)); // Nope - stops the Skip
        assertEquals(0, game.getActivePlayerIndex(), "First Nope must restore the skipper's turn");

        clock.advance(1000);
        game.processTurn(p2, Action.play(n2)); // Yup - restores the Skip
        assertEquals(1, game.getActivePlayerIndex(), "Second Nope (Yup) must end the skipper's turn again");

        clock.advance(1000);
        game.processTurn(p2, Action.play(n3)); // Nope - stops it again
        assertEquals(0, game.getActivePlayerIndex(), "Third Nope must restore the skipper's turn again");

        // Window expires with an odd number of Nopes - the blocked Skip settles like a pass
        clock.advance(1000);
        game.resolvePendingEffect();
        assertEquals(p1HandAfterSkip + 1, p1.getHand().size(),
                "Blocked Skip: the skipper draws a card like a normal pass");
        assertEquals(1, game.getActivePlayerIndex(), "After settling, the turn moves on to the next player");
        assertEquals(3, game.getDiscardPile().getNopeCount(), "All three Nopes should be in the discard pile");
    }
}
