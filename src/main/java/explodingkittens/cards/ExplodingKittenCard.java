package explodingkittens.cards;

import explodingkittens.game.GameContext;
import explodingkittens.online.Player;

/**
 * Represents an Exploding Kitten card — the core mechanic of the game.
 * When drawn, the player must play a Defuse card or explode and lose.
 * Drawn on pass: defused with a Defuse card, or the player explodes.
 */
public class ExplodingKittenCard extends Card {
    private static final long serialVersionUID = 1L;

    public ExplodingKittenCard() {
        super("ExplodingKitten");
    }

    @Override
    public void executeEffect(GameContext context, Player actor, Player target) {
        // Exploding Kitten effect is handled in Game.processTurn() when drawn,
        // not via executeEffect(). This method exists to satisfy the Card contract.
        if (actor != null) {
            actor.sendMessage("You drew an Exploding Kitten! Defuse it or explode!");
        }
    }
}