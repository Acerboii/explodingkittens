package explodingkittens.cards;

import explodingkittens.game.GameContext;
import explodingkittens.online.Player;

/**
 * Represents an Attack card which forces the next player to take turns
 * without drawing a card.
 *
 * REQ 11a: Playing an Attack card will end the turn without drawing a card.
 * REQ 11a-i: The next Player takes 2 turns in a row unless that Player plays
 * another Attack card, in which case the consecutive player has to take
 * an additional 2 turns (e.g. 4 turns, then 6, etc.).
 */
public class AttackCard extends Card {
    private static final long serialVersionUID = 1L;

    public AttackCard() {
        super("Attack");
    }

    @Override
    public void executeEffect(GameContext context, Player actor, Player target) {
        if (actor != null) {
            actor.sendMessage("Attack played: Ending turn without drawing. Next player takes 2 turns!");
        }
        // Note: Attack turn-ending and chaining logic is handled in Game.executeCardEffect()
        // to properly support multi-card play tracking (REQ 11f/11g).
    }
}