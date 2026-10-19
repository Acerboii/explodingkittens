package explodingkittens;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Standalone test runner that executes unit tests without requiring external libraries or Maven.
 */
public class TestRunner {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Running Exploding Kittens Standalone Test Suite...");
        System.out.println("==================================================");

        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;

        Class<?>[] testClasses = new Class<?>[]{
                UMLReflectionTest.class,
                GameMechanicsTest.class,
                CardEffectTest.class,
                NetworkGameFlowTest.class,
                NopeInterruptWindowTest.class,
                GameMenuTest.class,
                BotActionTest.class
        };

        for (Class<?> testClass : testClasses) {
            System.out.println("\nTesting: " + testClass.getSimpleName());
            System.out.println("--------------------------------------------------");
            Object instance;
            try {
                instance = testClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                System.err.println("Could not instantiate " + testClass.getSimpleName() + ": " + e.getMessage());
                continue;
            }

            for (Method method : testClass.getDeclaredMethods()) {
                if (method.getName().startsWith("test") && Modifier.isPrivate(method.getModifiers())) {
                    totalTests++;
                    try {
                        method.setAccessible(true);
                        method.invoke(instance);
                        System.out.println("  [PASS] " + method.getName());
                        passedTests++;
                    } catch (Throwable t) {
                        Throwable cause = t.getCause() != null ? t.getCause() : t;
                        System.err.println("  [FAIL] " + method.getName() + " -> " + cause.getMessage());
                        failedTests++;
                    }
                }
            }
        }

        System.out.println("\n==================================================");
        System.out.println("Test Results: " + passedTests + " passed, " + failedTests + " failed, " + totalTests + " total.");
        System.out.println("==================================================");

        System.exit(failedTests > 0 ? 1 : 0);
    }
}