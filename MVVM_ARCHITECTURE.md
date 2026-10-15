# Arquitectura MVVM y Capas del Sistema - GODSMINO

## 1. Patrón MVVM (Model-View-ViewModel)
GODSMINO implementa estrictamente el patrón **MVVM** para garantizar la separación de preocupaciones, mantenibilidad, testabilidad y escalabilidad del código.

```
┌────────────────────────────────────────────────────────┐
│                        VIEW                            │
│   (Jetpack Compose UI: Composables, Screens, Dialogs)  │
└──────────────────────────▲─────────────────────────────┘
                           │ Observa (StateFlow / Compose State)
                           ▼
┌────────────────────────────────────────────────────────┐
│                      VIEWMODEL                         │
│   (GameViewModel: Lógica de Negocio, Estado, Corrutinas)│
└──────────────────────────▲─────────────────────────────┘
                           │ Lee / Escribe / Escucha en Tiempo Real
                           ▼
┌────────────────────────────────────────────────────────┐
│                   REPOSITORY LAYER                     │
│    (GameRepository / AuthRepository / FirebaseDataSource)│
└────────────────────────────────────────────────────────┘
```

---

## 2. Descripción de Capas

### 2.1. Capa de Modelo (Domain & Data Models)
Define las entidades de datos puras que representan el estado del juego:
- **`Tile`**: Representa una ficha de dominó (`left: Int`, `right: Int`, `id: String`).
- **`Player`**: Representa a un participante (`id: String`, `name: String`, `score: Int`, `tiles: List<Tile>`, `powers: Map<PowerType, Int>`).
- **`GameSession`**: Estado global de la partida (`roomId: String`, `board: List<Tile>`, `currentTurnPlayerId: String`, `boneyardCount: Int`, `status: GameStatus`, `activeEvent: RandomEvent?`).
- **`Power` / `PowerType`**: Enumeración y estructura de poderes (`WILDCARD`, `SKIP`, `SWAP`, `REVERSE`).
- **`RandomEvent`**: Evento aleatorio activo (`id: String`, `title: String`, `description: String`, `effectType: EventEffectType`).

### 2.2. Capa de Repositorio (`GameRepository`)
Actúa como única fuente de verdad, abstrae el origen de los datos (Firebase Firestore / Realtime Database vs. Mock local para pruebas):
- `observeGameSession(roomId: String): Flow<GameSession>`
- `suspend fun makeMove(roomId: String, move: Move)`
- `suspend fun drawTile(roomId: String, playerId: String)`
- `suspend fun usePower(roomId: String, playerId: String, powerType: PowerType)`
- `suspend fun triggerRandomEvent(roomId: String)`

### 2.3. Capa de ViewModel (`GameViewModel`)
Mantiene el estado de la interfaz de usuario mediante `StateFlow` y procesa las intenciones del usuario:
- **Estado Expuesto (`uiState: StateFlow<GameUiState>`)**:
  - `isLoading: Boolean`
  - `gameSession: GameSession?`
  - `selectedTile: Tile?`
  - `selectedPower: PowerType?`
  - `errorMessage: String?`
- **Funciones de Negocio**:
  - `onTileClicked(tile: Tile)`
  - `onBoardPositionClicked(position: BoardEnd)`
  - `onPowerSelected(powerType: PowerType)`
  - `drawTileFromBoneyard()`
  - `quitGame()`
  - `restartGame()`

### 2.4. Capa de Vista (`View` - Jetpack Compose)
Interfaces de usuario puramente reactivas que observan el `GameViewModel`:
- **`LoginScreen`**: Pantalla de autenticación (Google / Email).
- **`LobbyScreen`**: Creación o unión a salas de juego.
- **`GameScreen`**: Pantalla principal del juego que integra todos los componentes reutilizables.
