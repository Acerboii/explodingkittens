package explodingkittens;

import explodingkittens.cards.*;
import explodingkittens.game.*;
import explodingkittens.online.*;

import java.lang.reflect.*;
import java.net.Socket;
import java.util.List;
import java.util.Stack;

import static explodingkittens.Assert.*;

/**
 * Validates that the implementation reflects the UML diagram exactly,
 * checking all packages, classes, interfaces, fields, and methods.
 */
public class UMLReflectionTest {

    private void testMainClass() throws NoSuchMethodException {
        Method mainMethod = Main.class.getMethod("main", String[].class);
        assertTrue(Modifier.isPublic(mainMethod.getModifiers()));
        assertTrue(Modifier.isStatic(mainMethod.getModifiers()));
    }

    private void testServerClass() throws NoSuchFieldException, NoSuchMethodException {
        Field gameField = Server.class.getDeclaredField("game");
        assertTrue(Modifier.isPrivate(gameField.getModifiers()));
        assertEquals(Game.class, gameField.getType());

        Field clientsField = Server.class.getDeclaredField("clients");
        assertTrue(Modifier.isPrivate(clientsField.getModifiers()));
        assertEquals(List.class, clientsField.getType());

        Method startMethod = Server.class.getMethod("start", int.class);
        assertTrue(Modifier.isPublic(startMethod.getModifiers()));

        Method broadcastMethod = Server.class.getMethod("broadcast", String.class);
        assertTrue(Modifier.isPublic(broadcastMethod.getModifiers()));
    }

    private void testClientClass() throws NoSuchFieldException, NoSuchMethodException {
        Field socketField = Client.class.getDeclaredField("socket");
        assertTrue(Modifier.isPrivate(socketField.getModifiers()));
        assertEquals(Socket.class, socketField.getType());

        Field outField = Client.class.getDeclaredField("out");
        assertTrue(Modifier.isPrivate(outField.getModifiers()));
        assertEquals(java.io.ObjectOutputStream.class, outField.getType());

        Field inField = Client.class.getDeclaredField("in");
        assertTrue(Modifier.isPrivate(inField.getModifiers()));
        assertEquals(java.io.ObjectInputStream.class, inField.getType());

        Method connectMethod = Client.class.getMethod("connect", String.class, int.class);
        assertTrue(Modifier.isPublic(connectMethod.getModifiers()));

        Method sendCommandMethod = Client.class.getMethod("sendCommand", String.class);
        assertTrue(Modifier.isPublic(sendCommandMethod.getModifiers()));
    }

    private void testPlayerClass() throws NoSuchFieldException, NoSuchMethodException {
        Field idField = Player.class.getDeclaredField("id");
        assertTrue(Modifier.isPrivate(idField.getModifiers()));
        assertEquals(int.class, idField.getType());

        Field isOnlineField = Player.class.getDeclaredField("isOnline");
        assertTrue(Modifier.isPrivate(isOnlineField.getModifiers()));
        assertEquals(boolean.class, isOnlineField.getType());

        Field connectionField = Player.class.getDeclaredField("connection");
        assertTrue(Modifier.isPrivate(connectionField.getModifiers()));
        assertEquals(ClientHandler.class, connectionField.getType());

        Method sendMessageMethod = Player.class.getMethod("sendMessage", String.class);
        assertTrue(Modifier.isPublic(sendMessageMethod.getModifiers()));

        Method receiveActionMethod = Player.class.getMethod("receiveAction");
        assertTrue(Modifier.isPublic(receiveActionMethod.getModifiers()));
        assertEquals(String.class, receiveActionMethod.getReturnType());
    }

    private void testClientHandlerClass() throws NoSuchFieldException, NoSuchMethodException {
        Field socketField = ClientHandler.class.getDeclaredField("socket");
        assertTrue(Modifier.isPrivate(socketField.getModifiers()));
        assertEquals(Socket.class, socketField.getType());

        Method listenMethod = ClientHandler.class.getMethod("listen");
        assertTrue(Modifier.isPublic(listenMethod.getModifiers()));
    }

    private void testGameClassAndGameContext() throws NoSuchFieldException, NoSuchMethodException {
        assertTrue(GameContext.class.isAssignableFrom(Game.class), "Game must implement GameContext");

        Field playersField = Game.class.getDeclaredField("players");
        assertTrue(Modifier.isPrivate(playersField.getModifiers()));
        assertEquals(List.class, playersField.getType());

        Field deckField = Game.class.getDeclaredField("deck");
        assertTrue(Modifier.isPrivate(deckField.getModifiers()));
        assertEquals(Deck.class, deckField.getType());

        Field discardPileField = Game.class.getDeclaredField("discardPile");
        assertTrue(Modifier.isPrivate(discardPileField.getModifiers()));
        assertEquals(DiscardPile.class, discardPileField.getType());

        Field activePlayerIndexField = Game.class.getDeclaredField("activePlayerIndex");
        assertTrue(Modifier.isPrivate(activePlayerIndexField.getModifiers()));
        assertEquals(int.class, activePlayerIndexField.getType());

        Method startMethod = Game.class.getMethod("start");
        assertTrue(Modifier.isPublic(startMethod.getModifiers()));

        Method processTurnMethod = Game.class.getMethod("processTurn", Player.class, Action.class);
        assertTrue(Modifier.isPublic(processTurnMethod.getModifiers()));

        Method notifyInterruptWindowMethod = Game.class.getMethod("notifyInterruptWindow");
        assertTrue(Modifier.isPublic(notifyInterruptWindowMethod.getModifiers()));

        // GameContext methods
        Method getDeckMethod = Game.class.getMethod("getDeck");
        assertTrue(Modifier.isPublic(getDeckMethod.getModifiers()));
        assertEquals(Deck.class, getDeckMethod.getReturnType());

        Method getDiscardPileMethod = Game.class.getMethod("getDiscardPile");
        assertTrue(Modifier.isPublic(getDiscardPileMethod.getModifiers()));
        assertEquals(DiscardPile.class, getDiscardPileMethod.getReturnType());

        Method getPlayersMethod = Game.class.getMethod("getPlayers");
        assertTrue(Modifier.isPublic(getPlayersMethod.getModifiers()));
        assertEquals(List.class, getPlayersMethod.getReturnType());

        Method endTurnMethod = Game.class.getMethod("endTurn");
        assertTrue(Modifier.isPublic(endTurnMethod.getModifiers()));
    }

    private void testInitGameClass() throws NoSuchMethodException {
        Method setupPlayersMethod = InitGame.class.getMethod("setupPlayers", int.class, int.class);
        assertTrue(Modifier.isPublic(setupPlayersMethod.getModifiers()));
        assertEquals(List.class, setupPlayersMethod.getReturnType());

        Method buildDeckMethod = InitGame.class.getMethod("buildDeck", int.class, List.class);
        assertTrue(Modifier.isPublic(buildDeckMethod.getModifiers()));
        assertEquals(Deck.class, buildDeckMethod.getReturnType());
    }

    private void testDiscardPileClass() throws NoSuchFieldException, NoSuchMethodException {
        Field cardsField = DiscardPile.class.getDeclaredField("cards");
        assertTrue(Modifier.isPrivate(cardsField.getModifiers()));
        assertEquals(Stack.class, cardsField.getType());

        Method addMethod = DiscardPile.class.getMethod("add", Card.class);
        assertTrue(Modifier.isPublic(addMethod.getModifiers()));

        Method getTopCardMethod = DiscardPile.class.getMethod("getTopCard");
        assertTrue(Modifier.isPublic(getTopCardMethod.getModifiers()));
        assertEquals(Card.class, getTopCardMethod.getReturnType());

        Method getNopeCountMethod = DiscardPile.class.getMethod("getNopeCount");
        assertTrue(Modifier.isPublic(getNopeCountMethod.getModifiers()));
        assertEquals(int.class, getNopeCountMethod.getReturnType());
    }

    private void testDeckClass() throws NoSuchFieldException, NoSuchMethodException {
        Field cardsField = Deck.class.getDeclaredField("cards");
        assertTrue(Modifier.isPrivate(cardsField.getModifiers()));
        assertEquals(List.class, cardsField.getType());

        Method drawMethod = Deck.class.getMethod("draw");
        assertTrue(Modifier.isPublic(drawMethod.getModifiers()));
        assertEquals(Card.class, drawMethod.getReturnType());

        Method insertAtMethod = Deck.class.getMethod("insertAt", Card.class, int.class);
        assertTrue(Modifier.isPublic(insertAtMethod.getModifiers()));

        Method shuffleMethod = Deck.class.getMethod("shuffle");
        assertTrue(Modifier.isPublic(shuffleMethod.getModifiers()));
    }

    private void testCardHierarchy() throws NoSuchFieldException, NoSuchMethodException {
        assertTrue(Modifier.isAbstract(Card.class.getModifiers()));

        Field nameField = Card.class.getDeclaredField("name");
        assertTrue(Modifier.isPrivate(nameField.getModifiers()));
        assertEquals(String.class, nameField.getType());

        Method executeEffectMethod = Card.class.getMethod("executeEffect", GameContext.class, Player.class, Player.class);
        assertTrue(Modifier.isPublic(executeEffectMethod.getModifiers()));
        assertTrue(Modifier.isAbstract(executeEffectMethod.getModifiers()));

        assertTrue(Card.class.isAssignableFrom(DefuseCard.class));
        assertTrue(Card.class.isAssignableFrom(AttackCard.class));
        assertTrue(Card.class.isAssignableFrom(NopeCard.class));

        // REQ 14/18: CardFactory is the extension point for creating Cards by name
        CardFactory factory = new CardFactory();
        assertTrue(factory.create("Defuse") instanceof DefuseCard);
        assertTrue(factory.create("Attack") instanceof AttackCard);
        assertTrue(factory.create("Nope") instanceof NopeCard);
    }

    private void testConsoleUIClass() throws NoSuchMethodException {
        Class<?> uiClass = explodingkittens.ui.ConsoleUI.class;
        assertTrue(Modifier.isPublic(uiClass.getModifiers()));

        Method displayWelcome = uiClass.getMethod("displayWelcome");
        assertTrue(Modifier.isPublic(displayWelcome.getModifiers()));

        Method displayGameState = uiClass.getMethod("displayGameState", GameContext.class, Player.class);
        assertTrue(Modifier.isPublic(displayGameState.getModifiers()));

        Method displayHand = uiClass.getMethod("displayHand", Player.class);
        assertTrue(Modifier.isPublic(displayHand.getModifiers()));

        Method displayOptions = uiClass.getMethod("displayOptions", Player.class, List.class);
        assertTrue(Modifier.isPublic(displayOptions.getModifiers()));

        Method promptAction = uiClass.getMethod("promptAction", Player.class, GameContext.class);
        assertTrue(Modifier.isPublic(promptAction.getModifiers()));

        Method promptCard = uiClass.getMethod("promptCard", Player.class);
        assertTrue(Modifier.isPublic(promptCard.getModifiers()));

        Method promptTarget = uiClass.getMethod("promptTarget", List.class, Player.class);
        assertTrue(Modifier.isPublic(promptTarget.getModifiers()));

        Method promptDefusePlacement = uiClass.getMethod("promptDefusePlacement", int.class);
        assertTrue(Modifier.isPublic(promptDefusePlacement.getModifiers()));

        Method displayWinner = uiClass.getMethod("displayWinner", Player.class);
        assertTrue(Modifier.isPublic(displayWinner.getModifiers()));
    }
}