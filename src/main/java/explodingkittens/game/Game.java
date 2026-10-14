package explodingkittens.game;

import explodingkittens.cards.Card;
import explodingkittens.cards.DefuseCard;
import explodingkittens.cards.ImplodingKittenCard;
import explodingkittens.online.Player;
import explodingkittens.ui.ConsoleUI;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Core Game engine managing players, deck, discard pile, and turn processing.
 */
public class Game implements GameContext, Serializable {
    private static final long serialVersionUID = 1L;

    private List<Player> players;
    private Deck deck;
    private DiscardPile discardPile;
    private int activePlayerIndex;

    private int turnsToTake;
    private boolean isRunning;
    private transient ConsoleUI ui;

    public Game() {
        this.players = new ArrayList<>();
        this.deck = new Deck();
        this.discardPile = new DiscardPile();
        this.activePlayerIndex = 0;
        this.turnsToTake = 1;
        this.isRunning = false;
        this.ui = new ConsoleUI();
    }

    public Game(List<Player> players, Deck deck) {
        this(players, deck, new DiscardPile());
    }

    public Game(List<Player> players, Deck deck, DiscardPile discardPile) {
        this.players = players != null ? new ArrayList<>(players) : new ArrayList<>();
        this.deck = deck != null ? deck : new Deck();
        this.discardPile = discardPile != null ? discardPile : new DiscardPile();
        this.activePlayerIndex = 0;
        this.turnsToTake = 1;
        this.isRunning = false;
        this.ui = new ConsoleUI();
    }

    /**
     * Starts the game session.
     */
    public void start() {
        if (players.size() < 2 || players.size() > 5) {
            throw new IllegalStateException("Game requires between 2 and 5 players (managed: " + players.size() + ")");
        }
        this.isRunning = true;
        this.activePlayerIndex = 0;
        this.turnsToTake = 1;
        if (ui != null) {
            ui.displayWelcome();
            ui.displayGameState(this, getActivePlayer());
        }
        broadcast("Game has started with " + players.size() + " players!");
        notifyActivePlayer();
    }

    /**
     * Processes a player's turn action.
     *
     * @param player the player performing the turn
     * @param action the action to process
     */
    public void processTurn(Player player, Action action) {
        if (!isRunning || player == null || player.isExploded()) {
            return;
        }
        if (!player.equals(getActivePlayer())) {
            player.sendMessage("It is not your turn!");
            return;
        }

        if (action == null || "PASS".equalsIgnoreCase(action.getType())) {
            // Pass ends the turn by drawing a card
            Card drawn = deck.draw();
            if (drawn != null) {
                if (ui != null) {
                    ui.displayCardDrawn(player, drawn);
                }
                if ("ExplodingKitten".equalsIgnoreCase(drawn.getName()) || drawn instanceof ImplodingKittenCard) {
                    Card defuse = player.getCardByName("Defuse");
                    if (defuse != null && !(drawn instanceof ImplodingKittenCard)) {
                        player.removeCard(defuse);
                        discardPile.add(defuse);
                        int insertPos = (ui != null && !player.isBot()) ? ui.promptDefusePlacement(deck.size()) : 0;
                        deck.insertAt(drawn, insertPos);
                        player.sendMessage("You defused the " + drawn.getName() + " and returned it to the deck!");
                        broadcast("Player " + player.getId() + " defused a kitten.");
                        if (ui != null) {
                            ui.displayMessage("Player " + player.getId() + " defused a kitten!");
                        }
                    } else {
                        player.setExploded(true);
                        discardPile.add(drawn);
                        for (Card c : new ArrayList<>(player.getHand())) {
                            discardPile.add(c);
                        }
                        player.getHand().clear();
                        player.sendMessage("You exploded!");
                        broadcast("Player " + player.getId() + " exploded!");
                        if (ui != null) {
                            ui.displayExplosion(player);
                        }
                    }
                } else {
                    player.addCard(drawn);
                    player.sendMessage("You drew: " + drawn.getName());
                }
            }
            endTurn();
        } else if ("PLAY_CARD".equalsIgnoreCase(action.getType()) || action.getCard() != null) {
            Card card = action.getCard();
            if (card != null && player.getHand().contains(card)) {
                player.removeCard(card);
                discardPile.add(card);
                broadcast("Player " + player.getId() + " played " + card.getName());
                if (ui != null) {
                    ui.displayAction("Player " + player.getId(), "Played " + card.getName());
                }
                notifyInterruptWindow();
                if (discardPile.getNopeCount() % 2 == 0) {
                    card.executeEffect(this, player, action.getTarget());
                } else {
                    broadcast("Effect of " + card.getName() + " was stopped by Nope!");
                    if (ui != null) {
                        ui.displayMessage("Effect of " + card.getName() + " was stopped by Nope!");
                    }
                }
            }
        }
    }

    /**
     * Notifies all players that an interrupt window has opened to play a Nope card.
     */
    public void notifyInterruptWindow() {
        for (Player p : players) {
            if (!p.isExploded() && p.hasCard("Nope")) {
                p.sendMessage("Interrupt window: You have 5 seconds to play Nope.");
            }
        }
    }

    @Override
    public void endTurn() {
        if (getRemainingPlayersCount() <= 1) {
            isRunning = false;
            Player winner = getWinner();
            if (winner != null) {
                broadcast("Player " + winner.getId() + " has won the game!");
                if (ui != null) {
                    ui.displayWinner(winner);
                }
            }
            return;
        }

        int count = players.size();
        for (int i = 1; i <= count; i++) {
            int nextIndex = (activePlayerIndex + i) % count;
            if (!players.get(nextIndex).isExploded()) {
                activePlayerIndex = nextIndex;
                break;
            }
        }
        notifyActivePlayer();
    }

    private void notifyActivePlayer() {
        Player current = getActivePlayer();
        if (current != null) {
            for (Player p : players) {
                if (p.equals(current)) {
                    p.sendMessage("It is your turn.");
                } else {
                    p.sendMessage("It is now the turn of Player " + current.getId());
                }
            }
        }
    }

    public void broadcast(String message) {
        for (Player p : players) {
            p.sendMessage(message);
        }
    }

    public int getRemainingPlayersCount() {
        int count = 0;
        for (Player p : players) {
            if (!p.isExploded()) {
                count++;
            }
        }
        return count;
    }

    public Player getWinner() {
        for (Player p : players) {
            if (!p.isExploded()) {
                return p;
            }
        }
        return null;
    }

    public Player getActivePlayer() {
        if (players == null || players.isEmpty() || activePlayerIndex < 0 || activePlayerIndex >= players.size()) {
            return null;
        }
        return players.get(activePlayerIndex);
    }

    @Override
    public Deck getDeck() {
        return deck;
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    @Override
    public DiscardPile getDiscardPile() {
        return discardPile;
    }

    public void setDiscardPile(DiscardPile discardPile) {
        this.discardPile = discardPile;
    }

    @Override
    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public int getActivePlayerIndex() {
        return activePlayerIndex;
    }

    public void setActivePlayerIndex(int activePlayerIndex) {
        this.activePlayerIndex = activePlayerIndex;
    }

    public int getTurnsToTake() {
        return turnsToTake;
    }

    public void setTurnsToTake(int turnsToTake) {
        this.turnsToTake = turnsToTake;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public ConsoleUI getUI() {
        return ui;
    }

    public void setUI(ConsoleUI ui) {
        this.ui = ui;
    }
}
