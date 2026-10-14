package explodingkittens.cards;

import explodingkittens.game.GameContext;
import explodingkittens.online.Player;
import java.io.Serializable;
import java.util.Objects;

/**
 * Abstract base class representing a card in Exploding Kittens.
 */
public abstract class Card implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;

    /**
     * Constructs a Card with the specified name.
     *
     * @param name the name of the card
     */
    public Card(String name) {
        this.name = name;
    }

    /**
     * Executes the card effect within the given game context.
     *
     * @param context the current game context
     * @param actor   the player who played the card
     * @param target  the target player, or null if no target is needed
     */
    public abstract void executeEffect(GameContext context, Player actor, Player target);

    /**
     * Returns the name of the card.
     *
     * @return card name
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return Objects.equals(name, card.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
