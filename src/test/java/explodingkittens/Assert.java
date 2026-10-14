package explodingkittens;

import java.util.Objects;

/**
 * Lightweight assertion utility for unit tests without external library dependencies.
 */
public final class Assert {

    private Assert() {}

    public static void assertTrue(boolean condition) {
        assertTrue(condition, "Expected true but was false");
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertFalse(boolean condition) {
        assertFalse(condition, "Expected false but was true");
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        assertEquals(expected, actual, "Expected: " + expected + ", but got: " + actual);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " (Expected: " + expected + ", Got: " + actual + ")");
        }
    }

    public static void assertNull(Object actual) {
        if (actual != null) {
            throw new AssertionError("Expected null, but got: " + actual);
        }
    }

    public static void assertNotNull(Object actual) {
        assertNotNull(actual, "Expected non-null object, but got null");
    }

    public static void assertNotNull(Object actual, String message) {
        if (actual == null) {
            throw new AssertionError(message);
        }
    }

    public static void assertThrows(Class<? extends Throwable> expectedType, Runnable executable, String message) {
        try {
            executable.run();
        } catch (Throwable t) {
            if (expectedType.isInstance(t)) {
                return;
            }
            throw new AssertionError(message + " - Expected exception: " + expectedType.getName() + ", but caught: " + t.getClass().getName());
        }
        throw new AssertionError(message + " - Expected exception: " + expectedType.getName() + " was not thrown");
    }
}
