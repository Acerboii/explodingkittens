package explodingkittens.ui;

import explodingkittens.game.ExpansionType;
import explodingkittens.game.InitGame;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Interactive game menu for selecting which expansions or other game
 * variants to play with (REQ 15).
 *
 * <p>Design notes:</p>
 * <ul>
 *   <li><b>Single Responsibility:</b> this class only presents the menu and
 *       collects the selection; {@link InitGame} owns the knowledge of which
 *       expansions are actually implemented.</li>
 *   <li><b>Open/Closed (extensibility):</b> when a future expansion is
 *       implemented (enum entry + deck entries + CardFactory registration),
 *       the menu automatically offers it without any change to this class.</li>
 *   <li><b>Testability / Dependency Inversion:</b> input and output streams
 *       are injectable (mirroring {@link ConsoleUI}), so the menu can be
 *       unit-tested with scripted input.</li>
 * </ul>
 */
public class GameMenu {

    private final PrintStream out;
    private final Scanner scanner;

    /** Creates a menu bound to the standard console. */
    public GameMenu() {
        this(System.in, System.out);
    }

    /**
     * Creates a menu bound to the supplied streams (used by unit tests).
     *
     * @param in  input stream to read choices from
     * @param out output stream to render the menu on
     */
    public GameMenu(InputStream in, PrintStream out) {
        this.scanner = new Scanner(in);
        this.out = out;
    }

    /**
     * Displays the expansion selection menu and returns the user's choice.
     *
     * <p>The base game is always included in the result. Expansions that are
     * not yet implemented are shown as unavailable and cannot be selected,
     * so with no expansions implemented the menu only lets the player play
     * the base game. An empty line selects the base game directly, and
     * 'q' quits.</p>
     *
     * @param initGame the initializer used to query which expansions are implemented
     * @return selected expansions (always containing BASE), or null if the
     *         user quit or the input stream ended
     */
    public List<ExpansionType> chooseExpansions(InitGame initGame) {
        while (true) {
            displayHeader();
            ExpansionType[] all = ExpansionType.values();
            for (int i = 0; i < all.length; i++) {
                out.println("  [" + (i + 1) + "] " + displayName(all[i]) + " " + statusOf(all[i], initGame));
            }
            out.print("Enter choice(s), comma-separated (empty = base game, 'q' = quit): ");
            if (!scanner.hasNextLine()) {
                return null; // input ended - treat as quit
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                return List.of(ExpansionType.BASE);
            }
            if (line.equalsIgnoreCase("q")) {
                return null;
            }

            List<ExpansionType> selected = new ArrayList<>();
            boolean invalid = false;
            for (String token : line.split("[,\\s]+")) {
                ExpansionType expansion = resolve(token, all);
                if (expansion == null) {
                    out.println("Unknown option: " + token);
                    invalid = true;
                    continue;
                }
                if (!initGame.isImplemented(expansion)) {
                    out.println(displayName(expansion) + " is not yet available.");
                    invalid = true;
                    continue;
                }
                if (!selected.contains(expansion)) {
                    selected.add(expansion);
                }
            }
            if (invalid) {
                continue; // re-prompt with a fresh menu display
            }
            if (!selected.contains(ExpansionType.BASE)) {
                selected.add(0, ExpansionType.BASE);
            }
            return selected;
        }
    }

    private void displayHeader() {
        out.println();
        out.println("=== Exploding Kittens - Game Menu ===");
        out.println("Select which expansions to play with:");
    }

    private String statusOf(ExpansionType expansion, InitGame initGame) {
        if (expansion == ExpansionType.BASE) {
            return "(always included)";
        }
        return initGame.isImplemented(expansion) ? "(available)" : "(not yet available)";
    }

    /**
     * Resolves a user token to an expansion by menu index or by name.
     *
     * @param token the token entered by the user
     * @param all   all known expansions (defines valid indices)
     * @return the matching expansion, or null if the token is unknown
     */
    private ExpansionType resolve(String token, ExpansionType[] all) {
        try {
            int index = Integer.parseInt(token);
            if (index >= 1 && index <= all.length) {
                return all[index - 1];
            }
        } catch (NumberFormatException ignored) {
            // fall through to name matching
        }
        String normalized = token.toLowerCase().replace('_', ' ').replace('-', ' ');
        for (ExpansionType expansion : all) {
            if (displayName(expansion).equals(normalized)
                    || expansion.name().toLowerCase().replace('_', ' ').equals(normalized)) {
                return expansion;
            }
        }
        return null;
    }

    /**
     * Human-readable expansion name (e.g. "imploding kittens").
     *
     * @param expansion the expansion to name
     * @return lowercase space-separated display name
     */
    public static String displayName(ExpansionType expansion) {
        return expansion.name().toLowerCase().replace('_', ' ');
    }
}
