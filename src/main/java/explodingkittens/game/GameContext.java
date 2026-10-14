package explodingkittens.game;

import explodingkittens.online.Player;
import java.util.List;

/**
 * Interface representing the game context accessible to cards and game systems.
 */
public interface GameContext {
    Deck getDeck();
    DiscardPile getDiscardPile();
    List<Player> getPlayers();
    void endTurn();
}
