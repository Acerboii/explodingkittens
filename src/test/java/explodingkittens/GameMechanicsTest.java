package explodingkittens;

import explodingkittens.cards.*;
import explodingkittens.game.*;
import explodingkittens.online.Player;

import java.util.ArrayList;
import java.util.List;

import static explodingkittens.Assert.*;

/**
 * Functional unit tests for core game rules and mechanics.
 */
public class GameMechanicsTest {

    public void testPlayerSetupAndDefuseCard() {
        InitGame initGame = new InitGame();
        List<Player> players = initGame.setupPlayers(2, 1);

        assertEquals(3, players.size(), "Total players should be 3 (2 human, 1 bot)");
        for (Player p : players) {
            assertTrue(p.hasCard("Defuse"), "Every player must be given 1 Defuse card initially");
        }
    }

    public void testPlayerSetupBounds() {
        InitGame initGame = new InitGame();
        assertThrows(IllegalArgumentException.class, () -> initGame.setupPlayers(1, 0), "Fewer than 2 players must fail");
        assertThrows(IllegalArgumentException.class, () -> initGame.setupPlayers(4, 2), "More than 5 players must fail");
    }

    public void testBuildDeckWithExpansions() {
        InitGame initGame = new InitGame();
        Deck baseDeck = initGame.buildDeck(List.of(ExpansionType.BASE));
        assertFalse(baseDeck.isEmpty());

        Deck expandedDeck = initGame.buildDeck(List.of(
                ExpansionType.BASE,
                ExpansionType.IMPLODING_KITTENS,
                ExpansionType.STREAKING_KITTENS,
                ExpansionType.BARKING_KITTENS
        ));

        boolean hasImploding = expandedDeck.getCards().stream().anyMatch(c -> c instanceof ImplodingKittenCard);
        boolean hasStreaking = expandedDeck.getCards().stream().anyMatch(c -> c instanceof StreakingKittenCard);
        boolean hasBarking = expandedDeck.getCards().stream().anyMatch(c -> c instanceof BarkingKittenCard);

        assertTrue(hasImploding, "Expanded deck must contain ImplodingKittenCard");
        assertTrue(hasStreaking, "Expanded deck must contain StreakingKittenCard");
        assertTrue(hasBarking, "Expanded deck must contain BarkingKittenCard");
    }

    public void testDiscardPileNopeCount() {
        DiscardPile discardPile = new DiscardPile();
        assertEquals(0, discardPile.getNopeCount());

        discardPile.add(new AttackCard());
        assertEquals(0, discardPile.getNopeCount());

        discardPile.add(new NopeCard());
        assertEquals(1, discardPile.getNopeCount());

        discardPile.add(new NopeCard());
        assertEquals(2, discardPile.getNopeCount());

        discardPile.add(new DefuseCard());
        assertEquals(0, discardPile.getNopeCount(), "Top card is not Nope, so count resets to 0");
    }

    public void testDeckDrawAndInsertAt() {
        Deck deck = new Deck();
        Card card1 = new AttackCard();
        Card card2 = new NopeCard();
        Card card3 = new DefuseCard();

        deck.add(card1);
        deck.add(card2);
        assertEquals(2, deck.size());

        deck.insertAt(card3, 1);
        assertEquals(3, deck.size());

        assertEquals(card1, deck.draw());
        assertEquals(card3, deck.draw());
        assertEquals(card2, deck.draw());
        assertNull(deck.draw());
    }

    public void testProcessTurnPassAndDraw() {
        List<Player> players = new ArrayList<>();
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        players.add(p1);
        players.add(p2);

        Deck deck = new Deck();
        AttackCard attackCard = new AttackCard();
        deck.add(attackCard);

        Game game = new Game(players, deck);
        game.start();

        assertEquals(0, game.getActivePlayerIndex());
        game.processTurn(p1, Action.pass());

        assertTrue(p1.getHand().contains(attackCard), "Player should have drawn the card on pass");
        assertEquals(1, game.getActivePlayerIndex(), "Turn should advance to next player");
    }

    public void testAttackCardEffect() {
        List<Player> players = new ArrayList<>();
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        players.add(p1);
        players.add(p2);

        AttackCard attack = new AttackCard();
        p1.addCard(attack);

        Deck deck = new Deck();
        deck.add(new NopeCard());

        Game game = new Game(players, deck);
        game.start();

        assertEquals(0, game.getActivePlayerIndex());
        game.processTurn(p1, Action.play(attack));

        assertFalse(p1.getHand().contains(attack), "Card should be removed from hand");
        assertEquals(attack, game.getDiscardPile().getTopCard(), "Card should be in discard pile");
        assertEquals(1, game.getActivePlayerIndex(), "Turn should advance after Attack");
    }

    public void testWinnerAnnouncedWhenOnlyOnePlayerRemains() {
        List<Player> players = new ArrayList<>();
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        players.add(p1);
        players.add(p2);

        p2.setExploded(true);

        Game game = new Game(players, new Deck());
        game.start();

        game.endTurn();

        assertFalse(game.isRunning(), "Game should stop when one player remains");
        assertEquals(p1, game.getWinner(), "Player 1 should be the winner");
    }
}
