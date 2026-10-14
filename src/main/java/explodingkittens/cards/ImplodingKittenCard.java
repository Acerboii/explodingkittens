package explodingkittens.cards;

import explodingkittens.game.GameContext;
import explodingkittens.online.Player;

/**
 * Represents an Imploding Kitten card from the Imploding Kittens expansion.
 */
public class ImplodingKittenCard extends Card {
    private static final long serialVersionUID = 1L;

    public ImplodingKittenCard() {
        super("ImplodingKitten");
    }

    @Override
    public void executeEffect(GameContext context, Player actor, Player target) {
        if (actor != null) {
            actor.sendMessage("Imploding Kitten activated!");
        }
    }
}
