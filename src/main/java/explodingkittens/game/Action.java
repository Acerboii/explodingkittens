package explodingkittens.game;

import explodingkittens.cards.Card;
import explodingkittens.online.Player;
import java.io.Serializable;

/**
 * Represents a player's action during a turn.
 */
public class Action implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private Card card;
    private Player target;

    public Action(String type) {
        this(type, null, null);
    }

    public Action(Card card) {
        this("PLAY_CARD", card, null);
    }

    public Action(Card card, Player target) {
        this("PLAY_CARD", card, target);
    }

    public Action(String type, Card card, Player target) {
        this.type = type;
        this.card = card;
        this.target = target;
    }

    public static Action pass() {
        return new Action("PASS");
    }

    public static Action play(Card card) {
        return new Action("PLAY_CARD", card, null);
    }

    public static Action play(Card card, Player target) {
        return new Action("PLAY_CARD", card, target);
    }

    public String getType() {
        return type;
    }

    public Card getCard() {
        return card;
    }

    public Player getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "Action{" +
                "type='" + type + '\'' +
                ", card=" + card +
                ", target=" + (target != null ? target.getId() : "none") +
                '}';
    }
}
