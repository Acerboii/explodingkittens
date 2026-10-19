package explodingkittens.cards;

import explodingkittens.game.GameContext;
import explodingkittens.online.Player;

/**
 * Represents a Favor card which forces another player to give you 1 card
 * of their choice.
 *
 * REQ 3c: 4 Favor cards in deck.
 * REQ 11b: Playing a Favor card forces another player to give you 1 card of their choice.
 */
public class FavorCard extends Card {
    private static final long serialVersionUID = 1L;

    public FavorCard() {
        super("Favor");
    }

    @Override
    public void executeEffect(GameContext context, Player actor, Player target) {
        if (actor != null) {
            actor.sendMessage("Favor played: " + (target != null ? "Player " + target.getId() : "target") + " must give a card.");
        }

        // REQ 11b: Force target player to CHOOSE a card to give to actor
        if (context != null && target != null && !target.getHand().isEmpty()) {
            // The target player chooses which card to give
            explodingkittens.cards.Card chosenCard = target.chooseCardToGive();
            if (chosenCard != null) {
                actor.addCard(chosenCard);

                if (target != null) {
                    target.sendMessage("You gave a " + chosenCard.getName() + " to Player " + actor.getId() + " due to Favor.");
                }
                actor.sendMessage("You received a " + chosenCard.getName() + " from Player " + target.getId());
            } else {
                actor.sendMessage("Player " + target.getId() + " has no cards to give.");
            }
        }
    }
}