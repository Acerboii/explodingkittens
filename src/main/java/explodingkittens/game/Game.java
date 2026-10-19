package explodingkittens.game;

import explodingkittens.cards.*;
import explodingkittens.online.Player;
import explodingkittens.ui.ConsoleUI;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Map;

/**
 * Core Game engine managing players, deck, discard pile, and turn processing.
 */
public class Game implements GameContext, Serializable {
    private static final long serialVersionUID = 1L;

    private List<Player> players;
    private Deck deck;
    private DiscardPile discardPile;
    private int activePlayerIndex;

    private int turnsToTake;
    private int pendingTurnsToTake; // Issue 16: extra turns for the next player (set by Attack)
    private boolean isRunning;
    private transient ConsoleUI ui;

    // REQ 8: injectable RNG used to pick the random first player (testability)
    private Random random;

    // Multi-card play tracking — REQ 11f, 11g
    private int cardsPlayedThisTurn;
    private Player currentTurnPlayer;

    // Flag to track if the current card play should end the turn
    private boolean turnShouldEnd;

    // Issue 12: Nope timer tracking — timestamp when interrupt window opens
    // A negative value means "no window open"; the -1 sentinel avoids colliding with
    // clock readings (e.g. a ManualClock starting at 0 ms in tests).
    private static final long NO_INTERRUPT_WINDOW = -1L;
    private long interruptWindowOpenTime = NO_INTERRUPT_WINDOW;
    private static final long NOPE_WINDOW_MS = 5000; // 5 seconds

    // REQ 12/12a: injectable clock + Nope chain state. The closures persist across
    // multiple in-window Nopes so each Nope can alternate its meaning (Nope <-> Yup)
    // indefinitely - a third, fourth ... Nope keeps toggling until the window closes.
    private transient Clock clock;
    private transient Runnable pendingApply;   // effect closure for non-turn-ending cards (runs at resolution iff unblocked)
    private transient Runnable pendingUndo;    // reverses an already-applied Attack/Skip
    private transient Runnable pendingReapply; // re-applies an undone Attack/Skip
    private transient Runnable pendingSettle;  // finalizes a blocked Skip at resolution (pass-draw)
    private int nopeCount;                     // in-window Nopes played on the current card (parity decides the outcome)
    private String pendingCardName;
    private int preSkipActiveIndex;
    private int preSkipTurnsToTake;

    public Game() {
        this.players = new ArrayList<>();
        this.deck = new Deck();
        this.discardPile = new DiscardPile();
        this.random = new Random();
        this.activePlayerIndex = 0;
        this.turnsToTake = 1;
        this.pendingTurnsToTake = 1;
        this.isRunning = false;
        this.ui = new ConsoleUI();
        this.cardsPlayedThisTurn = 0;
        this.currentTurnPlayer = null;
        this.turnShouldEnd = false;
        this.interruptWindowOpenTime = NO_INTERRUPT_WINDOW;
        this.clock = new Clock.SystemClock();
    }

    public Game(List<Player> players, Deck deck) {
        this(players, deck, new DiscardPile());
    }

    public Game(List<Player> players, Deck deck, DiscardPile discardPile) {
        this.players = players != null ? new ArrayList<>(players) : new ArrayList<>();
        this.deck = deck != null ? deck : new Deck();
        this.discardPile = discardPile != null ? discardPile : new DiscardPile();
        this.random = new Random();
        this.activePlayerIndex = 0;
        this.turnsToTake = 1;
        this.pendingTurnsToTake = 1;
        this.isRunning = false;
        this.ui = new ConsoleUI();
        this.cardsPlayedThisTurn = 0;
        this.currentTurnPlayer = null;
        this.turnShouldEnd = false;
        this.interruptWindowOpenTime = NO_INTERRUPT_WINDOW;
        this.clock = new Clock.SystemClock();
    }

    /**
     * Creates a game with an explicit RNG for picking the random first player (REQ 8).
     *
     * @param players      the players in the game
     * @param deck         the draw deck
     * @param discardPile  the discard pile
     * @param random       the RNG used to pick the first player (null falls back to {@link Random})
     */
    public Game(List<Player> players, Deck deck, DiscardPile discardPile, Random random) {
        this(players, deck, discardPile);
        this.random = random != null ? random : new Random();
    }

    /**
     * Gets the RNG used to pick the random first player (REQ 8).
     */
    public Random getRandom() {
        return random;
    }

    /**
     * Sets the RNG used to pick the random first player (REQ 8).
     *
     * @param random the RNG to use (null falls back to a fresh {@link Random})
     */
    public void setRandom(Random random) {
        this.random = random != null ? random : new Random();
    }

    /** Gets the clock used for interrupt-window timing (REQ 12). */
    public Clock getClock() { return clock; }

    /** Sets the clock used for interrupt-window timing (REQ 12); null falls back to system time. */
    public void setClock(Clock clock) { this.clock = clock; }

    /** True while a nopeable card effect is waiting for the interrupt window to close (REQ 12). */
    /** True while a Nope chain is open (a card effect is still up for Nope/Yup toggling). */
    public boolean hasPendingEffect() {
        return pendingApply != null || pendingUndo != null || pendingReapply != null || pendingSettle != null;
    }

    /**
     * Resolves the open Nope chain (REQ 12/12a): applies or discards the deferred
     * effect based on the final parity of in-window Nopes, settles a blocked Skip
     * like a pass, and closes the interrupt window. An even number of Nopes means
     * the effect takes place; an odd number means it stays stopped.
     */
    public void resolvePendingEffect() {
        if (!hasPendingEffect()) {
            return;
        }
        boolean blocked = (nopeCount % 2) == 1;
        if (pendingApply != null) {
            Runnable r = pendingApply;
            pendingApply = null;
            if (blocked) {
                broadcast("Effect of " + pendingCardName + " remains stopped by Nope.");
            } else {
                r.run();
                broadcast("Effect of " + pendingCardName + " takes place.");
            }
        }
        if (blocked && pendingSettle != null) {
            pendingSettle.run();
        }
        pendingUndo = null;
        pendingReapply = null;
        pendingSettle = null;
        nopeCount = 0;
        pendingCardName = null;
        interruptWindowOpenTime = NO_INTERRUPT_WINDOW;
    }

    private long nowMs() {
        return clock != null ? clock.currentTimeMillis() : System.currentTimeMillis();
    }

    /**
     * Starts the game session.
     */
    public void start() {
        if (players.size() < 2 || players.size() > 5) {
            throw new IllegalStateException("Game requires between 2 and 5 players (managed: " + players.size() + ")");
        }
        this.isRunning = true;
        // REQ 8: A random player goes first. The RNG is injectable
        // (constructor overload / setRandom) so tests can verify the behavior
        // deterministically with a seeded Random.
        if (this.random == null) {
            this.random = new Random();
        }
        this.activePlayerIndex = this.random.nextInt(players.size());
        this.turnsToTake = 1;
        this.pendingTurnsToTake = 1;
        this.cardsPlayedThisTurn = 0;
        this.currentTurnPlayer = getActivePlayer();
        this.turnShouldEnd = false;
        this.interruptWindowOpenTime = NO_INTERRUPT_WINDOW;
        this.pendingApply = null;
        this.pendingUndo = null;
        this.pendingReapply = null;
        this.pendingSettle = null;
        this.nopeCount = 0;
        this.pendingCardName = null;
        if (ui != null) {
            ui.displayWelcome();
            ui.displayGameState(this, getActivePlayer());
        }
        broadcast("Game has started with " + players.size() + " players!");
        notifyActivePlayer();
    }

    /**
     * Processes a player's turn action.
     *
     * @param player the player performing the turn
     * @param action the action to process
     */
    public void processTurn(Player player, Action action) {
        if (!isRunning || player == null || player.isExploded()) {
            return;
        }
        // REQ 12: A Nope can be played by ANY player while the interrupt window is open,
        // even though it is not their turn.
        boolean isNopePlay = action != null && action.getCard() != null &&
                (action.getCard() instanceof NopeCard || "Nope".equalsIgnoreCase(action.getCard().getName()));
        boolean inWindowNope = isNopePlay && isInterruptWindowOpen();
        if (!player.equals(getActivePlayer()) && !inWindowNope) {
            player.sendMessage("It is not your turn!");
            return;
        }

        // REQ 12: Resolve any deferred card effect before processing a new action.
        // If the incoming action is a valid in-window Nope, it is handled here instead.
        if (inWindowNope) {
            Card nopeCard = action.getCard();
            if (!player.getHand().contains(nopeCard)) {
                return;
            }
            player.removeCard(nopeCard);
            discardPile.add(nopeCard);
            broadcast("Player " + player.getId() + " played Nope");
            if (!hasPendingEffect()) {
                player.sendMessage("Nope had no card effect to stop.");
                return;
            }
            // REQ 12a: each in-window Nope alternates the meaning (Nope <-> Yup), so
            // the chain can continue into a third, fourth ... Nope indefinitely.
            nopeCount++;
            boolean nowBlocked = (nopeCount % 2) == 1;
            if (pendingUndo != null || pendingReapply != null) {
                // Turn-ending card (Attack/Skip): its turn state must stay consistent
                // while the chain toggles, so the reversal/re-application runs now.
                Runnable r = nowBlocked ? pendingUndo : pendingReapply;
                r.run();
                broadcast(nowBlocked
                        ? "Effect of " + pendingCardName + " was stopped by Nope!"
                        : "Nope became Yup - effect of " + pendingCardName + " resumes!");
            } else {
                // Non-turn-ending card: the effect stays deferred and is applied (or
                // not) when the chain resolves, based on the final parity.
                broadcast(nowBlocked
                        ? "Effect of " + pendingCardName + " was stopped by Nope!"
                        : "Nope became Yup - effect of " + pendingCardName + " will resume!");
            }
            return;
        } else if (hasPendingEffect()) {
            resolvePendingEffect();
            // Resolution may have moved the turn (e.g. a blocked Skip settles like a
            // pass). A stale non-Nope action from the previous active player is rejected.
            Player activeAfterResolve = getActivePlayer();
            if (!player.equals(activeAfterResolve)) {
                if (activeAfterResolve != null) {
                    player.sendMessage("It is now the turn of Player " + activeAfterResolve.getId());
                }
                return;
            }
        }

        if (action == null || "PASS".equalsIgnoreCase(action.getType())) {
            // Pass ends the turn by drawing a card
            passEndsTurn(player);
        } else if ("PLAY_CARD".equalsIgnoreCase(action.getType()) || action.getCard() != null) {
            Card card = action.getCard();
            if (card != null && player.getHand().contains(card)) {

                // REQ 12: Enforce the 5-second interrupt window. A Nope played outside the
                // window is rejected entirely (it cannot be played as a normal card).
                if (card instanceof NopeCard || "Nope".equalsIgnoreCase(card.getName())) {
                    if (!isInterruptWindowOpen()) {
                        player.sendMessage("Nope can only be played within 5 seconds of a card play. Play rejected.");
                        return;
                    }
                    // Window open but no deferred effect left to stop (already resolved).
                    player.sendMessage("Nope had no card effect to stop.");
                    return;
                }

                player.removeCard(card);
                discardPile.add(card);
                broadcast("Player " + player.getId() + " played " + card.getName());
                if (ui != null) {
                    ui.displayAction("Player " + player.getId(), "Played " + card.getName());
                }

                // Track cards played this turn — REQ 11f, 11g
                if (currentTurnPlayer == null || currentTurnPlayer.equals(player)) {
                    cardsPlayedThisTurn++;
                    currentTurnPlayer = player;
                }

                // Reset turnShouldEnd flag before executing the card effect
                turnShouldEnd = false;

                // Issue 12 / REQ 12: Open the interrupt window and DEFER the card effect so a
                // valid Nope played within 5 seconds can stop it. The effect is applied when
                // the next action arrives (or via resolvePendingEffect()).
                boolean isNopeable = !isNopeImmune(card);
                if (isNopeable) {
                    openInterruptWindow();
                    this.pendingCardName = card.getName();
                    this.nopeCount = 0;
                    if (card instanceof AttackCard || "Attack".equalsIgnoreCase(card.getName())) {
                        // Turn-ending cards must apply BEFORE endTurn() (it transfers their
                        // state), so they execute immediately - an in-window Nope UNDOES them
                        // and a further Nope re-applies them (REQ 12/12a).
                        final Card fCard = card;
                        final Player fActor = player;
                        final Player fTarget = action.getTarget();
                        executeCardEffect(fCard, fActor, fTarget);
                        int grantedTurns = pendingTurnsToTake; // what the next player will take
                        this.pendingUndo = () -> setTurnsToTake(1);
                        this.pendingReapply = () -> setTurnsToTake(grantedTurns);
                    } else if (card instanceof SkipCard || "Skip".equalsIgnoreCase(card.getName())) {
                        this.preSkipActiveIndex = activePlayerIndex;
                        this.preSkipTurnsToTake = turnsToTake;
                        final Card fCard = card;
                        final Player fActor = player;
                        final Player fTarget = action.getTarget();
                        executeCardEffect(fCard, fActor, fTarget);
                        this.pendingUndo = this::undoSkip;
                        this.pendingReapply = this::reapplySkip;
                        this.pendingSettle = () -> settleBlockedSkip(players.get(preSkipActiveIndex));
                    } else {
                        // Non-turn-ending effects are DEFERRED until the Nope chain resolves
                        // so a valid Nope played within 5 seconds can stop them (REQ 12). The
                        // effect is applied iff the chain ends with an even number of Nopes
                        // (0 or 2 or 4 ...), i.e. its meaning was last flipped to Yup (REQ 12a).
                        final Card fCard = card;
                        final Player fActor = player;
                        final Player fTarget = action.getTarget();
                        this.pendingApply = () -> executeCardEffect(fCard, fActor, fTarget);
                    }
                } else {
                    // Issue 13: Card is immune to Nope (ExplodingKitten or Defuse) - execute directly
                    executeCardEffect(card, player, action.getTarget());
                }

                // REQ 11f: After playing 2 cards, steal random card from another player
                if (cardsPlayedThisTurn == 2) {
                    handleMultiCardSteal(player);
                }

                // REQ 11g: After playing 3 cards, name a card to receive
                if (cardsPlayedThisTurn == 3) {
                    handleMultiCardNameCard(player);
                }

                // End turn if this card is a turn-ending card (Attack, Skip)
                if (turnShouldEnd) {
                    endTurn();
                }
            }
        }
    }

    /**
     * Issue 13: Check if a card is immune to Nope.
     * Exploding Kitten and Defuse cards cannot be blocked by Nope.
     */
    private boolean isNopeImmune(Card card) {
        return card instanceof ExplodingKittenCard ||
               card instanceof DefuseCard ||
               "ExplodingKitten".equalsIgnoreCase(card.getName()) ||
               "Defuse".equalsIgnoreCase(card.getName());
    }

    /**
     * Issue 12: Open the 5-second interrupt window for Nope.
     */
    private void openInterruptWindow() {
this.interruptWindowOpenTime = nowMs();
        for (Player p : players) {
            if (!p.isExploded() && p.hasCard("Nope")) {
                p.sendMessage("Interrupt window: You have " + (NOPE_WINDOW_MS / 1000) + " seconds to play Nope.");
            }
        }
    }

    /**
     * Issue 12: Check if the Nope interrupt window is still open.
     * Returns true if the window is still within 5 seconds.
     */
    public boolean isInterruptWindowOpen() {
        if (interruptWindowOpenTime < 0) {
            return false;
        }
return (nowMs() - interruptWindowOpenTime) <= NOPE_WINDOW_MS;
    }

    /**
     * Issue 12: Get remaining time in the interrupt window (in milliseconds).
     */
    public long getInterruptWindowRemainingMs() {
        if (interruptWindowOpenTime < 0) {
            return 0;
        }
long remaining = NOPE_WINDOW_MS - (nowMs() - interruptWindowOpenTime);
        return Math.max(0, remaining);
    }

    /**
     * Executes the card-specific effect. Separated from processTurn for clarity.
     */
    private void executeCardEffect(Card card, Player actor, Player target) {
        if (card instanceof AttackCard) {
            // REQ 11a-i: Set pendingTurnsToTake for the NEXT player (Issue 16)
            // Chaining: if current player was attacked (turnsToTake > 1),
            // the chain accumulates: 2 → 4 → 6 → 8, etc.
            if (turnsToTake > 1) {
                pendingTurnsToTake = turnsToTake + 2;
            } else {
                pendingTurnsToTake = 2;
            }
            // Reset turnsToTake so endTurn() advances to the next player
            // (who will receive pendingTurnsToTake as their turnsToTake)
            turnsToTake = 1;
            // Attack ends the turn without drawing — set flag for processTurn to handle
            turnShouldEnd = true;
        } else if (card instanceof SkipCard) {
            // REQ 11d: Reset pendingTurnsToTake to 1 and end turn
            // Also reset current turnsToTake so the player's extra turns are cancelled
            pendingTurnsToTake = 1;
            turnsToTake = 1;
            // Skip ends the turn without drawing — set flag for processTurn to handle
            turnShouldEnd = true;
        } else {
            // For all other cards, delegate to the card's own executeEffect
            card.executeEffect(this, actor, target);
        }
    }

    /**
 * REQ 12: Undo an already-applied Skip - restore the skipper as the active
 * player with their pre-Skip turn count. The skipper's pass-draw is deferred
 * until the chain settles ({@link #settleBlockedSkip(Player)}), which keeps the
 * toggle fully reversible for further Nopes (REQ 12a).
 */
    private void undoSkip() {
        setActivePlayerIndex(preSkipActiveIndex);
        setTurnsToTake(preSkipTurnsToTake);
        notifyActivePlayer();
    }

    /**
     * REQ 12a: Re-apply an undone Skip - end the skipper's turn without drawing
     * and advance to the next alive player (mirrors the original endTurn transfer).
     */
    private void reapplySkip() {
        int count = players.size();
        for (int i = 1; i <= count; i++) {
            int nextIndex = (activePlayerIndex + i) % count;
            if (!players.get(nextIndex).isExploded()) {
                activePlayerIndex = nextIndex;
                break;
            }
        }
        turnsToTake = 1;
        pendingTurnsToTake = 1;
        cardsPlayedThisTurn = 0;
        currentTurnPlayer = getActivePlayer();
        notifyActivePlayer();
    }

    /**
     * REQ 12: Settle a blocked Skip when the Nope chain resolves - the skipper
     * completes their turn like a normal pass (draw a card, then end the turn).
     */
    private void settleBlockedSkip(Player skipper) {
        passEndsTurn(skipper);
    }

    /**
     * Ends a turn the way a pass does: draw a card (handling defuse/explosion),
     * reset turn tracking and hand over the turn. Shared by the PASS action and
     * by the settlement of a blocked Skip (REQ 12/12a).
     */
    private void passEndsTurn(Player player) {
        // Pass ends the turn by drawing a card
        Card drawn = deck.draw();
        if (drawn != null) {
            if (ui != null) {
                ui.displayCardDrawn(player, drawn);
            }
            if ("ExplodingKitten".equalsIgnoreCase(drawn.getName())) {
                // REQ 10: Check for defuse
                Card defuse = player.getCardByName("Defuse");
                if (defuse != null) {
                    player.removeCard(defuse);
                    discardPile.add(defuse);
                    int insertPos = chooseDefuseInsertionPosition(player);
                    deck.insertAt(drawn, insertPos);
                    player.sendMessage("You defused the " + drawn.getName() + " and returned it to the deck!");
                    broadcast("Player " + player.getId() + " defused a kitten.");
                    if (ui != null) {
                        ui.displayMessage("Player " + player.getId() + " defused a kitten!");
                    }
                } else {
                    player.setExploded(true);
                    discardPile.add(drawn);
                    for (Card c : new ArrayList<>(player.getHand())) {
                        discardPile.add(c);
                    }
                    player.getHand().clear();
                    player.sendMessage("You exploded!");
                    broadcast("Player " + player.getId() + " exploded!");
                    if (ui != null) {
                        ui.displayExplosion(player);
                    }
                }
            } else {
                player.addCard(drawn);
                player.sendMessage("You drew: " + drawn.getName());
            }
        }
        resetTurnTracking();
        endTurn();
    }

    /**
     * Fix 5: Choose where in the deck to place a defused kitten.
     * Online players answer over their network connection (never the server console,
     * which would hang); local human players are prompted via UI; bots default to
     * the top of the deck.
     */
    private int chooseDefuseInsertionPosition(Player player) {
        if (player.isOnline()) {
            return player.chooseDefusePosition(deck.size());
        }
        if (ui != null && !player.isBot()) {
            return ui.promptDefusePlacement(deck.size());
        }
        return 0;
    }

    /**
     * REQ 11f: After playing 2 cards, steal a random card from another player.
     */
    private void handleMultiCardSteal(Player actor) {
        // Find another player with cards
        for (Player p : players) {
            if (!p.equals(actor) && !p.isExploded() && !p.getHand().isEmpty()) {
                // Steal a random card
                int randomIndex = (int) (Math.random() * p.getHand().size());
                Card stolen = p.getHand().remove(randomIndex);
                actor.addCard(stolen);
                actor.sendMessage("You stole a " + stolen.getName() + " from Player " + p.getId() + "!");
                p.sendMessage("Player " + actor.getId() + " stole a " + stolen.getName() + " from you!");
                break;
            }
        }
    }

    /**
     * Issue 15: REQ 11g: After playing 3 cards, name a card to receive from another player.
     * Prompts the player to name a card and choose a target.
     */
    private void handleMultiCardNameCard(Player actor) {
        // Find available targets
        List<Player> availableTargets = new ArrayList<>();
        for (Player p : players) {
            if (!p.equals(actor) && !p.isExploded()) {
                availableTargets.add(p);
            }
        }

        if (availableTargets.isEmpty()) {
            actor.sendMessage("No available targets to receive a card from.");
            return;
        }

        // Prompt player to name a card and choose a target
        if (!actor.isBot()) {
            Card namedCard = null;
            if (actor.isOnline()) {
                // Fix 5: online players name the card over the network, never on the server console
                namedCard = actor.chooseNamedCard(availableTargets);
            } else if (ui != null) {
                namedCard = ui.promptNamedCard(actor, availableTargets);
            }
            if (namedCard != null) {
                actor.addCard(namedCard);
                actor.sendMessage("You received a " + namedCard.getName() + "!");
                return;
            }
        }

        // Bot or no UI fallback: try to find any card from any target
        for (Player p : availableTargets) {
            if (!p.getHand().isEmpty()) {
                Card anyCard = p.getHand().remove(0);
                actor.addCard(anyCard);
                actor.sendMessage("You received a " + anyCard.getName() + " from Player " + p.getId() + "!");
                p.sendMessage("Player " + actor.getId() + " took a " + anyCard.getName() + " from you!");
                return;
            }
        }
    }

    /**
     * Resets the turn tracking when the turn ends or changes player.
     * Note: interruptWindowOpenTime is NOT reset here — the Nope interrupt window
     * persists across turns so the next player can still play Nope within 5 seconds.
     * It is only reset when a Nope is actually played or when the window times out.
     */
    private void resetTurnTracking() {
        cardsPlayedThisTurn = 0;
        currentTurnPlayer = null;
        // Do NOT reset interruptWindowOpenTime — keep interrupt window open for Nope
    }

    /**
     * Notifies all players that an interrupt window has opened to play a Nope card.
     * Issue 12: Now includes timer enforcement.
     */
    public void notifyInterruptWindow() {
        // This method is kept for backward compatibility.
        // The actual timer logic is in openInterruptWindow() and isInterruptWindowOpen().
    }

    @Override
    public void endTurn() {
        if (getRemainingPlayersCount() <= 1) {
            isRunning = false;
            Player winner = getWinner();
            if (winner != null) {
                broadcast("Player " + winner.getId() + " has won the game!");
                if (ui != null) {
                    ui.displayWinner(winner);
                }
            }
            return;
        }

        // Issue 16: If current player has extra turns (from Attack), stay on them.
        // turnsToTake represents remaining turns for the CURRENT active player.
        if (turnsToTake > 1) {
            turnsToTake--;
            // Stay on current player — reset turn tracking
            cardsPlayedThisTurn = 0;
            currentTurnPlayer = getActivePlayer();
            // Do NOT reset interruptWindowOpenTime — keep interrupt window open for Nope
            notifyActivePlayer();
            return;
        }

        // Save pending turns for the next player (set by Attack during current turn)
        int savedPending = pendingTurnsToTake;

        // Advance to next alive player
        int count = players.size();
        for (int i = 1; i <= count; i++) {
            int nextIndex = (activePlayerIndex + i) % count;
            if (!players.get(nextIndex).isExploded()) {
                activePlayerIndex = nextIndex;
                break;
            }
        }

        // Transfer pending turns to the new active player
        turnsToTake = savedPending > 1 ? savedPending : 1;
        pendingTurnsToTake = 1; // Reset pending for future Attacks

        // Reset turn tracking for new player
        cardsPlayedThisTurn = 0;
        currentTurnPlayer = getActivePlayer();
        // Do NOT reset interruptWindowOpenTime — keep interrupt window open for Nope

        notifyActivePlayer();
    }

    private void notifyActivePlayer() {
        Player current = getActivePlayer();
        if (current != null) {
            for (Player p : players) {
                if (p.equals(current)) {
                    p.sendMessage("It is your turn.");
                } else {
                    p.sendMessage("It is now the turn of Player " + current.getId());
                }
            }
        }
    }

    public void broadcast(String message) {
        for (Player p : players) {
            p.sendMessage(message);
        }
    }

    public int getRemainingPlayersCount() {
        int count = 0;
        for (Player p : players) {
            if (!p.isExploded()) {
                count++;
            }
        }
        return count;
    }

    public Player getWinner() {
        for (Player p : players) {
            if (!p.isExploded()) {
                return p;
            }
        }
        return null;
    }

    public Player getActivePlayer() {
        if (players == null || players.isEmpty() || activePlayerIndex < 0 || activePlayerIndex >= players.size()) {
            return null;
        }
        return players.get(activePlayerIndex);
    }

    @Override
    public Deck getDeck() {
        return deck;
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    @Override
    public DiscardPile getDiscardPile() {
        return discardPile;
    }

    public void setDiscardPile(DiscardPile discardPile) {
        this.discardPile = discardPile;
    }

    @Override
    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public int getActivePlayerIndex() {
        return activePlayerIndex;
    }

    public void setActivePlayerIndex(int activePlayerIndex) {
        this.activePlayerIndex = activePlayerIndex;
    }

    public int getTurnsToTake() {
        return turnsToTake;
    }

    public void setTurnsToTake(int turnsToTake) {
        this.turnsToTake = turnsToTake;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public ConsoleUI getUI() {
        return ui;
    }

    public void setUI(ConsoleUI ui) {
        this.ui = ui;
    }

    /**
     * Returns the number of cards played by the current player this turn.
     * Used for REQ 11f (2 cards → steal) and REQ 11g (3 cards → name a card).
     */
    public int getCardsPlayedThisTurn() {
        return cardsPlayedThisTurn;
    }

    /**
     * Returns whether the current card play should end the turn.
     */
    public boolean getTurnShouldEnd() {
        return turnShouldEnd;
    }
}
