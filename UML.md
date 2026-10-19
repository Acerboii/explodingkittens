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
        + promptNamedCard(actor: Player, availableTargets: List<Player>): Card
        + displayMessage(message: String)
        + displayAction(actor: String, action: String)
        + displayCardDrawn(player: Player, card: Card)
        + displayExplosion(player: Player)
        + displayWinner(winner: Player)
        + displayDivider()
    }

    class GameMenu {
        + chooseExpansions(initGame: InitGame): List<ExpansionType> {REQ 15}
        + displayName(expansion: ExpansionType): String {REQ 15}
    }
}

package "explodingkittens.online" {
    class Server {
        - game: Game
        - clients: List<ClientHandler>
        + start(port: int)
        + startGameLoop()
        + broadcast(message: String)
        + stop()
    }

    class Client {
        - socket: Socket
        - out: ObjectOutputStream
        - in: ObjectInputStream
        + connect(ip: String, port: int)
        + sendCommand(cmd: String)
        + disconnect()
    }

    class Player {
        - id: int
        - isOnline: boolean
        - connection: ClientHandler
        - hand: List<Card>
        - isBot: boolean
        - exploded: boolean
        + sendMessage(msg: String)
        + receiveAction(): String
        + decideBotAction(): String {REQ 16: random available card, else Pass}
        + addCard(card: Card)
        + removeCard(card: Card): boolean
        + hasCard(cardName: String): boolean
        + getCardByName(cardName: String): Card
        + chooseCardToGive(): Card {Fix 3: REQ 11b}
        + chooseDefusePosition(deckSize: int): int {Fix 5: online}
        + chooseNamedCard(availableTargets: List<Player>): Card {Fix 5: online}
    }

    class ClientHandler {
        - socket: Socket
        - out: ObjectOutputStream
        - in: ObjectInputStream
        - messageQueue: BlockingQueue<String>
        + listen()
        + sendMessage(msg: String)
        + readMessage(): String
        + close()
    }

    Server *-- ClientHandler
    Player "1" o-- "1" ClientHandler : Network IO
}

package "explodingkittens.game" {
    class Game {
        - players: List<Player>
        - deck: Deck
        - discardPile: DiscardPile
        - activePlayerIndex: int
        - turnsToTake: int
        - isRunning: boolean
        - cardsPlayedThisTurn: int
        - currentTurnPlayer: Player
        - turnShouldEnd: boolean
        - random: Random {Fix 1: REQ 8}
        - interruptWindowOpenTime: long {Fix 2: REQ 12}
        - clock: Clock {Fix 2: REQ 12}
        - nopeCount: int {Fix 2: REQ 12a}
        + start()
        + processTurn(player: Player, action: Action)
        + getRandom(): Random {Fix 1: REQ 8}
        + setRandom(random: Random) {Fix 1: REQ 8}
        + notifyInterruptWindow() {Fix 2: REQ 12}
        + isInterruptWindowOpen(): boolean {Fix 2: REQ 12}
        + getInterruptWindowRemainingMs(): long {Fix 2: REQ 12}
        + getClock(): Clock {Fix 2: REQ 12}
        + setClock(clock: Clock) {Fix 2: REQ 12}
        + hasPendingEffect(): boolean {Fix 2: REQ 12/12a}
        + resolvePendingEffect() {Fix 2: REQ 12/12a}
        + endTurn()
        + broadcast(message: String)
        + getActivePlayer(): Player
        + getRemainingPlayersCount(): int
        + getWinner(): Player
    }

    class Action {
        - type: String
        - card: Card
        - target: Player
        + pass(): Action
        + play(card: Card): Action
        + play(card: Card, target: Player): Action
        + getType(): String
        + getCard(): Card
        + getTarget(): Player
    }

    class InitGame {
        + setupPlayers(numPlayers: int, numBots: int): List<Player>
        + buildDeck(playerCount: int, expansions: List<ExpansionType>): Deck
        + initializeGame(numPlayers: int, numBots: int, expansions: List<ExpansionType>): Game
        + isImplemented(expansion: ExpansionType): boolean {REQ 15}
    }

    class DiscardPile {
        - cards: Stack<Card>
        + add(card: Card)
        + getTopCard(): Card
        + getNopeCount(): int
        + size(): int
        + isEmpty(): boolean
    }

    class Deck {
        - cards: List<Card>
        + draw(): Card
        + insertAt(card: Card, index: int)
        + shuffle()
        + add(card: Card)
        + size(): int
        + isEmpty(): boolean
    }

    enum ExpansionType {
        BASE
        IMPLODING_KITTENS
        STREAKING_KITTENS
        BARKING_KITTENS
    }

    interface GameContext {
        + getDeck(): Deck
        + getDiscardPile(): DiscardPile
        + getPlayers(): List<Player>
        + endTurn()
    }

    interface Clock {
        + currentTimeMillis(): long
    }

    Game ..|> GameContext
    Game --> Clock : timing (Fix 2)
    Game *-- DiscardPile
    Game *-- Deck
    Game ..> Action : uses
    Game --> ConsoleUI
    InitGame ..> Game : Initializes
}

package "explodingkittens.cards" {
    abstract class Card {
        - name: String
        + {abstract} executeEffect(context: GameContext, actor: Player, target: Player)
        + getName(): String
    }

    class ExplodingKittenCard extends Card
    class DefuseCard extends Card
    class AttackCard extends Card
    class FavorCard extends Card
    class NopeCard extends Card
    class ShuffleCard extends Card
    class SkipCard extends Card
    class SeeTheFutureCard extends Card
    class HairyPotatoCatCard extends Card
    class CattermelonCard extends Card
    class RainbowRalphingCatCard extends Card
    class TacoCatCard extends Card
    class OverweightBikiniCatCard extends Card
    class CardFactory {
        + register(name: String, factory: Supplier<Card>)
        + create(name: String): Card
        + isRegistered(name: String): boolean
    }
}

Main --> Server
Main --> Client
Main --> InitGame
Main --> GameMenu : shows (REQ 15)
GameMenu ..> InitGame : queries implemented expansions (REQ 15)
Game "1" *-- "2..5" Player : manages
Player "1" o-- "0..*" Card : hand
DiscardPile o-- Card
Deck o-- Card
Action o-- Card
Action o-- Player : target
CardFactory ..> Card : creates (REQ 14/18)
InitGame --> CardFactory : uses

@enduml

## Fixes reflected in this diagram

| Fix | Change | Diagram elements |
|-----|--------|------------------|
| Fix 1 (REQ 8) | Random first player, injectable RNG for testability | `Game.random`, `Game.getRandom()`, `Game.setRandom()` |
| Fix 2 (REQ 12/12a) | Nope blocking + 5-second interrupt window + indefinite Nope/Yup chain | `Game.interruptWindowOpenTime`, `Game.clock`, `Game.nopeCount`, `isInterruptWindowOpen()`, `getInterruptWindowRemainingMs()`, `notifyInterruptWindow()`, `getClock()/setClock()`, `hasPendingEffect()/resolvePendingEffect()` (parity-based resolution), private chain closures (`pendingApply/pendingUndo/pendingReapply/pendingSettle`, `undoSkip/reapplySkip/settleBlockedSkip/passEndsTurn`), `Clock` interface |
| Fix 3 (REQ 11b) | Favor card | `Player.chooseCardToGive()`, `FavorCard` |
| Fix 4 | Cat-card combo double-trigger prevention | Behavior-only change inside private `Game.executeCardEffect()`; no public API added, nothing to draw |
| Fix 5 | Online prompt hangs (defuse / named card) | `Player.chooseDefusePosition()`, `Player.chooseNamedCard()` — network-routed via `ClientHandler.readMessage()`; `Game` dispatches on `isOnline` before UI/bot fallbacks (private helpers, intentionally not shown). Partial expansion logic was removed in favor of the `CardFactory` extension point (REQ 14/18) |
| REQ 15 | Game menu for selecting which expansions or other game variants to play with | `GameMenu` class (`chooseExpansions()` returns selection incl. BASE, rejects unimplemented options and re-prompts), `InitGame.isImplemented()` (BASE always true; non-BASE needs registered DeckEntries), `Main` wiring (no-args demo shows menu first; `menu` keyword starts server after menu) |