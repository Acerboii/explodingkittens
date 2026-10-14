package explodingkittens.online;

import explodingkittens.cards.Card;
import explodingkittens.game.Action;
import explodingkittens.game.Game;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Server managing client connections, message broadcasting, and game session coordination.
 */
public class Server {
    private Game game;
    private List<ClientHandler> clients;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public Server() {
        this(null);
    }

    public Server(Game game) {
        this.game = game;
        this.clients = Collections.synchronizedList(new ArrayList<>());
        this.running = false;
    }

    /**
     * Starts the server on the specified port and listens for client connections.
     *
     * @param port network port to listen on
     */
    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Server started on port " + port);

            Thread acceptThread = new Thread(() -> {
                while (running && !serverSocket.isClosed()) {
                    try {
                        Socket socket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(socket);
                        clients.add(handler);
                        handler.listen();
                        System.out.println("Client connected: " + socket.getRemoteSocketAddress());
                        broadcast("A new player has connected to the server.");

                        // Assign client to the next available online player
                        if (game != null) {
                            for (Player p : game.getPlayers()) {
                                if (p.isOnline() && p.getConnection() == null) {
                                    p.setConnection(handler);
                                    handler.sendMessage("You connected to the server as Player " + p.getId());
                                    System.out.println("Assigned client to Player " + p.getId());
                                    break;
                                }
                            }

                            // Check if all online players have connected
                            boolean allOnlineConnected = true;
                            for (Player p : game.getPlayers()) {
                                if (p.isOnline() && p.getConnection() == null) {
                                    allOnlineConnected = false;
                                    break;
                                }
                            }

                            if (allOnlineConnected && !game.isRunning()) {
                                System.out.println("All players connected! Launching game loop...");
                                broadcast("\nAll players have connected! Starting Exploding Kittens...\n");
                                startGameLoop();
                            }
                        }
                    } catch (IOException e) {
                        if (!running) break;
                    }
                }
            });
            acceptThread.start();
        } catch (IOException e) {
            System.err.println("Could not start server on port " + port + ": " + e.getMessage());
        }
    }

    /**
     * Starts the turn-based game loop in a background thread.
     */
    public void startGameLoop() {
        if (game == null) return;
        Thread gameThread = new Thread(() -> {
            try {
                game.start();

                while (game.isRunning() && game.getRemainingPlayersCount() > 1) {
                    Player active = game.getActivePlayer();
                    if (active == null || active.isExploded()) {
                        game.endTurn();
                        continue;
                    }

                    // Broadcast round status
                    broadcast("\n------------------------------------------------------------");
                    broadcast("                    ROUND: PLAYER " + active.getId() + "'S TURN");
                    broadcast("Draw Deck: " + game.getDeck().size() + " cards | Top Discard: " +
                            (game.getDiscardPile().getTopCard() != null ? game.getDiscardPile().getTopCard().getName() : "[Empty]"));
                    broadcast("------------------------------------------------------------");

                    // Send private hand view to each player
                    for (Player p : game.getPlayers()) {
                        if (!p.isExploded()) {
                            StringBuilder handMsg = new StringBuilder("\nYour Hand (Player " + p.getId() + "):\n");
                            List<Card> hand = p.getHand();
                            if (hand.isEmpty()) {
                                handMsg.append("  (Empty hand)\n");
                            } else {
                                for (int i = 0; i < hand.size(); i++) {
                                    handMsg.append("  [").append(i + 1).append("] ").append(hand.get(i).getName()).append("\n");
                                }
                            }
                            p.sendMessage(handMsg.toString());
                        }
                    }

                    // Prompt active player for their action
                    StringBuilder prompt = new StringBuilder();
                    prompt.append("\nYou have the following options:\n");
                    prompt.append("  [P] Pass (Draw a card from the deck and end your turn)\n");
                    List<Card> activeHand = active.getHand();
                    for (int i = 0; i < activeHand.size(); i++) {
                        prompt.append("  [").append(i + 1).append("] Play ").append(activeHand.get(i).getName()).append("\n");
                    }
                    prompt.append("Choose an action (P or card number): ");
                    active.sendMessage(prompt.toString());

                    // Receive action from the active player
                    String actionCmd = active.receiveAction();
                    if (actionCmd == null || actionCmd.trim().isEmpty() ||
                            actionCmd.trim().equalsIgnoreCase("pass") || actionCmd.trim().equalsIgnoreCase("p")) {
                        game.processTurn(active, Action.pass());
                    } else {
                        Action action = parseActionCommand(active, actionCmd.trim());
                        game.processTurn(active, action);
                    }

                    Thread.sleep(600);
                }

                Player winner = game.getWinner();
                if (winner != null) {
                    broadcast("\n===============================================================");
                    broadcast("   GAME OVER! Player " + winner.getId() + " has won the game!");
                    broadcast("===============================================================\n");
                }
            } catch (Exception e) {
                System.err.println("Game loop error: " + e.getMessage());
            }
        });
        gameThread.start();
    }

    private Action parseActionCommand(Player active, String input) {
        try {
            int cardIndex = Integer.parseInt(input) - 1;
            List<Card> hand = active.getHand();
            if (cardIndex >= 0 && cardIndex < hand.size()) {
                Card card = hand.get(cardIndex);
                Player target = null;
                for (Player p : game.getPlayers()) {
                    if (p.getId() != active.getId() && !p.isExploded()) {
                        target = p;
                        break;
                    }
                }
                return Action.play(card, target);
            }
        } catch (NumberFormatException ignored) {}

        Card byName = active.getCardByName(input);
        if (byName != null) {
            return Action.play(byName);
        }

        return Action.pass();
    }

    /**
     * Broadcasts a message to all connected clients.
     *
     * @param message the message string to broadcast
     */
    public void broadcast(String message) {
        if (clients != null) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.sendMessage(message);
                }
            }
        }
    }

    /**
     * Stops the server and closes client connections.
     */
    public void stop() {
        running = false;
        if (game != null) {
            game.setRunning(false);
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        if (clients != null) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.close();
                }
                clients.clear();
            }
        }
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public void addClient(ClientHandler client) {
        if (client != null) {
            clients.add(client);
        }
    }

    public boolean isRunning() {
        return running;
    }
}
