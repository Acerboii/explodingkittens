package explodingkittens;

import explodingkittens.game.ExpansionType;
import explodingkittens.game.Game;
import explodingkittens.game.InitGame;
import explodingkittens.online.Client;
import explodingkittens.online.Server;
import java.util.Arrays;
import java.util.List;

/**
 * Main application entry point for Exploding Kittens.
 * Connects to Server, Client, and InitGame.
 */
public class Main {

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        int defaultPort = 2048;

        if (args == null || args.length == 0) {
            printUsage();
            System.out.println("\nStarting a local demo game with 2 players and 1 bot...");
            InitGame initGame = new InitGame();
            Game game = initGame.initializeGame(2, 1, List.of(ExpansionType.BASE));
            game.start();
            return;
        }

        String firstArg = args[0].trim();

        // Check explicit "server" or "client" keywords
        if ("server".equalsIgnoreCase(firstArg)) {
            int numPlayers = (args.length >= 2) ? parseIntOrDefault(args[1], 2) : 2;
            int numBots = (args.length >= 3) ? parseIntOrDefault(args[2], 0) : 0;
            int port = (args.length >= 4) ? parseIntOrDefault(args[3], defaultPort) : defaultPort;
            startServer(numPlayers, numBots, port);
            return;
        }

        if ("client".equalsIgnoreCase(firstArg)) {
            String ip = (args.length >= 2) ? args[1] : "127.0.0.1";
            int port = (args.length >= 3) ? parseIntOrDefault(args[2], defaultPort) : defaultPort;
            startClient(ip, port);
            return;
        }

        // Auto-detect based on first argument:
        // If first argument is an integer, it is Server syntax: <numPlayers> <numBots> [port]
        // If first argument is an IP address/hostname (not integer), it is Client syntax: <IP> [port]
        if (isInteger(firstArg)) {
            int numPlayers = Integer.parseInt(firstArg);
            int numBots = (args.length >= 2) ? parseIntOrDefault(args[1], 0) : 0;
            int port = (args.length >= 3) ? parseIntOrDefault(args[2], defaultPort) : defaultPort;
            startServer(numPlayers, numBots, port);
        } else {
            String ip = firstArg;
            int port = (args.length >= 2) ? parseIntOrDefault(args[1], defaultPort) : defaultPort;
            startClient(ip, port);
        }
    }

    private static void startServer(int numPlayers, int numBots, int port) {
        System.out.println("Starting Exploding Kittens Server on port " + port +
                " for " + numPlayers + " players and " + numBots + " bots...");
        InitGame initGame = new InitGame();
        List<ExpansionType> expansions = Arrays.asList(
                ExpansionType.BASE,
                ExpansionType.IMPLODING_KITTENS,
                ExpansionType.STREAKING_KITTENS,
                ExpansionType.BARKING_KITTENS
        );
        Game game = initGame.initializeGame(numPlayers, numBots, expansions);
        Server server = new Server(game);
        server.start(port);
    }

    private static void startClient(String ip, int port) {
        System.out.println("Connecting to Exploding Kittens Server at " + ip + ":" + port + "...");
        Client client = new Client();
        try {
            client.connect(ip, port);
        } catch (Exception e) {
            System.err.println("Failed to connect to server at " + ip + ":" + port + ": " + e.getMessage());
        }
    }

    private static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static int parseIntOrDefault(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static void printUsage() {
        System.out.println("Exploding Kittens");
        System.out.println("Usage:");
        System.out.println("  Server: java explodingkittens.Main <numPlayers> <numBots> [port]");
        System.out.println("          java explodingkittens.Main server <numPlayers> <numBots> [port]");
        System.out.println("  Client: java explodingkittens.Main <serverIP> [port]");
        System.out.println("          java explodingkittens.Main client <serverIP> [port]");
    }
}
