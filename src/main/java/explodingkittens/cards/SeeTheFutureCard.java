package explodingkittens.cards;

import explodingkittens.game.GameContext;
import explodingkittens.online.Player;

import java.util.List;

/**
 * Represents a SeeTheFuture card which allows the player to view the top 3 cards.
 *
 * REQ 3g: 5 SeeTheFuture cards in deck.
 * REQ 11e: Playing a SeeTheFuture card will allow the Player to view the top 3 cards in the deck.
 */
public class SeeTheFutureCard extends Card {
    private static final long serialVersionUID = 1L;

    public SeeTheFutureCard() {
        super("SeeTheFuture");
    }

    @Override
    public void executeEffect(GameContext context, Player actor, Player target) {
        if (actor != null && context != null) {
            // REQ 11e: Reveal top 3 cards to the actor
            List<Card> topCards = context.getDeck().getCards();
            int revealCount = Math.min(3, topCards.size());

            StringBuilder msg = new StringBuilder("SeeTheFuture reveals: ");
            for (int i = 0; i < revealCount; i++) {
                if (i > 0) msg.append(", ");
                msg.append(topCards.get(i).getName());
            }
            actor.sendMessage(msg.toString());
        }
    }
}