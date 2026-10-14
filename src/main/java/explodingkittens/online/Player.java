package explodingkittens.online;

import explodingkittens.cards.Card;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
            return "Pass";
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
        isOnline = online;
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
        this.hand = hand != null ? hand : new ArrayList<>();
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
        isBot = bot;
    }

    public boolean isExploded() {
        return exploded;
    }

    public void setExploded(boolean exploded) {
        this.exploded = exploded;
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
