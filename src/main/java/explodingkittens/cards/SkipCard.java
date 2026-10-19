package explodingkittens.cards;

import explodingkittens.game.GameContext;
import explodingkittens.online.Player;

/**
 * Represents a Skip card which ends your turn without drawing a card
 * (only counts as 1 turn in case of taking multiple turns).
 *
 * REQ 3f: 4 Skip cards in deck.
 * REQ 11d: Playing a Skip card will end your turn without drawing a card
 * (only 1 turn in case of taking multiple turns).
 */
public class SkipCard extends Card {
    private static final long serialVersionUID = 1L;

    public SkipCard() {
        super("Skip");
    }

    @Override
    public void executeEffect(GameContext context, Player actor, Player target) {
        if (actor != null) {
            actor.sendMessage("Skip played: Ending turn without drawing.");
        }
        // Note: Skip turn-ending logic is handled in Game.executeCardEffect()
        // to properly support multi-card play tracking (REQ 11f/11g).
    }
}