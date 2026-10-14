package explodingkittens.game;

import explodingkittens.cards.Card;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the draw deck of cards.
 */
public class Deck implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
    }

    public Deck(List<Card> cards) {
        this.cards = cards != null ? new ArrayList<>(cards) : new ArrayList<>();
    }

    /**
     * Draws the top card from the deck.
     *
     * @return the drawn card, or null if deck is empty
     */
    public Card draw() {
        if (cards == null || cards.isEmpty()) {
            return null;
        }
        return cards.remove(0);
    }

    /**
     * Inserts a card at a specific index in the deck.
     *
     * @param card  the card to insert
     * @param index insertion index (0-based)
     */
    public void insertAt(Card card, int index) {
        if (card == null) return;
        if (cards == null) {
            cards = new ArrayList<>();
        }
        int clampedIndex = Math.max(0, Math.min(index, cards.size()));
        cards.add(clampedIndex, card);
    }

    /**
     * Shuffles the cards currently in the deck.
     */
    public void shuffle() {
        if (cards != null) {
            Collections.shuffle(cards);
        }
    }

    public void add(Card card) {
        if (card != null) {
            if (cards == null) cards = new ArrayList<>();
            cards.add(card);
        }
    }

    public List<Card> getCards() {
        return cards;
    }

    public int size() {
        return cards != null ? cards.size() : 0;
    }

    public boolean isEmpty() {
        return cards == null || cards.isEmpty();
    }
}
