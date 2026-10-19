package explodingkittens.cards;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Extensible factory that creates cards by name.
 *
 * <p>This is the extension point for future expansions (REQ 14): a new expansion
 * registers its card names here and adds its deck entries in
 * {@code InitGame.EXPANSION_DECKS} without touching core game logic.</p>
 *
 * <p>The constructor registers ALL base cards using the exact name strings
 * already used throughout the code (hasCard()/getCardByName()).</p>
 */
public class CardFactory {

    private final Map<String, Supplier<Card>> factories = new HashMap<>();

    /**
     * Creates a factory with all base cards registered by name.
     */
    public CardFactory() {
        register("Defuse", DefuseCard::new);
        register("Attack", AttackCard::new);
        register("Favor", FavorCard::new);
        register("Nope", NopeCard::new);
        register("Shuffle", ShuffleCard::new);
        register("Skip", SkipCard::new);
        register("SeeTheFuture", SeeTheFutureCard::new);
        register("HairyPotatoCat", HairyPotatoCatCard::new);
        register("Cattermelon", CattermelonCard::new);
        register("RainbowRalphingCat", RainbowRalphingCatCard::new);
        register("TacoCat", TacoCatCard::new);
        register("OverweightBikiniCat", OverweightBikiniCatCard::new);
    }

    /**
     * Registers a card supplier under the given name (overwrites existing).
     *
     * @param name    the card name to register
     * @param factory the supplier producing the card
     */
    public void register(String name, Supplier<Card> factory) {
        if (name == null || factory == null) {
            throw new IllegalArgumentException("Card name and factory must not be null");
        }
        factories.put(name, factory);
    }

    /**
     * Creates a card instance for the given name.
     *
     * @param name the registered card name
     * @return a new card instance
     * @throws IllegalArgumentException if the name is not registered
     */
    public Card create(String name) {
        Supplier<Card> factory = factories.get(name);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown card: " + name);
        }
        return factory.get();
    }

    /**
     * Checks whether a card name is registered.
     *
     * @param name the card name to check
     * @return true if the name has a registered factory
     */
    public boolean isRegistered(String name) {
        return factories.containsKey(name);
    }

    /**
     * Test hook: removes all registered cards so they can be re-registered.
     */
    void clear() {
        factories.clear();
    }
}
