Exploding Kittens - Architecture Implementation
================================================

This project implements the Exploding Kittens architecture defined in UML.md.
It uses a standard Maven-style directory layout (src/main/java and src/test/java)
and requires no external build tools or third-party dependencies (pure standard Java).

Project Structure:
------------------
- explodingkittens
  - Main.java: Application entry point supporting server/client startup
- explodingkittens.ui
  - ConsoleUI.java: Formats game state, turn options, and interactive user prompts
- explodingkittens.online
  - Server.java: Coordinates game server and client broadcasts
  - Client.java: Network client connecting via TCP sockets
  - Player.java: Manages player identity, hand, and actions
  - ClientHandler.java: Network IO handler for connected clients
- explodingkittens.game
  - Game.java: Implements GameContext, turn progression, and interrupt window
  - GameContext.java: Interface providing deck, discard pile, and turn actions
  - InitGame.java: Factory setting up players and building decks with expansions
  - DiscardPile.java: Stack of played cards with Nope tracking
  - Deck.java: Draw deck with draw, insert, and shuffle capabilities
  - Action.java: Encapsulates player turn actions
  - ExpansionType.java: Enumeration of supported expansions (Base, Imploding, Streaking, Barking)
- explodingkittens.cards
  - Card.java: Abstract base card with executeEffect
  - DefuseCard.java: Saves player from exploding
  - AttackCard.java: Ends turn without drawing and forces turns on next player
  - NopeCard.java: Stops previous card action
  - BarkingKittenCard.java: Barking Kittens expansion card
  - StreakingKittenCard.java: Streaking Kittens expansion card
  - ImplodingKittenCard.java: Imploding Kittens expansion card

Compiling and Running (Standard Java):
--------------------------------------
1. Compile sources:
   mkdir -p out
   javac -d out $(find src/main/java -name "*.java")

2. Compile and run test suite:
   javac -d out $(find src/main/java src/test/java -name "*.java")
   java -cp out explodingkittens.TestRunner

3. Running Server:
   java -cp out explodingkittens.Main <numPlayers> <numBots> [port]
   Example:
   java -cp out explodingkittens.Main 2 0 2048

4. Running Client:
   java -cp out explodingkittens.Main <serverIP> [port]
   Example:
   java -cp out explodingkittens.Main 127.0.0.1 2048
