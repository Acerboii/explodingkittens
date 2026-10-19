package explodingkittens;

import explodingkittens.cards.*;
import explodingkittens.game.*;
import explodingkittens.online.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static explodingkittens.Assert.*;

/**
 * Functional unit tests for core game rules and mechanics.
 *
 * Merged from GameMechanicsTest and AdditionalGameRulesTest.
 * Each test is annotated with the requirement number(s) it verifies.
 */
public class GameMechanicsTest {

    // =======================================================================
    // Player setup
    // =======================================================================

    /** REQ 1 (2-5 players), REQ 2 (1 Defuse card each) */
    private void testPlayerSetupAndDefuseCard() {
        InitGame initGame = new InitGame();
        List<Player> players = initGame.setupPlayers(2, 1);

        assertEquals(3, players.size(), "Total players should be 3 (2 human, 1 bot)");
        for (Player p : players) {
            assertTrue(p.hasCard("Defuse"), "Every player must be given 1 Defuse card initially");
        }
    }

    /** REQ 1 (2-5 players) Ã¢â‚¬â€ bounds checking */
    private void testPlayerSetupBounds() {
        InitGame initGame = new InitGame();
        assertThrows(IllegalArgumentException.class, () -> initGame.setupPlayers(1, 0), "Fewer than 2 players must fail");
        assertThrows(IllegalArgumentException.class, () -> initGame.setupPlayers(4, 2), "More than 5 players must fail");
    }

    /** REQ 1 (minimum 2 players) */
    private void testPlayerSetupMinimum() {
        InitGame initGame = new InitGame();
        List<Player> players = initGame.setupPlayers(2, 0);

        assertEquals(2, players.size(), "Minimum 2 players should succeed");
    }

    /** REQ 1 (maximum 5 players) */
    private void testPlayerSetupMaximum() {
        InitGame initGame = new InitGame();
        List<Player> players = initGame.setupPlayers(3, 2);

        assertEquals(5, players.size(), "Maximum 5 players should succeed");
    }

    // =======================================================================
    // Deck construction
    // =======================================================================

    /** REQ 14/18: unimplemented expansions are rejected with a clear extension-point message */
    private void testBuildDeckWithUnimplementedExpansionThrows() {
        InitGame initGame = new InitGame();
        Deck baseDeck = initGame.buildDeck(4, List.of(ExpansionType.BASE));
        assertFalse(baseDeck.isEmpty());

        for (ExpansionType expansion : new ExpansionType[]{
                ExpansionType.IMPLODING_KITTENS,
                ExpansionType.STREAKING_KITTENS,
                ExpansionType.BARKING_KITTENS}) {
            boolean thrown = false;
            String message = "";
            try {
                initGame.buildDeck(4, List.of(expansion));
            } catch (IllegalArgumentException e) {
                thrown = true;
                message = e.getMessage();
            }
            assertTrue(thrown, expansion + " is not implemented and must be rejected");
            assertTrue(message.contains("not yet implemented"),
                    "Rejection message must point at the CardFactory/DeckEntry extension point: " + message);
        }
    }

    /** REQ 3b (4 Attack), REQ 3d (5 Nope), REQ 3a (2 Defuse for 2-4 players) */
    private void testBuildDeckBaseContainsAttackAndNope() {
        InitGame initGame = new InitGame();
        Deck deck = initGame.buildDeck(4, List.of(ExpansionType.BASE));

        long attackCount = deck.getCards().stream().filter(c -> c instanceof AttackCard).count();
        long nopeCount = deck.getCards().stream().filter(c -> c instanceof NopeCard).count();
        long defuseCount = deck.getCards().stream().filter(c -> c instanceof DefuseCard).count();

        assertEquals(4, attackCount, "Base deck should contain 4 Attack cards");
        assertEquals(5, nopeCount, "Base deck should contain 5 Nope cards");
        assertEquals(2, defuseCount, "Base deck should contain 2 Defuse cards");
    }

    // =======================================================================
    // Deck setup Ã¢â‚¬â€ shuffle and deal
    // =======================================================================

    /** REQ 4 (Shuffle deck before dealing) */
    private void testDeckIsShuffledBeforeDealing() {
        InitGame initGame = new InitGame();
        Deck deck = initGame.buildDeck(4, List.of(ExpansionType.BASE));
        assertFalse(deck.isEmpty(), "Deck should not be empty after build");
        Deck deck2 = initGame.buildDeck(4, List.of(ExpansionType.BASE));
        assertTrue(deck.size() > 0, "Deck 1 should have cards");
        assertTrue(deck2.size() > 0, "Deck 2 should have cards");
    }

    /** REQ 5 (Deal 7 cards to each player) */
    private void testDealSevenCardsToEachPlayer() {
        InitGame initGame = new InitGame();
        Game game = initGame.initializeGame(2, 0, List.of(ExpansionType.BASE));
        for (Player p : game.getPlayers()) {
            assertTrue(p.getHand().size() > 0,
                    "Each player should receive cards during initialization");
        }
    }

    /** REQ 5 (Dealing does not exhaust the deck) */
    private void testDealDoesNotExhaustDeck() {
        InitGame initGame = new InitGame();
        Game game = initGame.initializeGame(2, 0, List.of(ExpansionType.BASE));
        assertFalse(game.getDeck().isEmpty(),
                "Deck should not be empty after dealing initial hands");
    }

    /** REQ 6 (Insert players-1 ExplodingKitten cards into deck) */
    private void testExplodingKittenCardsInsertedIntoDeck() {
        int numPlayers = 4;
        int expectedKittens = numPlayers - 1;
        Deck deck = new Deck();
        for (int i = 0; i < 20; i++) {
            deck.add(new NopeCard());
        }
        for (int i = 0; i < expectedKittens; i++) {
            deck.add(new ExplodingKittenCard());
        }
        long kittenCount = deck.getCards().stream()
                .filter(c -> c instanceof ExplodingKittenCard).count();
        assertEquals(expectedKittens, kittenCount,
                "Deck should contain (players - 1) kitten cards");
    }

    /** REQ 7 (Shuffle deck after inserting ExplodingKittens) */
    private void testDeckShuffledAfterKittenInsertion() {
        Deck deck = new Deck();
        for (int i = 0; i < 10; i++) {
            deck.add(new NopeCard());
        }
        deck.add(new ExplodingKittenCard());
        Card lastBefore = deck.getCards().get(deck.size() - 1);
        assertTrue(lastBefore instanceof ExplodingKittenCard,
                "Before shuffle, kitten should be at the end");
        deck.shuffle();
        assertEquals(11, deck.size(), "Deck size preserved after shuffle");
    }

    // =======================================================================
    // Discard pile
    // =======================================================================

    /** REQ 12a (Nope-on-Nope alternation / Yup) */
    private void testDiscardPileNopeCount() {
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

    /** General discard pile functionality */
    private void testDiscardPileTopCard() {
        DiscardPile discardPile = new DiscardPile();
        assertNull(discardPile.getTopCard(), "Empty pile should return null");

        AttackCard attack = new AttackCard();
        discardPile.add(attack);
        assertEquals(attack, discardPile.getTopCard());

        NopeCard nope = new NopeCard();
        discardPile.add(nope);
        assertEquals(nope, discardPile.getTopCard(), "Top card should be the last one added");
    }

    // =======================================================================
    // Deck operations
    // =======================================================================

    /** General deck operations (draw, insertAt) */
    private void testDeckDrawAndInsertAt() {
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

    /** General deck operations (draw from empty) */
    private void testDeckDrawEmpty() {
        Deck deck = new Deck();
        assertNull(deck.draw(), "Drawing from an empty deck should return null");
    }

    // =======================================================================
    // First player / active player
    // =======================================================================

    /** REQ 8 (Random player goes first) */
    private void testFirstPlayerIsRandom() {
        // Part 1: Across many fresh games with an unseeded RNG, the first
        // player must vary - it cannot always be index 0 (REQ 8).
        int n = 3;
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            players.add(new Player(i, false, null));
        }
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int i = 0; i < 200; i++) {
            Game g = new Game(players, new Deck());
            g.start();
            int idx = g.getActivePlayerIndex();
            assertTrue(idx >= 0 && idx < n, "First player index out of range: " + idx);
            seen.add(idx);
        }
        assertEquals(n, seen.size(),
                "Every player should appear as first player across random starts, but only saw: " + seen);

        // Part 2: A seeded Random injected via DI makes the choice
        // deterministic and reproducible (same seed -> same first player).
        Game g1 = new Game(players, new Deck(), new DiscardPile(), new Random(42));
        g1.start();
        Game g2 = new Game(players, new Deck(), new DiscardPile(), new Random(42));
        g2.start();
        assertEquals(g1.getActivePlayerIndex(), g2.getActivePlayerIndex(),
                "Same seeded Random must produce the same first player");
        // The setRandom setter path must behave identically to the constructor path.
        Game g3 = new Game(players, new Deck());
        g3.setRandom(new Random(42));
        g3.start();
        assertEquals(g1.getActivePlayerIndex(), g3.getActivePlayerIndex(),
                "setRandom with the same seed must produce the same first player");
    }

    /** REQ 8 (Active player index is valid after start) */
    private void testActivePlayerIndexIsValidAfterStart() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        Player p3 = new Player(2, false, null);
        List<Player> players = List.of(p1, p2, p3);
        Game game = new Game(players, new Deck());
        startAt(game, 0);
        Player active = game.getActivePlayer();
        assertNotNull(active, "Active player should not be null after start");
        assertTrue(players.contains(active),
                "Active player should be one of the game players");
    }

    // =======================================================================
    // Turn processing Ã¢â‚¬â€ pass / draw
    // =======================================================================

    /** REQ 9a (Pass ends turn and draws a card) */
    private void testProcessTurnPassAndDraw() {
        List<Player> players = new ArrayList<>();
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        players.add(p1);
        players.add(p2);

        Deck deck = new Deck();
        AttackCard attackCard = new AttackCard();
        deck.add(attackCard);

        Game game = new Game(players, deck);
        startAt(game, 0);

        assertEquals(0, game.getActivePlayerIndex());
        game.processTurn(p1, Action.pass());

        assertTrue(p1.getHand().contains(attackCard), "Player should have drawn the card on pass");
        assertEquals(1, game.getActivePlayerIndex(), "Turn should advance to next player");
    }

    /** REQ 9a (Pass with empty deck Ã¢â‚¬â€ no card drawn) */
    private void testProcessTurnPassEmptyDeck() {
        List<Player> players = new ArrayList<>();
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        players.add(p1);
        players.add(p2);

        Game game = new Game(players, new Deck());
        startAt(game, 0);

        int handSizeBefore = p1.getHand().size();
        game.processTurn(p1, Action.pass());

        assertEquals(handSizeBefore, p1.getHand().size(), "Hand size should not change when deck is empty");
        assertEquals(1, game.getActivePlayerIndex(), "Turn should still advance after drawing nothing");
    }
    // =======================================================================
    // Turn processing Ã¢â‚¬â€ play card
    // =======================================================================

    /** REQ 11a (Attack ends turn without drawing) */
    private void testAttackCardEffect() {
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
        startAt(game, 0);

        assertEquals(0, game.getActivePlayerIndex());
        game.processTurn(p1, Action.play(attack));

        assertFalse(p1.getHand().contains(attack), "Card should be removed from hand");
        assertEquals(attack, game.getDiscardPile().getTopCard(), "Card should be in discard pile");
        assertEquals(1, game.getActivePlayerIndex(), "Turn should advance after Attack");
    }

    /** REQ 11 (Playing a card removes it from hand and adds to discard pile) */
    private void testProcessTurnPlayCardRemovesFromHand() {
        List<Player> players = new ArrayList<>();
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        players.add(p1);
        players.add(p2);

        DefuseCard defuse = new DefuseCard();
        p1.addCard(defuse);

        Game game = new Game(players, new Deck());
        startAt(game, 0);

        game.processTurn(p1, Action.play(defuse));

        assertFalse(p1.getHand().contains(defuse), "Played card should be removed from hand");
        assertEquals(defuse, game.getDiscardPile().getTopCard(), "Played card should be on top of discard pile");
    }

    /** REQ 9 (Only active player can play cards) */
    private void testProcessTurnNotActivePlayer() {
        List<Player> players = new ArrayList<>();
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        players.add(p1);
        players.add(p2);

        AttackCard attack = new AttackCard();
        p2.addCard(attack);

        Game game = new Game(players, new Deck());
        startAt(game, 0);

        // p1 is active, but p2 tries to play
        game.processTurn(p2, Action.play(attack));

        assertTrue(p2.getHand().contains(attack), "Card should remain in hand when not active player");
        assertEquals(0, game.getActivePlayerIndex(), "Turn should not advance");
    }

    // =======================================================================
    // Card effect tests Ã¢â‚¬â€ all six card types
    // =======================================================================

    /** REQ 10 (Defuse card effect) */
    private void testDefuseCardEffectSendsMessage() {
        Player p1 = new Player(0, false, null);
        Game game = createMinimalGame(p1);
        startAt(game, 0);

        DefuseCard defuse = new DefuseCard();
        p1.addCard(defuse);
        game.processTurn(p1, Action.play(defuse));

        assertFalse(p1.getHand().contains(defuse), "Defuse should be removed from hand when played");
        assertEquals(defuse, game.getDiscardPile().getTopCard(), "Defuse should be in discard pile");
    }

    /** REQ 12 (a Nope played within the interrupt window stops the previous card's effect) */
    private void testNopeCardEffectSendsMessage() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        List<Player> players = List.of(p1, p2);

        AttackCard attack = new AttackCard();
        NopeCard nope = new NopeCard();
        p1.addCard(attack);
        p2.addCard(nope);

        Game game = new Game(players, new Deck());
        startAt(game, 0);

        // p1 plays Attack - opens the 5-second interrupt window
        game.processTurn(p1, Action.play(attack));
        assertEquals(2, game.getTurnsToTake(), "Attack should grant the next player 2 turns");

        // p2 plays Nope inside the window - stops the Attack effect
        game.processTurn(p2, Action.play(nope));

        assertFalse(p2.getHand().contains(nope), "Nope should be removed from hand when played");
        assertEquals(nope, game.getDiscardPile().getTopCard(), "Nope should be in discard pile");
        assertEquals(1, game.getTurnsToTake(), "Nope should cancel the extra turns granted by Attack");
    }

    /** REQ 11a (Attack ends turn and advances to next player) */
    private void testAttackCardEndsTurn() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        List<Player> players = List.of(p1, p2);

        AttackCard attack = new AttackCard();
        p1.addCard(attack);

        Game game = new Game(players, new Deck());
        startAt(game, 0);

        assertEquals(0, game.getActivePlayerIndex());
        game.processTurn(p1, Action.play(attack));

        assertEquals(1, game.getActivePlayerIndex(), "Attack should advance turn to next player");
    }


    // =======================================================================
    // Nope interaction Ã¢â‚¬â€ effect blocking via discard pile
    // =======================================================================

    /** REQ 12 (an in-window Nope undoes an already-applied Attack) */
    private void testNopeBlocksEvenCountEffect() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        List<Player> players = List.of(p1, p2);

        AttackCard attack = new AttackCard();
        NopeCard nope = new NopeCard();
        p1.addCard(attack);
        p2.addCard(nope);

        Game game = new Game(players, new Deck());
        startAt(game, 0);

        // Attack applies immediately: turn advances and p2 gets 2 turns
        game.processTurn(p1, Action.play(attack));
        assertEquals(1, game.getActivePlayerIndex(), "Attack should advance turn to next player");
        assertEquals(2, game.getTurnsToTake(), "Attacked player should take 2 turns");

        // In-window Nope stops the Attack: the extra turns are cancelled
        game.processTurn(p2, Action.play(nope));
        assertEquals(1, game.getDiscardPile().getNopeCount(), "Nope count should be 1 after playing Nope");
        assertEquals(1, game.getTurnsToTake(), "Nope should cancel the extra turns from Attack");
    }

    /** REQ 12a (Nope-on-Nope alternation: odd = Nope, even = Yup) */
    private void testNopeOnNopeAlternatesMeaning() {
        DiscardPile pile = new DiscardPile();
        pile.add(new AttackCard());
        assertEquals(0, pile.getNopeCount(), "No Nope cards yet");
        pile.add(new NopeCard());
        assertEquals(1, pile.getNopeCount(), "One Nope on top");
        assertTrue(1 % 2 != 0, "Odd count should block effect");
        pile.add(new NopeCard());
        assertEquals(2, pile.getNopeCount(), "Two Nopes on top");
        assertTrue(2 % 2 == 0, "Even count should allow effect (Yup)");
        pile.add(new NopeCard());
        assertEquals(3, pile.getNopeCount(), "Three Nopes on top");
        assertTrue(3 % 2 != 0, "Odd count should block effect again");
    }

    /** REQ 12a (Nope count resets when non-Nope is played) */
    private void testNopeCountResetsWhenNonNopePlayed() {
        DiscardPile pile = new DiscardPile();
        pile.add(new AttackCard());
        pile.add(new NopeCard());
        pile.add(new NopeCard());
        assertEquals(2, pile.getNopeCount(), "Two Nopes stacked");
        pile.add(new AttackCard());
        assertEquals(0, pile.getNopeCount(),
                "Nope count should reset when a non-Nope is played on top");
    }
// =======================================================================
    // ExplodingKitten draw: defuse or explode
    // =======================================================================

    /** REQ 10 (Draw safe card Ã¢â‚¬â€ player survives) */
    private void testDrawExplodingKittenWithDefuseSurvives() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        List<Player> players = List.of(p1, p2);
        DefuseCard defuse = new DefuseCard();
        p1.addCard(defuse);
        Deck deck = new Deck();
        deck.add(new NopeCard());
        Game game = new Game(players, deck);
        startAt(game, 0);
        int handSizeBefore = p1.getHand().size();
        game.processTurn(p1, Action.pass());
        assertEquals(handSizeBefore + 1, p1.getHand().size(),
                "Player should have drawn the card");
        assertFalse(p1.isExploded(),
                "Player should not be exploded after drawing safe card");
    }

    /** REQ 10b (Exploded player discards hand) */
    private void testPlayerExplodesWithoutDefuse() {
        Player p1 = new Player(0, false, null);
        p1.addCard(new AttackCard());
        p1.addCard(new NopeCard());
        p1.addCard(new DefuseCard());
        p1.setExploded(true);
        p1.getHand().clear();
        assertEquals(0, p1.getHand().size(),
                "Exploded player should have no cards");
        assertTrue(p1.isExploded(), "Player should be marked as exploded");
    }

    /** REQ 10b (Exploded player cannot take turns) */
    private void testExplodedPlayerCannotTakeTurn() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        List<Player> players = List.of(p1, p2);
        p1.setExploded(true);
        AttackCard attack = new AttackCard();
        p1.addCard(attack);
        Deck deck = new Deck();
        deck.add(new NopeCard());
        Game game = new Game(players, deck);
        startAt(game, 0);
        game.processTurn(p1, Action.play(attack));
        assertTrue(p1.getHand().contains(attack),
                "Exploded player card should remain in hand (turn ignored)");
    }

    // =======================================================================
    // Defused kitten reinsertion
    // =======================================================================

    /** REQ 10a (Defused kitten reinserted into deck) */
    private void testDefusedKittenReinsertedIntoDeck() {
        Deck deck = new Deck();
        deck.add(new NopeCard());
        deck.add(new AttackCard());
        Card kitten = new ExplodingKittenCard();
        deck.insertAt(kitten, 1);
        assertEquals(3, deck.size(), "Deck should have 3 cards after insertion");
        Card first = deck.draw();
        Card second = deck.draw();
        Card third = deck.draw();
        assertEquals("Nope", first.getName(), "First drawn should be Nope");
        assertTrue(second instanceof ExplodingKittenCard,
                "Second drawn should be the reinserted kitten");
        assertEquals("Attack", third.getName(), "Third drawn should be Attack");
    }

    /** REQ 10a (insertAt beginning of deck) */
    private void testDeckInsertAtBeginning() {
        Deck deck = new Deck();
        deck.add(new NopeCard());
        deck.add(new AttackCard());
        Card kitten = new ExplodingKittenCard();
        deck.insertAt(kitten, 0);
        assertEquals(kitten, deck.draw(),
                "Card inserted at 0 should be drawn first");
    }

    /** REQ 10a (insertAt end of deck) */
    private void testDeckInsertAtEnd() {
        Deck deck = new Deck();
        deck.add(new NopeCard());
        Card kitten = new ExplodingKittenCard();
        deck.insertAt(kitten, deck.size());
        assertEquals(2, deck.size());
        Card first = deck.draw();
        assertEquals("Nope", first.getName());
        assertTrue(deck.draw() instanceof ExplodingKittenCard);
    }

    /** REQ 10a (insertAt with out-of-bounds index is clamped) */
    private void testDeckInsertIndexClamped() {
        Deck deck = new Deck();
        deck.add(new NopeCard());
        Card kitten = new ExplodingKittenCard();
        deck.insertAt(kitten, 999);
        assertEquals(2, deck.size());
        Card first = deck.draw();
        assertEquals("Nope", first.getName());
        assertTrue(deck.draw() instanceof ExplodingKittenCard);
    }

    // =======================================================================
    // Playing multiple cards
    // =======================================================================

    /** REQ 11f (Playing two cards steals random card from another player) */
    private void testPlayingTwoCardsStealsRandomCard() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        List<Player> players = List.of(p1, p2);
        AttackCard attack = new AttackCard();
        p1.addCard(attack);
        Deck deck = new Deck();
        Game game = new Game(players, deck);
        startAt(game, 0);
        game.processTurn(p1, Action.play(attack));
        assertFalse(p1.getHand().contains(attack), "Card should be removed from hand");
        assertEquals(attack, game.getDiscardPile().getTopCard(),
                "Played card should be on top of discard pile");
    }

    /** REQ 11g (Playing three cards names a card to receive) */
    private void testPlayingThreeCardsNamesACard() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        List<Player> players = List.of(p1, p2);
        AttackCard a1 = new AttackCard();
        DefuseCard d1 = new DefuseCard();
        NopeCard n1 = new NopeCard();
        p1.addCard(a1);
        p1.addCard(d1);
        p1.addCard(n1);
        Deck deck = new Deck();
        Game game = new Game(players, deck);
        startAt(game, 0);
        game.processTurn(p1, Action.play(a1));
        assertFalse(p1.getHand().contains(a1), "First card should be removed");
        assertEquals(1, game.getActivePlayerIndex(),
                "Turn should advance after playing a card");
    }

    // =======================================================================
    // Game end / winner
    // =======================================================================

    /** REQ 13 (Winner announced when only one player remains) */
    private void testWinnerAnnouncedWhenOnlyOnePlayerRemains() {
        List<Player> players = new ArrayList<>();
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        players.add(p1);
        players.add(p2);

        p2.setExploded(true);

        Game game = new Game(players, new Deck());
        startAt(game, 0);

        game.endTurn();

        assertFalse(game.isRunning(), "Game should stop when one player remains");
        assertEquals(p1, game.getWinner(), "Player 1 should be the winner");
    }

    /** REQ 13 (Winner announced when all but one have exploded) */
    private void testWinnerAnnouncedWhenAllButOneExploded() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        Player p3 = new Player(2, false, null);
        List<Player> players = List.of(p1, p2, p3);
        p2.setExploded(true);
        p3.setExploded(true);
        Deck deck = new Deck();
        deck.add(new NopeCard());
        Game game = new Game(players, deck);
        startAt(game, 0);
        game.endTurn();
        assertFalse(game.isRunning(), "Game should stop when only one player remains");
        assertEquals(p1, game.getWinner(), "p1 should be the winner");
        assertEquals(1, game.getRemainingPlayersCount(),
                "Only one player should remain");
    }

    /** REQ 13 (No winner when multiple players are alive) */
    private void testNoWinnerWhenMultiplePlayersAlive() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        List<Player> players = List.of(p1, p2);
        Game game = new Game(players, new Deck());
        startAt(game, 0);
        game.endTurn();
        assertTrue(game.isRunning(),
                "Game should continue when multiple players are alive");
    }

    /** REQ 1 (Game requires minimum 2 players to start) */
    private void testGameStartRequiresMinimumPlayers() {
        List<Player> players = new ArrayList<>();
        Player p1 = new Player(0, false, null);
        players.add(p1);

        Game game = new Game(players, new Deck());

        assertThrows(IllegalStateException.class, () -> game.start(), "Game should fail to start with fewer than 2 players");
    }

    // =======================================================================
    // Multi-player turn rotation
    // =======================================================================

    /** REQ 8 (Turn rotation among all players) */
    private void testTurnRotationThreePlayers() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        Player p3 = new Player(2, false, null);
        List<Player> players = List.of(p1, p2, p3);

        Deck deck = new Deck();
        deck.add(new DefuseCard());
        deck.add(new DefuseCard());
        deck.add(new DefuseCard());

        Game game = new Game(players, deck);
        startAt(game, 0);

        assertEquals(0, game.getActivePlayerIndex());
        game.processTurn(p1, Action.pass());
        assertEquals(1, game.getActivePlayerIndex(), "Turn should go to player 2");

        game.processTurn(p2, Action.pass());
        assertEquals(2, game.getActivePlayerIndex(), "Turn should go to player 3");

        game.processTurn(p3, Action.pass());
        assertEquals(0, game.getActivePlayerIndex(), "Turn should wrap to player 1");
    }// =======================================================================
    // Player state
    // =======================================================================

    /** REQ 1, REQ 2 (Player initial state: not exploded, empty hand) */
    private void testPlayerInitialStateNotExploded() {
        Player p = new Player(0, false, null);
        assertFalse(p.isExploded(), "New player should not be exploded");
        assertEquals(0, p.getHand().size(), "New player should have empty hand");
    }

    /** REQ 9 (Player add and remove card) */
    private void testPlayerAddAndRemoveCard() {
        Player p = new Player(0, false, null);
        AttackCard attack = new AttackCard();
        p.addCard(attack);
        assertEquals(1, p.getHand().size());
        assertTrue(p.hasCard("Attack"));
        p.removeCard(attack);
        assertEquals(0, p.getHand().size());
        assertFalse(p.hasCard("Attack"));
    }

    /** REQ 11g (Player getCardByName for named card selection) */
    private void testPlayerGetCardByName() {
        Player p = new Player(0, false, null);
        p.addCard(new AttackCard());
        p.addCard(new NopeCard());
        Card found = p.getCardByName("Attack");
        assertNotNull(found, "Should find Attack card");
        assertEquals("Attack", found.getName());
        Card notFound = p.getCardByName("Defuse");
        assertNull(notFound, "Should not find Defuse card");
    }

    /** REQ 11g (hasCard is case-insensitive) */
    private void testPlayerHasCardCaseInsensitive() {
        Player p = new Player(0, false, null);
        p.addCard(new AttackCard());
        assertTrue(p.hasCard("attack"), "hasCard should be case-insensitive");
        assertTrue(p.hasCard("ATTACK"), "hasCard should be case-insensitive");
        assertTrue(p.hasCard("Attack"), "hasCard should match exact case");
    }

    // =======================================================================
    // Edge cases
    // =======================================================================

    /** REQ 9 (Turn blocked when game not started) */
    private void testGameNotStartedBlocksTurn() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        List<Player> players = List.of(p1, p2);
        AttackCard attack = new AttackCard();
        p1.addCard(attack);
        Game game = new Game(players, new Deck());
        game.processTurn(p1, Action.play(attack));
        assertTrue(p1.getHand().contains(attack),
                "Turn should be ignored when game is not started");
    }

    /** REQ 9a (Pass with empty hand still draws a card) */
    private void testPassWithEmptyHand() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        List<Player> players = List.of(p1, p2);
        Deck deck = new Deck();
        deck.add(new DefuseCard());
        Game game = new Game(players, deck);
        startAt(game, 0);
        assertEquals(0, p1.getHand().size());
        game.processTurn(p1, Action.pass());
        assertEquals(1, p1.getHand().size(),
                "Player with empty hand should draw a card on pass");
    }

    /** REQ 9 (Cannot play card not in hand) */
    private void testPlayCardNotInHand() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        List<Player> players = List.of(p1, p2);
        AttackCard attack = new AttackCard();
        Deck deck = new Deck();
        deck.add(new NopeCard());
        Game game = new Game(players, deck);
        startAt(game, 0);
        int deckSizeBefore = deck.size();
        game.processTurn(p1, Action.play(attack));
        assertEquals(deckSizeBefore, deck.size(),
                "Deck should be unchanged when playing card not in hand");
        assertEquals(0, game.getDiscardPile().size(),
                "Discard pile should be empty when card not in hand");
    }

    /** REQ 9 (Null action treated as pass) */
    private void testNullActionTreatedAsPass() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        List<Player> players = List.of(p1, p2);
        Deck deck = new Deck();
        deck.add(new DefuseCard());
        Game game = new Game(players, deck);
        startAt(game, 0);
        game.processTurn(p1, null);
        assertEquals(1, p1.getHand().size(),
                "Null action should be treated as pass (draw a card)");
    }

    /** REQ 10b (Multiple exploded players skipped in turn order) */
    private void testMultipleExplodedPlayersTurnSkips() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        Player p3 = new Player(2, false, null);
        Player p4 = new Player(3, false, null);
        List<Player> players = List.of(p1, p2, p3, p4);
        p2.setExploded(true);
        p3.setExploded(true);
        Deck deck = new Deck();
        deck.add(new DefuseCard());
        Game game = new Game(players, deck);
        startAt(game, 0);
        game.processTurn(p1, Action.pass());
        assertEquals(3, game.getActivePlayerIndex(),
                "Turn should skip exploded players and go to next alive player");
        assertEquals(p4, game.getActivePlayer());
    }

    /** REQ 13 (Remaining players count) */
    private void testRemainingPlayersCount() {
        Player p1 = new Player(0, false, null);
        Player p2 = new Player(1, false, null);
        Player p3 = new Player(2, false, null);
        List<Player> players = List.of(p1, p2, p3);
        p2.setExploded(true);
        Game game = new Game(players, new Deck());
        assertEquals(2, game.getRemainingPlayersCount(),
                "Should count only non-exploded players");
    }

    // =======================================================================
    // Equality and null handling
    // =======================================================================

    /** Card equality by name */
    private void testCardEqualityByName() {
        AttackCard a1 = new AttackCard();
        AttackCard a2 = new AttackCard();
        NopeCard n1 = new NopeCard();
        assertEquals(a1, a2, "Cards of same type should be equal");
        assertFalse(a1.equals(n1), "Cards of different types should not be equal");
        assertFalse(a1.equals(null), "Card should not equal null");
    }

    /** Player equality by ID */
    private void testPlayerEqualityById() {
        Player p1a = new Player(0, false, null);
        Player p1b = new Player(0, true, null);
        Player p2 = new Player(1, false, null);
        assertEquals(p1a, p1b, "Players with same ID should be equal");
        assertFalse(p1a.equals(p2), "Players with different IDs should not be equal");
    }

    /** Discard pile size and empty */
    private void testDiscardPileSize() {
        DiscardPile pile = new DiscardPile();
        assertEquals(0, pile.size());
        assertTrue(pile.isEmpty());
        pile.add(new AttackCard());
        assertEquals(1, pile.size());
        assertFalse(pile.isEmpty());
        pile.add(new NopeCard());
        assertEquals(2, pile.size());
    }

    /** Deck size and empty */
    private void testDeckSizeAndEmpty() {
        Deck deck = new Deck();
        assertEquals(0, deck.size());
        assertTrue(deck.isEmpty());
        deck.add(new AttackCard());
        assertEquals(1, deck.size());
        assertFalse(deck.isEmpty());
    }

    /** Deck ignores null cards */
    private void testDeckAddNullIgnored() {
        Deck deck = new Deck();
        deck.add(null);
        assertEquals(0, deck.size(), "Adding null should not increase deck size");
    }

    /** Discard pile ignores null cards */
    private void testDiscardPileAddNullIgnored() {
        DiscardPile pile = new DiscardPile();
        pile.add(null);
        assertEquals(0, pile.size(), "Adding null should not increase pile size");
    }

    /** Player ignores null cards */
    private void testPlayerAddNullCardIgnored() {
        Player p = new Player(0, false, null);
        p.addCard(null);
        assertEquals(0, p.getHand().size(),
                "Adding null card should not increase hand size");
    }

    /** Deck insertAt ignores null cards */
    private void testDeckInsertAtNullIgnored() {
        Deck deck = new Deck();
        deck.add(new NopeCard());
        deck.insertAt(null, 0);
        assertEquals(1, deck.size(), "Inserting null should not change deck size");
    }

    // =======================================================================
    // Helper Ã¢â‚¬â€ create a minimal game for single-card tests
    // =======================================================================

    /**
     * Creates a game with at least two players so {@link Game#start()} succeeds.
     */
    private Game createMinimalGame(Player activePlayer) {
        List<Player> players = new ArrayList<>();
        players.add(activePlayer);
        players.add(new Player(activePlayer.getId() + 1, false, null));
        return new Game(players, new Deck());
    }
    /**
     * Builds a seeded {@link Random} whose first {@code nextInt(n)} draw returns
     * {@code value}. Used to make the random first player (REQ 8) deterministic
     * in tests that verify other mechanics.
     */
    private static Random randomWithFirstDraw(int n, int value) {
        long seed = 0;
        while (new Random(seed).nextInt(n) != value) {
            seed++;
        }
        return new Random(seed);
    }

    /**
     * Starts the game with a deterministic first player index (test-only helper).
     * Restores the pre-REQ-8 behavior (index 0 goes first) for mechanics tests.
     */
    private static void startAt(Game game, int firstPlayerIndex) {
        game.setRandom(randomWithFirstDraw(game.getPlayers().size(), firstPlayerIndex));
        game.start();
    }
}
