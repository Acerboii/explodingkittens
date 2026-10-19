package explodingkittens.game;

import explodingkittens.cards.Card;
import explodingkittens.cards.CardFactory;
import explodingkittens.cards.DefuseCard;
import explodingkittens.cards.ExplodingKittenCard;
import explodingkittens.online.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class responsible for initializing players, deck, and game instances.
 */
public class InitGame {

    /**
     * A single data-driven deck entry: which card (by registered name) and how many.
     */
    private static final class DeckEntry {
        final String cardName;
        final int count;

        DeckEntry(String cardName, int count) {
            this.cardName = cardName;
            this.count = count;
        }
    }

    /**
     * Data-driven expansion configuration (REQ 14).
     *
     * <p>Non-BASE expansions are intentionally EMPTY: their logic was removed in
     * favor of the {@link CardFactory} extension point. To implement one, add its
     * DeckEntries here and register its cards in {@link CardFactory}.</p>
     */
    private static final Map<ExpansionType, List<DeckEntry>> EXPANSION_DECKS;

    static {
        Map<ExpansionType, List<DeckEntry>> decks = new EnumMap<>(ExpansionType.class);
        decks.put(ExpansionType.BASE, Collections.emptyList());
        decks.put(ExpansionType.IMPLODING_KITTENS, Collections.emptyList());
        decks.put(ExpansionType.STREAKING_KITTENS, Collections.emptyList());
        decks.put(ExpansionType.BARKING_KITTENS, Collections.emptyList());
        EXPANSION_DECKS = Collections.unmodifiableMap(decks);
    }

    /**
     * Sets up players for the game, including local, online, and bot players.
     * Each player is dealt 1 Defuse card according to the official rules.
     *
     * @param numPlayers number of human players
     * @param numBots    number of bot players
     * @return list of initialized players
     */
    public List<Player> setupPlayers(int numPlayers, int numBots) {
        int total = numPlayers + numBots;
        if (total < 2 || total > 5) {
            throw new IllegalArgumentException("Total player count must be between 2 and 5. Got: " + total);
        }

        List<Player> players = new ArrayList<>();
        // First player is host / local player
        Player host = new Player(0, false, null);
        host.addCard(new DefuseCard());
        players.add(host);

        // Add bots
        for (int i = 0; i < numBots; i++) {
            Player bot = new Player(players.size(), false, true, null);
            bot.addCard(new DefuseCard());
            players.add(bot);
        }

        // Remaining human players (online)
        while (players.size() < total) {
            Player onlinePlayer = new Player(players.size(), true, null);
            onlinePlayer.addCard(new DefuseCard());
            players.add(onlinePlayer);
        }

        return players;
    }

    /**
     * Builds a deck containing base cards and any requested expansion packs
     * using a default {@link CardFactory}.
     *
     * @param playerCount total number of players (affects Defuse count, REQ 3a)
     * @param expansions  list of expansions to include
     * @return assembled and shuffled Deck
     */
    public Deck buildDeck(int playerCount, List<ExpansionType> expansions) {
        return buildDeck(playerCount, expansions, new CardFactory());
    }

    /**
     * Builds a deck containing base cards and any requested expansion packs.
     *
     * <p>All cards are created through the supplied {@link CardFactory}, which is
     * the extension point for future expansions (REQ 14).</p>
     *
     * @param playerCount total number of players (affects Defuse count, REQ 3a)
     * @param expansions  list of expansions to include
     * @param factory     the card factory used to create every card
     * @return assembled and shuffled Deck
     */
    public Deck buildDeck(int playerCount, List<ExpansionType> expansions, CardFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("CardFactory must not be null");
        }
        Deck deck = new Deck();

        // Base cards — REQ 3
        // REQ 3a: 2 Defuse cards for 2-4 players, 1 Defuse card for 5 players
        int defuseCount = (playerCount == 5) ? 1 : 2;
        addCards(deck, factory, "Defuse", defuseCount);
        // REQ 3b-l: fixed counts per card type
        addCards(deck, factory, "Attack", 4);
        addCards(deck, factory, "Favor", 4);
        addCards(deck, factory, "Nope", 5);
        addCards(deck, factory, "Shuffle", 4);
        addCards(deck, factory, "Skip", 4);
        addCards(deck, factory, "SeeTheFuture", 5);
        addCards(deck, factory, "HairyPotatoCat", 4);
        addCards(deck, factory, "Cattermelon", 4);
        addCards(deck, factory, "RainbowRalphingCat", 4);
        addCards(deck, factory, "TacoCat", 4);
        addCards(deck, factory, "OverweightBikiniCat", 4);

        // Expansions — REQ 14 (data-driven via EXPANSION_DECKS)
        if (expansions != null) {
            for (ExpansionType expansion : expansions) {
                if (expansion == null || expansion == ExpansionType.BASE) {
                    continue;
                }
                List<DeckEntry> entries = EXPANSION_DECKS.getOrDefault(expansion, Collections.emptyList());
                if (entries.isEmpty()) {
                    throw new IllegalArgumentException(
                            expansion + " not yet implemented - add its DeckEntries and register its cards in CardFactory");
                }
                for (DeckEntry entry : entries) {
                    addCards(deck, factory, entry.cardName, entry.count);
                }
            }
        }

        deck.shuffle();
        return deck;
    }

    /**
     * Reports whether an expansion pack is implemented (REQ 15 menu support).
     *
     * <p>The base game is always implemented. A non-BASE expansion becomes
     * implemented once its deck entries are registered in
     * {@code EXPANSION_DECKS} and its cards in {@link CardFactory}. The game
     * menu uses this to show which variants are playable, so with no
     * expansions implemented only the base game can be selected.</p>
     *
     * @param expansion the expansion to check
     * @return true if the expansion can be played
     */
    public boolean isImplemented(ExpansionType expansion) {
        if (expansion == null || expansion == ExpansionType.BASE) {
            return true;
        }
        List<DeckEntry> entries = EXPANSION_DECKS.getOrDefault(expansion, Collections.emptyList());
        return !entries.isEmpty();
    }

    /**
     * Adds {@code count} instances of the card registered under {@code name}.
     */
    private static void addCards(Deck deck, CardFactory factory, String name, int count) {
        for (int i = 0; i < count; i++) {
            Card card = factory.create(name);
            deck.add(card);
        }
    }

    /**
     * Initializes a complete Game session with players, deck, and dealt hands.
     *
     * @param numPlayers number of human players
     * @param numBots    number of bots
     * @param expansions list of expansions
     * @return configured Game instance
     */
    public Game initializeGame(int numPlayers, int numBots, List<ExpansionType> expansions) {
        return initializeGame(numPlayers, numBots, expansions, new CardFactory());
    }

    /**
     * Initializes a complete Game session with players, deck, and dealt hands
     * using the supplied {@link CardFactory} (REQ 14 extension point).
     *
     * @param numPlayers number of human players
     * @param numBots    number of bots
     * @param expansions list of expansions
     * @param factory    the card factory used to create every deck card
     * @return configured Game instance
     */
    public Game initializeGame(int numPlayers, int numBots, List<ExpansionType> expansions, CardFactory factory) {
        List<Player> players = setupPlayers(numPlayers, numBots);
        int totalPlayers = numPlayers + numBots;
        Deck deck = buildDeck(totalPlayers, expansions, factory);

        // Deal 7 cards from deck to each player (REQ 5)
        for (Player player : players) {
            for (int i = 0; i < 7 && !deck.isEmpty(); i++) {
                Card drawn = deck.draw();
                if (drawn != null) {
                    player.addCard(drawn);
                }
            }
        }

        // REQ 6: Insert ExplodingKitten cards into the deck (1 fewer than number of players)
        int numExplodingKittens = players.size() - 1;
        for (int i = 0; i < numExplodingKittens; i++) {
            deck.add(new ExplodingKittenCard());
        }

        // REQ 7: Shuffle the deck after inserting ExplodingKittens, before the game starts
        deck.shuffle();

        DiscardPile discardPile = new DiscardPile();
        return new Game(players, deck, discardPile);
    }
}