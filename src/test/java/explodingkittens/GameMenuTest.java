package explodingkittens;

import explodingkittens.game.ExpansionType;
import explodingkittens.game.InitGame;
import explodingkittens.ui.GameMenu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static explodingkittens.Assert.assertEquals;
import static explodingkittens.Assert.assertNull;
import static explodingkittens.Assert.assertTrue;

/**
 * Unit tests for the interactive game menu (REQ 15).
 *
 * <p>The menu is exercised with scripted input streams so no real console is
 * needed. With no expansions implemented, every non-BASE option is reported
 * as unavailable and only the base game can be selected.</p>
 */
public class GameMenuTest {

    private final ByteArrayOutputStream lastOutput = new ByteArrayOutputStream();

    /** Runs the menu with the given scripted input and returns its result. */
    private List<ExpansionType> runMenu(String input) {
        lastOutput.reset();
        GameMenu menu = new GameMenu(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(lastOutput, true));
        return menu.chooseExpansions(new InitGame());
    }

    /** Returns everything the menu printed during the last run. */
    private String output() {
        return new String(lastOutput.toByteArray(), StandardCharsets.UTF_8);
    }

    private void testBaseOnlySelectionByIndex() {
        List<ExpansionType> selected = runMenu("1\n");
        assertEquals(List.of(ExpansionType.BASE), selected);
    }

    private void testEmptyLineSelectsBaseGame() {
        List<ExpansionType> selected = runMenu("\n");
        assertEquals(List.of(ExpansionType.BASE), selected);
    }

    private void testNameBasedSelection() {
        List<ExpansionType> selected = runMenu("base\n");
        assertEquals(List.of(ExpansionType.BASE), selected);
    }

    private void testUnimplementedExpansionIsRejectedThenBaseAccepted() {
        // "2" = imploding kittens (not implemented) -> rejected, then "1" accepted
        List<ExpansionType> selected = runMenu("2\n1\n");
        assertEquals(List.of(ExpansionType.BASE), selected);
        assertTrue(output().contains("imploding kittens is not yet available"),
                "Menu must report that the expansion is unavailable: " + output());
    }

    private void testMultipleChoicesWithUnavailableAreRejectedThenBaseSelected() {
        // 1 = BASE, 3 = streaking kittens, 4 = barking kittens
        // A line mixing valid and unavailable choices is rejected wholesale (with
        // an explanation per choice) and the menu re-prompts; then "1" selects base.
        List<ExpansionType> selected = runMenu("1,3,4\n1\n");
        assertEquals(List.of(ExpansionType.BASE), selected);
        assertTrue(output().contains("streaking kittens is not yet available"),
                "Menu must reject streaking kittens: " + output());
        assertTrue(output().contains("barking kittens is not yet available"),
                "Menu must reject barking kittens: " + output());
    }

    private void testUnknownOptionRepromptsUntilQuit() {
        List<ExpansionType> selected = runMenu("9\nq\n");
        assertNull(selected, "Quitting after an unknown option must return null");
        assertTrue(output().contains("Unknown option: 9"),
                "Menu must report the unknown option: " + output());
    }

    private void testQuitReturnsNull() {
        assertNull(runMenu("q\n"));
    }

    private void testMenuListsAllExpansionsWithAvailabilityStatus() {
        runMenu("q\n");
        String printed = output();
        assertTrue(printed.contains("base (always included)"),
                "Menu must list base game as always included: " + printed);
        assertTrue(printed.contains("imploding kittens (not yet available)"),
                "Menu must list imploding kittens as unavailable: " + printed);
        assertTrue(printed.contains("streaking kittens (not yet available)"),
                "Menu must list streaking kittens as unavailable: " + printed);
        assertTrue(printed.contains("barking kittens (not yet available)"),
                "Menu must list barking kittens as unavailable: " + printed);
    }
}