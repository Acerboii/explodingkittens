package explodingkittens;

import explodingkittens.cards.AttackCard;
import explodingkittens.cards.DefuseCard;
import explodingkittens.cards.NopeCard;
import explodingkittens.cards.SkipCard;
import explodingkittens.game.Action;
import explodingkittens.game.Deck;
import explodingkittens.game.Game;
import explodingkittens.online.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static explodingkittens.Assert.*;

/**
 * Unit tests for the bot decision behavior (REQ 16).
 *
 * A bot no longer always passes: it plays a random available card from its
 * hand. A Nope is never picked because it can only be played within the
 * interrupt window; when nothing is playable the bot passes.
 */
public class BotActionTest {

    /** REQ 16: a bot with playable cards returns a card number, never "Pass" */
    private void testBotPlaysRandomCardInsteadOfPassing() {
        Player bot = new Player(0, false, true, null);
        bot.addCard(new DefuseCard());
        bot.addCard(new SkipCard());

        String action = bot.decideBotAction();

        assertFalse("Pass".equalsIgnoreCase(action), "Bot with playable cards must not pass");
        assertTrue("1".equals(action) || "2".equals(action),
                "Bot must return a 1-based card position in its hand, got: " + action);
    }

    /** REQ 16: repeated decisions vary - the choice is random, not fixed */
    private void testBotDecisionIsRandomOverManyCalls() {
        Player bot = new Player(0, false, true, null);
        bot.addCard(new DefuseCard());
        bot.addCard(new SkipCard());

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 300; i++) {
            seen.add(bot.decideBotAction());
        }

        assertTrue(seen.contains("1") && seen.contains("2"),
                "Over 300 calls the bot should have picked both cards, saw: " + seen);
    }

    /** REQ 16 / REQ 12: a Nope can only be played within the interrupt window,
     * so it is never part of the bot's random selection */
    private void testBotNeverPicksNope() {
        Player bot = new Player(0, false, true, null);
        bot.addCard(new NopeCard());
        bot.addCard(new SkipCard());

        for (int i = 0; i < 100; i++) {
            assertEquals("2", bot.decideBotAction(),
                    "Bot must always pick the Skip (position 2), never the Nope");
        }
    }

    /** REQ 16: when the only card in hand is a Nope, the bot passes */
    private void testBotWithOnlyNopePasses() {
        Player bot = new Player(0, false, true, null);
        bot.addCard(new NopeCard());

        assertEquals("Pass", bot.decideBotAction(),
                "A bot whose hand holds only a Nope must pass");
    }

    /** REQ 16: an empty hand means nothing is playable - the bot passes */
    private void testBotWithEmptyHandPasses() {
        Player bot = new Player(0, false, true, null);

        assertEquals("Pass", bot.decideBotAction(),
                "A bot with an empty hand must pass");
    }

    /** REQ 16: receiveAction() delegates to the bot decision (wiring check) */
    private void testReceiveActionDelegatesToBotDecision() {
        Player bot = new Player(0, false, true, null);
        bot.addCard(new SkipCard());

        assertEquals("1", bot.receiveAction(),
                "receiveAction() for a bot must return its decided card number");
    }

    /** REQ 16: the bot's action string actually drives a game turn -
     * playing the chosen card removes it from the hand and discards it */
    private void testBotActionDrivesGameTurn() {
        List<Player> players = new ArrayList<>();
        Player bot = new Player(0, false, true, null);
        Player human = new Player(1, false, false, null);
        players.add(bot);
        players.add(human);

        AttackCard attack = new AttackCard();
        bot.addCard(attack);

        Deck deck = new Deck();
        Game game = new Game(players, deck);
        startAt(game, 0);

        String cmd = bot.receiveAction();
        assertFalse("Pass".equalsIgnoreCase(cmd), "Bot with an Attack in hand must not pass");
        int index = Integer.parseInt(cmd.trim()) - 1;
        Action action = Action.play(bot.getHand().get(index));

        game.processTurn(bot, action);

        assertFalse(bot.getHand().contains(attack), "Played card must leave the bot's hand");
        assertTrue(game.getDiscardPile().getCards().contains(attack),
                "Played card must be in the discard pile");
        assertEquals(1, game.getActivePlayerIndex(), "Attack ends the turn - turn moves on");
        assertEquals(2, game.getTurnsToTake(), "Attacked player should take 2 turns");
    }

    /** Starts the game and forces a deterministic first player. */
    private static void startAt(Game game, int firstPlayerIndex) {
        game.start();
        game.setActivePlayerIndex(firstPlayerIndex);
    }
}
