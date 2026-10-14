package explodingkittens.cards;

import explodingkittens.game.GameContext;
import explodingkittens.online.Player;

/**
 * Represents a Barking Kitten card from the Barking Kittens expansion.
 */
public class BarkingKittenCard extends Card {
    private static final long serialVersionUID = 1L;

    public BarkingKittenCard() {
        super("BarkingKitten");
    }

    @Override
    public void executeEffect(GameContext context, Player actor, Player target) {
        if (actor != null) {
            actor.sendMessage("Barking Kitten played.");
        }
        if (target != null) {
            target.sendMessage("Player " + (actor != null ? actor.getId() : "") + " played Barking Kitten against you!");
        }
    }
}
