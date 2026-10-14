@startuml
skinparam classAttributeIconSize 0

package "explodingkittens" {
class Main {
+ main(args: String[])
}
}

package "explodingkittens.ui" {
class ConsoleUI {
+ displayWelcome()
+ displayGameState(context: GameContext, viewer: Player)
+ displayHand(player: Player)
+ displayOptions(player: Player, targets: List<Player>)
+ promptAction(player: Player, context: GameContext): Action
+ promptCard(player: Player): Card
+ promptTarget(potentialTargets: List<Player>, actor: Player): Player
+ promptDefusePlacement(deckSize: int): int
+ displayMessage(message: String)
+ displayAction(actor: String, action: String)
+ displayCardDrawn(player: Player, card: Card)
+ displayExplosion(player: Player)
+ displayWinner(winner: Player)
+ displayDivider()
}
}

package "explodingkittens.online" {
class Server {
- game: Game
- clients: List<ClientHandler>
+ start(port: int)
+ broadcast(message: String)
}

    class Client {
        - socket: Socket
        - out: ObjectOutputStream
        - in: ObjectInputStream
        + connect(ip: String, port: int)
        + sendCommand(cmd: String)
    }

    class Player {
        - id: int
        - isOnline: boolean
        - connection: ClientHandler
        + sendMessage(msg: String)
        + receiveAction(): String
    }
    
    class ClientHandler {
        - socket: Socket
        + listen()
    }
    
    Server *-- ClientHandler
    Player "1" *-- "1" ClientHandler : Network IO
}

package "explodingkittens.game" {
class Game {
- players: List<Player>
- deck: Deck
- discardPile: DiscardPile
- activePlayerIndex: int
+ start()
+ processTurn(player: Player, action: Action)
+ notifyInterruptWindow()
}

    class InitGame {
        + setupPlayers(numPlayers: int, numBots: int): List<Player>
        + buildDeck(expansions: List<ExpansionType>): Deck
    }

    class DiscardPile {
        - cards: Stack<Card>
        + add(card: Card)
        + getTopCard(): Card
        + getNopeCount(): int
    }
    
    class Deck {
        - cards: List<Card>
        + draw(): Card
        + insertAt(card: Card, index: int)
        + shuffle()
    }

    interface GameContext {
        + getDeck(): Deck
        + getDiscardPile(): DiscardPile
        + getPlayers(): List<Player>
        + endTurn()
    }
    
    Game ..|> GameContext
    Game *-- DiscardPile
    Game *-- Deck
    InitGame ..> Game : Initializes
}

package "explodingkittens.cards" {
abstract class Card {
- name: String
+ {abstract} executeEffect(context: GameContext, actor: Player, target: Player)
}

    class DefuseCard extends Card
    class AttackCard extends Card
    class NopeCard extends Card
    class BarkingKittenCard extends Card
    class StreakingKittenCard extends Card
    class ImplodingKittenCard extends Card
}

Main --> Server
Main --> Client
Main --> InitGame
Main --> ConsoleUI
Game --> ConsoleUI
Game "1" *-- "2..5" Player : manages
Player "1" o-- "0..*" Card : hand
DiscardPile o-- Card
Deck o-- Card

@enduml