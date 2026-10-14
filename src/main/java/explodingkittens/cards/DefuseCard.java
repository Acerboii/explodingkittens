package explodingkittens.cards;

import explodingkittens.game.GameContext;
import explodingkittens.online.Player;

/**
 * Represents a Defuse card which saves a player from exploding.
 */
public class DefuseCard extends Card {
    private static final long serialVersionUID = 1L;

    public DefuseCard() {
        super("Defuse");
    }

    @Override
    public void executeEffect(GameContext context, Player actor, Player target) {
        if (actor != null) {
            actor.sendMessage("Defuse card played: Exploding Kitten defused.");
        }
    }
}
