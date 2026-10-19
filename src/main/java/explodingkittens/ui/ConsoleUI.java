package explodingkittens.ui;

import explodingkittens.cards.Card;
import explodingkittens.game.Action;
import explodingkittens.game.GameContext;
import explodingkittens.online.Player;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Console user interface responsible for formatting game state, hands,
 * turn prompts, and interactive user inputs in the terminal.
 */
public class ConsoleUI {
    private final PrintStream out;
    private final Scanner scanner;

    public ConsoleUI() {
        this(System.in, System.out);
    }

    public ConsoleUI(InputStream in, PrintStream out) {
        this.out = out != null ? out : System.out;
        this.scanner = new Scanner(in != null ? in : System.in);
    }

    public void displayWelcome() {
        out.println("===============================================================");
        out.println("                 EXPLODING KITTENS - ONLINE                    ");
        out.println("===============================================================");
    }

    public void displayGameState(GameContext context, Player viewer) {
        if (context == null) return;
        out.println("\n----------------------- [GAME STATE] -----------------------");
        out.println("Draw Deck: " + (context.getDeck() != null ? context.getDeck().size() : 0) + " cards remaining");
        Card topDiscard = context.getDiscardPile() != null ? context.getDiscardPile().getTopCard() : null;
        out.println("Top Discard: " + (topDiscard != null ? topDiscard.getName() : "[Empty]"));

        out.print("Players alive: ");
        if (context.getPlayers() != null) {
            for (Player p : context.getPlayers()) {
                String status = p.isExploded() ? "[Dead]" : "[Alive, " + p.getHand().size() + " cards]";
                String me = (viewer != null && p.getId() == viewer.getId()) ? " (YOU)" : "";
                out.print("P" + p.getId() + me + " " + status + " | ");
            }
        }
        out.println();
        out.println("------------------------------------------------------------");
    }

    public void displayHand(Player player) {
        if (player == null) return;
        out.println("Your Hand (Player " + player.getId() + "):");
        List<Card> hand = player.getHand();
        if (hand == null || hand.isEmpty()) {
            out.println("  (Empty hand)");
            return;
        }
        for (int i = 0; i < hand.size(); i++) {
            out.println("  [" + (i + 1) + "] " + hand.get(i).getName());
        }
    }

    public void displayOptions(Player player, List<Player> targets) {
        out.println("\nAvailable Actions:");
        out.println("  [P] Pass (Draw a card from the deck and end turn)");
        if (player != null && !player.getHand().isEmpty()) {
            out.println("  [1-" + player.getHand().size() + "] Play the corresponding card from your hand");
        }
        if (targets != null && !targets.isEmpty()) {
            StringBuilder sb = new StringBuilder("  Available Targets: ");
            for (Player t : targets) {
                sb.append("Player ").append(t.getId()).append(" ");
            }
            out.println(sb.toString());
        }
    }

    public Action promptAction(Player player, GameContext context) {
        if (player == null) return Action.pass();
        displayHand(player);

        List<Player> availableTargets = new ArrayList<>();
        if (context != null && context.getPlayers() != null) {
            for (Player p : context.getPlayers()) {
                if (p.getId() != player.getId() && !p.isExploded()) {
                    availableTargets.add(p);
                }
            }
        }
        displayOptions(player, availableTargets);

        out.print("\nChoose an action (P or card number): ");
        if (!scanner.hasNextLine()) {
            return Action.pass();
        }

        String input = scanner.nextLine().trim();
        if (input.equalsIgnoreCase("P") || input.equalsIgnoreCase("Pass")) {
            return Action.pass();
        }

        try {
            int cardIndex = Integer.parseInt(input) - 1;
            List<Card> hand = player.getHand();
            if (cardIndex >= 0 && cardIndex < hand.size()) {
                Card selectedCard = hand.get(cardIndex);
                Player target = null;
                // If there are multiple targets and card may affect opponents
                if (!availableTargets.isEmpty()) {
                    target = promptTarget(availableTargets, player);
                }
                return Action.play(selectedCard, target);
            } else {
                out.println("Invalid card number. Defaulting to Pass.");
                return Action.pass();
            }
        } catch (NumberFormatException e) {
            out.println("Unrecognized input. Defaulting to Pass.");
            return Action.pass();
        }
    }

    public Card promptCard(Player player) {
        if (player == null || player.getHand().isEmpty()) return null;
        displayHand(player);
        out.print("Select a card number: ");
        if (scanner.hasNextLine()) {
            try {
                int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                if (idx >= 0 && idx < player.getHand().size()) {
                    return player.getHand().get(idx);
                }
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public Player promptTarget(List<Player> potentialTargets, Player actor) {
        if (potentialTargets == null || potentialTargets.isEmpty()) return null;
        if (potentialTargets.size() == 1) return potentialTargets.get(0);

        out.println("Select a target player:");
        for (int i = 0; i < potentialTargets.size(); i++) {
            Player p = potentialTargets.get(i);
            out.println("  [" + (i + 1) + "] Player " + p.getId());
        }
        out.print("Enter target choice: ");
        if (scanner.hasNextLine()) {
            try {
                int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                if (idx >= 0 && idx < potentialTargets.size()) {
                    return potentialTargets.get(idx);
                }
            } catch (NumberFormatException ignored) {}
        }
        return potentialTargets.get(0);
    }

    public int promptDefusePlacement(int deckSize) {
        out.println("\n*** YOU DEFUSED THE KITTEN! ***");
        out.print("Where in the deck do you want to place it? [0 = Top, " + deckSize + " = Bottom]: ");
        if (scanner.hasNextLine()) {
            try {
                int pos = Integer.parseInt(scanner.nextLine().trim());
                return Math.max(0, Math.min(pos, deckSize));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    public void displayMessage(String message) {
        out.println("[INFO]: " + message);
    }

    public void displayAction(String actor, String action) {
        out.println("\n>> [ACTION] " + actor + " -> " + action);
    }

    public void displayCardDrawn(Player player, Card card) {
        out.println("Player " + (player != null ? player.getId() : "?") + " drew: " + (card != null ? card.getName() : "nothing"));
    }

    public void displayExplosion(Player player) {
        out.println("\n***************************************************************");
        out.println("   BOOOOOOM! Player " + (player != null ? player.getId() : "?") + " exploded and is OUT of the game!");
        out.println("***************************************************************\n");
    }

    public void displayWinner(Player winner) {
        out.println("\n===============================================================");
        out.println("   WINNER: Player " + (winner != null ? winner.getId() : "Unknown") + " has won the game!");
        out.println("===============================================================\n");
    }

    public void displayDivider() {
        out.println("---------------------------------------------------------------");
    }

    /**
     * Issue 15: Prompt the player to name a card and choose a target player.
     * Used for REQ 11g (3 cards played).
     */
    public Card promptNamedCard(Player actor, List<Player> availableTargets) {
        if (actor == null || availableTargets == null || availableTargets.isEmpty()) return null;

        out.println("\n*** Name a card to receive from another player! ***");
        out.println("Available targets:");
        for (int i = 0; i < availableTargets.size(); i++) {
            Player p = availableTargets.get(i);
            out.println("  [" + (i + 1) + "] Player " + p.getId() + " (Hand: " + p.getHand().size() + " cards)");
        }
        out.print("Enter target player number: ");
        if (scanner.hasNextLine()) {
            try {
                int targetIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                if (targetIdx >= 0 && targetIdx < availableTargets.size()) {
                    Player target = availableTargets.get(targetIdx);
                    out.print("Name the card you want to receive: ");
                    if (scanner.hasNextLine()) {
                        String cardName = scanner.nextLine().trim();
                        // Try to find the named card in target's hand
                        Card namedCard = target.getCardByName(cardName);
                        if (namedCard != null) {
                            target.removeCard(namedCard);
                            return namedCard;
                        } else {
                            // Try partial match
                            for (Card c : target.getHand()) {
                                if (c.getName().toLowerCase().contains(cardName.toLowerCase())) {
                                    target.removeCard(c);
                                    return c;
                                }
                            }
                            out.println("Card '" + cardName + "' not found in Player " + target.getId() + "'s hand.");
                            return null;
                        }
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}