package explodingkittens.online;

import explodingkittens.cards.Card;
import explodingkittens.cards.NopeCard;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;

/**
 * Represents a player in Exploding Kittens.
 */
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private boolean isOnline;
    private ClientHandler connection;
    private List<Card> hand;
    private boolean isBot;
    private boolean exploded;

    private static transient Scanner scanner = new Scanner(System.in);

    /**
     * Constructs a local player with the given ID.
     *
     * @param id player identifier
     */
    public Player(int id) {
        this(id, false, null);
    }

    /**
     * Constructs a player with specified online status and connection.
     *
     * @param id         player identifier
     * @param isOnline   whether the player is connected online
     * @param connection network IO handler
     */
    public Player(int id, boolean isOnline, ClientHandler connection) {
        this(id, isOnline, false, connection);
    }

    /**
     * Constructs a player with full configuration.
     *
     * @param id         player identifier
     * @param isOnline   whether the player is connected online
     * @param isBot      whether the player is an automated bot
     * @param connection network IO handler
     */
    public Player(int id, boolean isOnline, boolean isBot, ClientHandler connection) {
        this.id = id;
        this.isOnline = isOnline;
        this.isBot = isBot;
        this.connection = connection;
        this.hand = new ArrayList<>();
        this.exploded = false;
    }

    /**
     * Sends a message to the player.
     *
     * @param msg the message to send
     */
    public void sendMessage(String msg) {
        if (isOnline && connection != null) {
            connection.sendMessage(msg);
        } else if (!isBot) {
            System.out.println("[Player " + id + "]: " + msg);
        }
    }

    /**
     * Receives an action command from the player.
     *
     * @return the action string
     */
    public String receiveAction() {
        if (isOnline && connection != null) {
            return connection.readMessage();
        } else if (isBot) {
            return decideBotAction();
        } else {
            if (scanner == null) {
                scanner = new Scanner(System.in);
            }
            if (scanner.hasNextLine()) {
                return scanner.nextLine();
            }
            return "Pass";
        }
    }

    /**
     * Bot decision (REQ 16): instead of always passing, play a random
     * available card from the hand. A Nope is never picked because it can
     * only be played within the interrupt window; when no card is playable
     * the bot passes.
     *
     * @return a 1-based card position in the hand, or "Pass"
     */
    public String decideBotAction() {
        List<Card> playable = new ArrayList<>();
        for (Card c : hand) {
            if (!(c instanceof NopeCard)) {
                playable.add(c);
            }
        }
        if (playable.isEmpty()) {
            return "Pass";
        }
        Card chosen = playable.get(new Random().nextInt(playable.size()));
        return String.valueOf(hand.indexOf(chosen) + 1);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        this.isOnline = online;
    }

    public ClientHandler getConnection() {
        return connection;
    }

    public void setConnection(ClientHandler connection) {
        this.connection = connection;
        this.isOnline = (connection != null);
    }

    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand != null ? new ArrayList<>(hand) : new ArrayList<>();
    }

    public void addCard(Card card) {
        if (card != null) {
            this.hand.add(card);
        }
    }

    public boolean removeCard(Card card) {
        return this.hand.remove(card);
    }

    public boolean hasCard(String cardName) {
        for (Card c : hand) {
            if (c.getName().equalsIgnoreCase(cardName)) {
                return true;
            }
        }
        return false;
    }

    public Card getCardByName(String cardName) {
        for (Card c : hand) {
            if (c.getName().equalsIgnoreCase(cardName)) {
                return c;
            }
        }
        return null;
    }

    public boolean isBot() {
        return isBot;
    }

    public void setBot(boolean bot) {
        this.isBot = bot;
    }

    public boolean isExploded() {
        return exploded;
    }

    public void setExploded(boolean exploded) {
        this.exploded = exploded;
    }

    /**
     * REQ 11b: Allows the target player to choose which card to give
     * when a Favor card is played on them.
     *
     * For bots: returns a random card.
     * For local players: prompts via console input.
     * For online players: sends a message and waits for a response.
     *
     * @return the card the player chooses to give, or null if hand is empty
     */
    public Card chooseCardToGive() {
        if (hand == null || hand.isEmpty()) {
            return null;
        }

        if (isBot) {
            // Bot: give a random card
            int randomIndex = (int) (Math.random() * hand.size());
            return hand.remove(randomIndex);
        } else if (!isOnline && connection == null) {
            // Local player: prompt via console
            return promptCardToGive();
        } else {
            // Online player: send message and wait
            sendMessage("You must give a card due to Favor. Your hand:");
            for (int i = 0; i < hand.size(); i++) {
                sendMessage("  [" + (i + 1) + "] " + hand.get(i).getName());
            }
            sendMessage("Enter the number of the card to give: ");
            if (connection != null) {
                try {
                    String response = connection.readMessage();
                    int idx = Integer.parseInt(response.trim()) - 1;
                    if (idx >= 0 && idx < hand.size()) {
                        return hand.remove(idx);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            // Fallback: give first card
            return hand.remove(0);
        }
    }

    /**
     * Prompts a local (non-online, non-bot) player to choose a card to give.
     */
    private Card promptCardToGive() {
        if (hand == null || hand.isEmpty()) {
            return null;
        }

        System.out.println("You must give a card due to Favor. Your hand:");
        for (int i = 0; i < hand.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + hand.get(i).getName());
        }
        System.out.print("Enter the number of the card to give: ");

        if (scanner == null) {
            scanner = new Scanner(System.in);
        }
        if (scanner.hasNextLine()) {
            try {
                int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                if (idx >= 0 && idx < hand.size()) {
                    return hand.remove(idx);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        // Fallback: give first card
        return hand.remove(0);
    }

    /**
     * Fix 5: Choose where in the deck to place a defused kitten (REQ 10 / Issue 12).
     * Online players are sent the placement question and wait for a number response
     * over the network; bots, local players, or unparseable responses default to the
     * top of the deck (0). Question text ends with ": " so the client console
     * auto-detects it as an action prompt.
     *
     * @param deckSize current deck size (maximum valid position)
     * @return insertion position clamped to [0, deckSize]
     */
    public int chooseDefusePosition(int deckSize) {
        if (!isBot && isOnline && connection != null) {
            sendMessage("*** YOU DEFUSED THE KITTEN! ***");
            sendMessage("Where in the deck do you want to place it? [0 = Top, " + deckSize + " = Bottom]: ");
            try {
                String response = connection.readMessage();
                int pos = Integer.parseInt(response.trim());
                return Math.max(0, Math.min(pos, deckSize));
            } catch (NumberFormatException ignored) {
            }
        }
        // Bot, local, no connection, or unparseable response: top of deck
        return 0;
    }

    /**
     * Fix 5: Name a card to receive from another player (REQ 11g / Issue 15).
     * Bots take the first card from the first non-empty target; online players pick
     * a target and name a card over the network. The named card is removed from the
     * target's hand (exact match, then partial match).
     *
     * @param availableTargets players the card can be taken from
     * @return the named card, or null if none could be obtained
     */
    public Card chooseNamedCard(List<Player> availableTargets) {
        if (availableTargets == null || availableTargets.isEmpty()) {
            return null;
        }

        if (isBot) {
            for (Player p : availableTargets) {
                if (!p.getHand().isEmpty()) {
                    return p.getHand().remove(0);
                }
            }
            return null;
        } else if (isOnline && connection != null) {
            sendMessage("*** Name a card to receive from another player! ***");
            sendMessage("Available targets:");
            for (int i = 0; i < availableTargets.size(); i++) {
                Player p = availableTargets.get(i);
                sendMessage("  [" + (i + 1) + "] Player " + p.getId() + " (Hand: " + p.getHand().size() + " cards)");
            }
            sendMessage("Enter target player number: ");
            try {
                String targetResponse = connection.readMessage();
                int targetIdx = Integer.parseInt(targetResponse.trim()) - 1;
                if (targetIdx >= 0 && targetIdx < availableTargets.size()) {
                    Player target = availableTargets.get(targetIdx);
                    sendMessage("Name the card you want to receive: ");
                    String cardName = connection.readMessage().trim();
                    // Try to find the named card in target's hand
                    Card namedCard = target.getCardByName(cardName);
                    if (namedCard != null) {
                        target.removeCard(namedCard);
                        return namedCard;
                    }
                    // Try partial match
                    for (Card c : target.getHand()) {
                        if (c.getName().toLowerCase().contains(cardName.toLowerCase())) {
                            target.removeCard(c);
                            return c;
                        }
                    }
                    sendMessage("Card '" + cardName + "' not found in Player " + target.getId() + "'s hand.");
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return id == player.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Player{" +
                "id=" + id +
                ", isOnline=" + isOnline +
                ", isBot=" + isBot +
                ", handSize=" + hand.size() +
                ", exploded=" + exploded +
                '}';
    }
}