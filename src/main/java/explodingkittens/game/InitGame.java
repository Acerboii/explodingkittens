package explodingkittens.game;

import explodingkittens.cards.*;
import explodingkittens.online.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory class responsible for initializing players, deck, and game instances.
 */
public class InitGame {

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
     * Builds a deck containing base cards and any requested expansion packs.
     *
     * @param expansions list of expansions to include
     * @return assembled and shuffled Deck
     */
    public Deck buildDeck(List<ExpansionType> expansions) {
        Deck deck = new Deck();

        // Base cards
        deck.add(new DefuseCard());
        deck.add(new DefuseCard());
        for (int i = 0; i < 4; i++) {
            deck.add(new AttackCard());
        }
        for (int i = 0; i < 5; i++) {
            deck.add(new NopeCard());
        }

        // Expansions
        if (expansions != null) {
            if (expansions.contains(ExpansionType.IMPLODING_KITTENS)) {
                deck.add(new ImplodingKittenCard());
            }
            if (expansions.contains(ExpansionType.STREAKING_KITTENS)) {
                deck.add(new StreakingKittenCard());
            }
            if (expansions.contains(ExpansionType.BARKING_KITTENS)) {
                deck.add(new BarkingKittenCard());
                deck.add(new BarkingKittenCard());
            }
        }

        deck.shuffle();
        return deck;
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
        List<Player> players = setupPlayers(numPlayers, numBots);
        Deck deck = buildDeck(expansions);

        // Deal 4 cards from deck to each player (leaving room for drawing)
        for (Player player : players) {
            for (int i = 0; i < 4 && !deck.isEmpty(); i++) {
                Card drawn = deck.draw();
                if (drawn != null) {
                    player.addCard(drawn);
                }
            }
        }

        DiscardPile discardPile = new DiscardPile();
        return new Game(players, deck, discardPile);
    }
}
