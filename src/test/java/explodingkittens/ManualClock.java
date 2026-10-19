package explodingkittens;

import explodingkittens.game.Clock;

/**
 * Fake clock for deterministic time-based tests (REQ 12 interrupt window).
 * Time starts at 0 ms and advances manually via {@link #advance(long)} so
 * the 5-second Nope interrupt window can be tested without real waiting.
 */
public class ManualClock implements Clock {
    private long now = 0;

    @Override
    public long currentTimeMillis() {
        return now;
    }

    /** Advances the fake clock by the given number of milliseconds. */
    public void advance(long ms) {
        now += ms;
    }

    /** Sets the fake clock to an exact millisecond value. */
    public void setNow(long ms) {
        now = ms;
    }
}
