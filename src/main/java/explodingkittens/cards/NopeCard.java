package explodingkittens.cards;

import explodingkittens.game.GameContext;
import explodingkittens.online.Player;

/**
 * Represents a Nope card which stops the action of another card.
 */
public class NopeCard extends Card {
    private static final long serialVersionUID = 1L;

    public NopeCard() {
        super("Nope");
    }

    @Override
    public void executeEffect(GameContext context, Player actor, Player target) {
        if (actor != null) {
            actor.sendMessage("Nope played: Stopping previous action.");
        }
    }
}
