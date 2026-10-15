# Diagrama de Clases - GODSMINO

A continuación se presenta el diagrama de clases estructural del sistema GODSMINO utilizando notación **Mermaid**, cumpliendo con los requerimientos de arquitectura MVVM y componentes.

```mermaid
classDiagram
    direction TB

    class Tile {
        +Int left
        +Int right
        +String id
        +Boolean isDouble()
        +Int sumPoints()
    }

    class Player {
        +String id
        +String name
        +Int score
        +List~Tile~ tiles
        +Map~PowerType, Int~ powerCounts
        +Boolean hasValidMove(List~Tile~ boardEnds)
    }

    class GameSession {
        +String roomId
        +List~Tile~ board
        +List~Player~ players
        +String currentTurnPlayerId
        +Int boneyardSize
        +GameStatus status
        +RandomEvent activeEvent
        +Int roundNumber
    }

    class PowerType {
        <<enumeration>>
        WILDCARD
        SKIP
        SWAP
        REVERSE
    }

    class GameStatus {
        <<enumeration>>
        WAITING
        PLAYING
        FINISHED
    }

    class RandomEvent {
        +String id
        +String title
        +String description
        +EventType type
    }

    class GameRepository {
        -FirebaseFirestore firestore
        +Flow~GameSession~ observeGame(String roomId)
        +suspend updateGameSession(GameSession session)
        +suspend drawTile(String roomId, String playerId)
        +suspend makeMove(String roomId, String playerId, Tile tile, String end)
        +suspend applyPower(String roomId, String playerId, PowerType power)
    }

    class GameViewModel {
        -GameRepository repository
        +StateFlow~GameUiState~ uiState
        +loadGame(String roomId)
        +selectTile(Tile tile)
        +playSelectedTile(String boardEnd)
        +drawTile()
        +usePower(PowerType power)
        +quitMatch()
    }

    class GameUiState {
        +GameSession session
        +Tile selectedTile
        +PowerType selectedPower
        +Boolean isLoading
        +String error
    }

    class GameScreen {
        +GameViewModel viewModel
        +Composable()
    }

    class DominoTileView {
        +Tile tile
        +Boolean isSelected
        +Modifier modifier
        +Composable()
    }

    class GameBoardView {
        +List~Tile~ board
        +Composable()
    }

    class ScoreboardView {
        +List~Player~ players
        +String currentTurnId
        +Composable()
    }

    class PowerControlsView {
        +Map~PowerType, Int~ powers
        +PowerType selectedPower
        +Function onPowerClick
        +Composable()
    }

    GameSession "1" *-- "*" Player : contains
    GameSession "1" *-- "*" Tile : board tiles
    Player "1" *-- "*" Tile : hand tiles
    GameViewModel --> GameRepository : uses
    GameViewModel --> GameUiState : exposes
    GameScreen --> GameViewModel : observes
    GameScreen --> DominoTileView : renders
    GameScreen --> GameBoardView : renders
    GameScreen --> ScoreboardView : renders
    GameScreen --> PowerControlsView : renders
```

---

## Explicación de Relaciones Principales
1. **Composición (`*--`)**: Una `GameSession` contiene múltiples `Player` y `Tile` (tanto en el tablero como en el pozo). Cada `Player` contiene una lista de fichas (`Tile`) en su mano.
2. **Dependencia (`-->`)**: El `GameViewModel` interactúa con el `GameRepository` para sincronizar y persistir el estado de la partida en Firebase.
3. **Observación**: La vista (`GameScreen` y sus componentes reutilizables) observa el `StateFlow<GameUiState>` expuesto por el `GameViewModel`.
