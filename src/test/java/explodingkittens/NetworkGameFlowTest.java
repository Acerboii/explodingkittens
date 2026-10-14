package explodingkittens;

import explodingkittens.cards.DefuseCard;
import explodingkittens.game.Deck;
import explodingkittens.game.DiscardPile;
import explodingkittens.game.Game;
import explodingkittens.online.Client;
import explodingkittens.online.Player;
import explodingkittens.online.Server;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import static explodingkittens.Assert.*;

/**
 * Validates networking game flow between Server and Client over sockets.
 */
public class NetworkGameFlowTest {

    public void testServerAndClientConnection() throws Exception {
        // Find a free port
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }

        List<Player> players = new ArrayList<>();
        Player p0 = new Player(0, false, null); // host on server
        p0.addCard(new DefuseCard());
        Player p1 = new Player(1, true, null);  // online client
        p1.addCard(new DefuseCard());
        players.add(p0);
        players.add(p1);

        Deck deck = new Deck();
        deck.add(new DefuseCard());
        DiscardPile discardPile = new DiscardPile();

        Game game = new Game(players, deck, discardPile);
        Server server = new Server(game);
        server.start(port);

        // Allow server to bind
        Thread.sleep(100);

        Client client = new Client();
        client.connect("127.0.0.1", port);

        // Allow connection to establish and assign to Player 1
        Thread.sleep(300);

        assertEquals(1, server.getClients().size(), "Server should have 1 connected client");
        assertNotNull(p1.getConnection(), "Player 1 should be assigned the client connection");

        client.disconnect();
        server.stop();
    }
}
