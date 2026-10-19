package explodingkittens;

import explodingkittens.cards.Card;
import explodingkittens.cards.CardFactory;
import explodingkittens.game.Deck;
import explodingkittens.game.ExpansionType;
import explodingkittens.game.GameContext;
import explodingkittens.game.InitGame;
import explodingkittens.online.Player;

import java.util.List;

import static explodingkittens.Assert.*;

/**
 * REQ 14/18: CardFactory is the extension point for future expansions.
 * Verifies base-deck counts (REQ 3), unknown-card rejection, extensibility,
 * and clean rejection of not-yet-implemented expansions.
 */
public class CardFactoryTest {

    /** Tiny test-local card used to demonstrate registering a new expansion card. */
    static class TestExpansionCard extends Card {
        TestExpansionCard() { super("TestExpansionCard"); }
        @Override
        public void executeEffect(GameContext context, Player actor, Player target) {
            // no effect - test vehicle only
        }
    }

    private int countByName(Deck deck, String name) {
        return (int) deck.getCards().stream()
                .filter(c -> name.equalsIgnoreCase(c.getName())).count();
    }

    /** REQ 3: factory-built base deck has the exact requirement-3 counts (4 players). */
    private void testBaseDeckExactCountsFourPlayers() {
        Deck deck = new InitGame().buildDeck(4, List.of(ExpansionType.BASE), new CardFactory());
        assertEquals(2, countByName(deck, "Defuse"), "REQ 3a: 2 Defuse for 2-4 players");
        assertEquals(4, countByName(deck, "Attack"), "REQ 3b: 4 Attack");
        assertEquals(4, countByName(deck, "Favor"), "REQ 3c: 4 Favor");
        assertEquals(5, countByName(deck, "Nope"), "REQ 3d: 5 Nope");
        assertEquals(4, countByName(deck, "Shuffle"), "REQ 3e: 4 Shuffle");
        assertEquals(4, countByName(deck, "Skip"), "REQ 3f: 4 Skip");
        assertEquals(5, countByName(deck, "SeeTheFuture"), "REQ 3g: 5 SeeTheFuture");
        assertEquals(4, countByName(deck, "HairyPotatoCat"), "REQ 3h: 4 HairyPotatoCat");
        assertEquals(4, countByName(deck, "Cattermelon"), "REQ 3i: 4 Cattermelon");
        assertEquals(4, countByName(deck, "RainbowRalphingCat"), "REQ 3j: 4 RainbowRalphingCat");
        assertEquals(4, countByName(deck, "TacoCat"), "REQ 3k: 4 TacoCat");
        assertEquals(4, countByName(deck, "OverweightBikiniCat"), "REQ 3l: 4 OverweightBikiniCat");
        assertEquals(46, deck.size(), "Base deck total for 2-4 players is 46");
    }

    /** REQ 3a: the 5-player branch leaves only 1 Defuse in the deck. */
    private void testBaseDeckExactCountsFivePlayers() {
        Deck deck = new InitGame().buildDeck(5, List.of(ExpansionType.BASE), new CardFactory());
        assertEquals(1, countByName(deck, "Defuse"), "REQ 3a: 1 Defuse for 5 players");
        assertEquals(45, deck.size(), "Base deck total for 5 players is 45");
    }

    /** Factory rejects unregistered card names. */
    private void testCreateUnknownCardThrows() {
        CardFactory factory = new CardFactory();
        boolean thrown = false;
        String message = "";
        try {
            factory.create("Unknown");
        } catch (IllegalArgumentException e) {
            thrown = true;
            message = e.getMessage();
        }
        assertTrue(thrown, "create() must throw for an unknown card name");
        assertTrue(message.contains("Unknown card"), "Message must name the unknown card: " + message);
    }

    /** All base card names are registered by default. */
    private void testAllBaseCardsRegistered() {
        CardFactory factory = new CardFactory();
        String[] baseNames = {"Defuse", "Attack", "Favor", "Nope", "Shuffle", "Skip",
                "SeeTheFuture", "HairyPotatoCat", "Cattermelon", "RainbowRalphingCat",
                "TacoCat", "OverweightBikiniCat"};
        for (String name : baseNames) {
            assertTrue(factory.isRegistered(name), name + " must be registered");
        }
    }

    /** REQ 14/18: a new expansion card can be added by registering it - no core changes. */
    private void testRegisterNewExpansionCard() {
        CardFactory factory = new CardFactory();
        factory.register("TestExpansionCard", TestExpansionCard::new);
        assertTrue(factory.isRegistered("TestExpansionCard"), "New card must be registered");
        Card created = factory.create("TestExpansionCard");
        assertTrue(created instanceof TestExpansionCard, "create() must return the registered card type");
    }

    /** Unimplemented expansions are rejected with an extension-point message. */
    private void testUnimplementedExpansionRejected() {
        InitGame initGame = new InitGame();
        boolean thrown = false;
        String message = "";
        try {
            initGame.buildDeck(4, List.of(ExpansionType.BARKING_KITTENS), new CardFactory());
        } catch (IllegalArgumentException e) {
            thrown = true;
            message = e.getMessage();
        }
        assertTrue(thrown, "Not-yet-implemented expansion must be rejected");
        assertTrue(message.contains("not yet implemented"),
                "Message must point at the extension point: " + message);
    }
}
