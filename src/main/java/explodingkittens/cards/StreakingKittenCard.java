package explodingkittens.cards;

import explodingkittens.game.GameContext;
import explodingkittens.online.Player;

/**
 * Represents a Streaking Kitten card from the Streaking Kittens expansion.
 */
public class StreakingKittenCard extends Card {
    private static final long serialVersionUID = 1L;

    public StreakingKittenCard() {
        super("StreakingKitten");
    }

    @Override
    public void executeEffect(GameContext context, Player actor, Player target) {
        if (actor != null) {
            actor.sendMessage("Streaking Kitten is active: You can secretly hold 1 Exploding Kitten.");
        }
    }
}
