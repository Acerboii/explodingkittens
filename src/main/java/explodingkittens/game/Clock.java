package explodingkittens.game;

/**
 * Abstraction over wall-clock time so the 5-second Nope interrupt window
 * (REQ 12) can be tested deterministically.
 *
 * REQ 19: Dependency injection for testability - tests can supply a fake
 * clock that advances time manually instead of waiting in real time.
 */
public interface Clock {

    /**
     * Returns the current time in milliseconds.
     *
     * @return current time in milliseconds
     */
    long currentTimeMillis();

    /**
     * Default clock backed by {@link System#currentTimeMillis()}.
     */
    class SystemClock implements Clock {
        @Override
        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }
    }
}