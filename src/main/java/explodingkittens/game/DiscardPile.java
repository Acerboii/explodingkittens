package explodingkittens.game;

import explodingkittens.cards.Card;
import explodingkittens.cards.NopeCard;
import java.io.Serializable;
import java.util.Stack;

/**
 * Represents the discard pile where played cards are placed.
 */
public class DiscardPile implements Serializable {
    private static final long serialVersionUID = 1L;

    private Stack<Card> cards;

    public DiscardPile() {
        this.cards = new Stack<>();
    }

    /**
     * Adds a card to the discard pile.
     *
     * @param card the card to add
     */
    public void add(Card card) {
        if (card != null) {
            cards.push(card);
        }
    }

    /**
     * Retrieves the top card of the discard pile without removing it.
     *
     * @return the top card, or null if the pile is empty
     */
    public Card getTopCard() {
        return cards.isEmpty() ? null : cards.peek();
    }

    /**
     * Counts the number of consecutive Nope cards at the top of the discard pile.
     *
     * @return number of consecutive Nope cards
     */
    public int getNopeCount() {
        int count = 0;
        for (int i = cards.size() - 1; i >= 0; i--) {
            Card card = cards.get(i);
            if (card instanceof NopeCard || (card != null && "Nope".equalsIgnoreCase(card.getName()))) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    public Stack<Card> getCards() {
        return cards;
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
