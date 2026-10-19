package explodingkittens.cards;

import explodingkittens.game.GameContext;
import explodingkittens.online.Player;

/**
 * Represents a HairyPotatoCat card — a Cat Card that forms combos.
 * Individual Cat Cards have no effect. Playing a matching pair steals a random
 * card from another player. Playing a matching trio lets you name a card.
 *
 * REQ 3h (4 in deck), REQ 11e (wild card for SeeTheFuture)
 */
public class HairyPotatoCatCard extends Card {
    private static final long serialVersionUID = 1L;

    public HairyPotatoCatCard() {
        super("HairyPotatoCat");
    }

    @Override
    public void executeEffect(GameContext context, Player actor, Player target) {
        if (actor != null) {
            actor.sendMessage("HairyPotatoCat played: Acts as a wild card for SeeTheFuture. " +
                    "Play matching pairs/trios for combo effects.");
        }
        // Individual Cat Cards have no effect. Matching pairs/trios are covered by the
        // type-agnostic REQ 11f/11g multi-card checks in Game.processTurn().
    }
}