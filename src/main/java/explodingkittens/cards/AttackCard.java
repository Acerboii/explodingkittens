package explodingkittens.cards;

import explodingkittens.game.GameContext;
import explodingkittens.online.Player;

/**
 * Represents an Attack card which forces the next player to take turns without actor drawing a card.
 */
public class AttackCard extends Card {
    private static final long serialVersionUID = 1L;

    public AttackCard() {
        super("Attack");
    }

    @Override
    public void executeEffect(GameContext context, Player actor, Player target) {
        if (actor != null) {
            actor.sendMessage("Attack played: Ending turn without drawing.");
        }
        if (context != null) {
            context.endTurn();
        }
    }
}
