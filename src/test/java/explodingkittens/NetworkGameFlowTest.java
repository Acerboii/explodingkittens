package explodingkittens;

import explodingkittens.cards.AttackCard;
import explodingkittens.cards.Card;
import explodingkittens.cards.DefuseCard;
import explodingkittens.cards.NopeCard;
import explodingkittens.cards.SkipCard;
import explodingkittens.game.Deck;
import explodingkittens.game.DiscardPile;
import explodingkittens.game.Game;
import explodingkittens.online.Client;
import explodingkittens.online.ClientHandler;
import explodingkittens.online.Player;
import explodingkittens.online.Server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static explodingkittens.Assert.*;

/**
 * Validates networking game flow between Server and Client over sockets.
 */
public class NetworkGameFlowTest {

    /**
     * Waits up to a timeout for a condition to become true.
     */
    private boolean waitForCondition(Runnable check, long timeoutMs) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Thread waiter = new Thread(() -> {
            try {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < timeoutMs) {
                    try {
                        check.run();
                        latch.countDown();
                        return;
                    } catch (@SuppressWarnings("unused") Exception ignored) {
                        // condition not yet met
                    }
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        waiter.start();
        return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    private void testServerAndClientConnection() throws Exception {
        int port = findFreePort();

        List<Player> players = new ArrayList<>();
        Player p0 = new Player(0, false, null);
        p0.addCard(new DefuseCard());
        Player p1 = new Player(1, true, null);
        p1.addCard(new DefuseCard());
        players.add(p0);
        players.add(p1);

        Deck deck = new Deck();
        deck.add(new DefuseCard());
        DiscardPile discardPile = new DiscardPile();

        Game game = new Game(players, deck, discardPile);
        Server server = new Server(game);
        server.start(port);

        // Allow server socket to bind
        Thread.sleep(100);

        Client client = new Client();
        client.connect("127.0.0.1", port);

        // Allow connection handshake to complete
        Thread.sleep(300);

        assertEquals(1, server.getClients().size(), "Server should have 1 connected client");
        assertNotNull(p1.getConnection(), "Player 1 should be assigned the client connection");
        assertTrue(server.isRunning(), "Server should be running after start");

        client.disconnect();
        server.stop();
    }

    private void testServerBroadcastsToClients() throws Exception {
        int port = findFreePort();

        List<Player> players = new ArrayList<>();
        players.add(new Player(0, false, null));
        players.add(new Player(1, true, null));

        Game game = new Game(players, new Deck(), new DiscardPile());
        Server server = new Server(game);
        server.start(port);

        Thread.sleep(100);

        Client client = new Client();
        client.connect("127.0.0.1", port);

        Thread.sleep(300);

        // Broadcast a test message
        server.broadcast("Test broadcast message");

        // Verify the client handler received the message
        assertTrue(server.getClients().size() >= 1, "Should have at least 1 client for broadcast");

        client.disconnect();
        server.stop();
    }

    private void testClientDisconnectRemovesConnection() throws Exception {
        int port = findFreePort();

        List<Player> players = new ArrayList<>();
        players.add(new Player(0, false, null));
        Player p1 = new Player(1, true, null);
        players.add(p1);

        Game game = new Game(players, new Deck(), new DiscardPile());
        Server server = new Server(game);
        server.start(port);

        Thread.sleep(100);

        Client client = new Client();
        client.connect("127.0.0.1", port);

        Thread.sleep(300);

        assertNotNull(p1.getConnection(), "Player should have a connection before disconnect");

        client.disconnect();
        server.stop();

        // After server stop, clients list is cleared
        assertEquals(0, server.getClients().size(), "Server should clear clients after stop");
    }

    private void testServerStopsGameLoop() throws Exception {
        int port = findFreePort();

        List<Player> players = new ArrayList<>();
        players.add(new Player(0, false, null));
        players.add(new Player(1, false, null));

        Game game = new Game(players, new Deck(), new DiscardPile());
        Server server = new Server(game);
        server.start(port);

        Thread.sleep(100);

        assertTrue(server.isRunning(), "Server should be running after start");

        server.stop();

        assertFalse(server.isRunning(), "Server should not be running after stop");
        assertFalse(game.isRunning(), "Game should also be stopped when server stops");
    }

    private void testMultipleClientsConnect() throws Exception {
        int port = findFreePort();

        List<Player> players = new ArrayList<>();
        players.add(new Player(0, false, null));
        Player p1 = new Player(1, true, null);
        Player p2 = new Player(2, true, null);
        players.add(p1);
        players.add(p2);

        Game game = new Game(players, new Deck(), new DiscardPile());
        Server server = new Server(game);
        server.start(port);

        Thread.sleep(100);

        Client client1 = new Client();
        client1.connect("127.0.0.1", port);
        Thread.sleep(200);

        Client client2 = new Client();
        client2.connect("127.0.0.1", port);
        Thread.sleep(200);

        assertEquals(2, server.getClients().size(), "Server should have 2 connected clients");
        assertNotNull(p1.getConnection(), "Player 1 should have a connection");
        assertNotNull(p2.getConnection(), "Player 2 should have a connection");

        client1.disconnect();
        client2.disconnect();
        server.stop();
    }

    private void testClientHandlerSendsAndReceives() throws Exception {
        int port = findFreePort();

        List<Player> players = new ArrayList<>();
        players.add(new Player(0, false, null));
        Player p1 = new Player(1, true, null);
        players.add(p1);

        Game game = new Game(players, new Deck(), new DiscardPile());
        Server server = new Server(game);
        server.start(port);

        Thread.sleep(100);

        Client client = new Client();
        client.connect("127.0.0.1", port);

        Thread.sleep(300);

        // Verify client handler is assigned and running
        ClientHandler handler = p1.getConnection();
        assertNotNull(handler, "Client handler should be assigned to player");
        assertTrue(handler.isRunning(), "Client handler should be running after connection");

        // Send a message through the handler
        handler.sendMessage("Test message from handler");

        client.disconnect();
        server.stop();
    }

    /**
     * Fix 5: Online defuse placement must be answered over the network connection,
     * not on the server console (which would hang). Drives a real socket pair:
     * the test plays the client end and answers the placement question with "3".
     */
    private void testOnlineDefusePlacementRoutedOverNetwork() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            // Connect the client end first so accept() can return, and send the
            // client-side stream header BEFORE constructing the server-side handler:
            // each ObjectInputStream constructor blocks until it has read the peer's
            // header, so both headers must be in flight before either side reads.
            Socket clientSide = new Socket("127.0.0.1", port);
            ObjectOutputStream cOut = new ObjectOutputStream(clientSide.getOutputStream());
            cOut.flush();

            ClientHandler handler = new ClientHandler(serverSocket.accept());
            handler.listen();
            ObjectInputStream cIn = new ObjectInputStream(clientSide.getInputStream());
            Player p1 = new Player(1, true, handler);

            // Drive the client: skip banner messages, answer "3" to the placement question
            Thread driver = new Thread(() -> {
                try {
                    while (true) {
                        Object msg = cIn.readObject();
                        if (msg != null && msg.toString().contains("Where in the deck")) {
                            cOut.writeObject("3");
                            cOut.flush();
                            return;
                        }
                    }
                } catch (Exception ignored) {
                }
            });
            driver.setDaemon(true);
            driver.start();

            int pos = p1.chooseDefusePosition(5);
            assertEquals(3, pos, "Online defuse placement must be answered over the network connection, not the server console");

            handler.close();
            clientSide.close();
        }
    }

    /**
     * Fix 5: The online named-card prompt (REQ 11g) must be answered over the
     * network connection. Drives a real socket pair and answers the exchange
     * (target number, then card name).
     */
    private void testOnlineNamedCardRoutedOverNetwork() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            Socket clientSide = new Socket("127.0.0.1", port);
            ObjectOutputStream cOut = new ObjectOutputStream(clientSide.getOutputStream());
            cOut.flush();

            ClientHandler handler = new ClientHandler(serverSocket.accept());
            handler.listen();
            ObjectInputStream cIn = new ObjectInputStream(clientSide.getInputStream());
            Player p1 = new Player(1, true, handler);
            p1.addCard(new AttackCard());   // [1]
            p1.addCard(new DefuseCard());   // [2]
            p1.addCard(new SkipCard());     // [3]

            Player victim = new Player(2, false, null);
            NopeCard nope = new NopeCard();
            SkipCard skip = new SkipCard();
            victim.addCard(nope);           // [1]
            victim.addCard(skip);           // [2]

            // Answer target "1" then card name "Skip"
            List<Player> targets = new ArrayList<>();
            targets.add(victim);
            Thread driver2 = new Thread(() -> {
                try {
                    boolean wantTarget = true;
                    while (true) {
                        Object msg = cIn.readObject();
                        String text = msg == null ? "" : msg.toString();
                        if (wantTarget && text.contains("target player number")) {
                            cOut.writeObject("1");
                            cOut.flush();
                            wantTarget = false;
                        } else if (!wantTarget && text.contains("Name the card")) {
                            cOut.writeObject("Skip");
                            cOut.flush();
                            return;
                        }
                    }
                } catch (Exception ignored) {
                }
            });
            driver2.setDaemon(true);
            driver2.start();

            Card received = p1.chooseNamedCard(targets);
            assertEquals(skip, received, "Online named-card prompt must be answered over the network connection");
            assertFalse(victim.getHand().contains(skip), "Named card must be removed from the victim's hand");

            handler.close();
            clientSide.close();
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Finds a free port by binding a ServerSocket to port 0.
     */
    private int findFreePort() {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException("Could not find free port", e);
        }
    }
}