package explodingkittens.cards;

import explodingkittens.game.GameContext;
import explodingkittens.online.Player;

/**
 * Represents a Shuffle card which shuffles the deck.
 *
 * REQ 3e: 4 Shuffle cards in deck.
 * REQ 11c: Playing a Shuffle card will shuffle the deck.
 */
public class ShuffleCard extends Card {
    private static final long serialVersionUID = 1L;

    public ShuffleCard() {
        super("Shuffle");
    }

    @Override
    public void executeEffect(GameContext context, Player actor, Player target) {
        if (actor != null) {
            actor.sendMessage("Shuffle played: Deck has been shuffled.");
        }
        // REQ 11c: Shuffle the deck
        if (context != null) {
            context.getDeck().shuffle();
        }
    }
}