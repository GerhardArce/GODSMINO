# Informe de Componentes Reutilizables - GODSMINO

## 1. Enfoque de Diseño Basado en Componentes
Para garantizar un código limpio, modular, documentado y reutilizable, la interfaz gráfica de GODSMINO en **Jetpack Compose** se ha estructurado en componentes atómicos e independientes. Cada componente encapsula su propia lógica de presentación y estilizado visual siguiendo las guías de Material 3.

---

## 2. Catálogo de Componentes Reutilizables

### 2.1. `DominoTileView` (Componente de Ficha)
- **Propósito:** Representa visualmente una ficha de dominó (dividida en dos mitades con puntos o números del 0 al 6).
- **Parámetros principales:** `tile: Tile`, `isSelected: Boolean`, `onClick: () -> Unit`, `modifier: Modifier`.
- **Características:** Soporta orientación horizontal y vertical, efectos visuales al ser seleccionada por el jugador y estilos adaptados a Material 3.

### 2.2. `GameBoardView` (Componente del Tablero)
- **Propósito:** Muestra la secuencia de fichas jugadas en el tablero en forma de serpiente o línea conectada.
- **Parámetros principales:** `board: List<Tile>`, `modifier: Modifier`.
- **Características:** Maneja desplazamiento horizontal/vertical automático cuando la cantidad de fichas colocadas crece, mostrando claramente los extremos abiertos.

### 2.3. `ScoreboardView` (Marcador Permanente)
- **Propósito:** Muestra permanentemente el turno actual, el estado de los jugadores y sus puntajes acumulados en tiempo real.
- **Parámetros principales:** `players: List<Player>`, `currentTurnPlayerId: String`, `roundNumber: Int`.
- **Características:** Destaca visualmente de quién es el turno actual con animaciones sutiles y muestra los puntos y estado de conexión de cada participante.

### 2.4. `PowerControlsView` (Controles de Poderes Especiales)
- **Propósito:** Permite al jugador visualizar sus poderes restantes (Comodín, Salto, Intercambio, Inversión) y seleccionarlos antes de realizar su movimiento.
- **Parámetros principales:** `powers: Map<PowerType, Int>`, `selectedPower: PowerType?`, `onPowerSelected: (PowerType) -> Unit`.
- **Características:** Deshabilita automáticamente los poderes que ya agotaron sus usos o cuando no es el turno del jugador.

### 2.5. `RandomEventBanner` (Notificador de Eventos Aleatorios)
- **Propósito:** Banner flotante o superior que notifica cuando un evento aleatorio está activo durante la partida.
- **Parámetros principales:** `event: RandomEvent?`.
- **Características:** Muestra el título, descripción y efectos temporales del evento aleatorio activo.

### 2.6. `GameControlsView` (Controles de Acción)
- **Propósito:** Agrupa los botones de acción del jugador (Robar del pozo, Pasar turno, Abandonar partida, Reiniciar).
- **Parámetros principales:** `canDraw: Boolean`, `onDrawClicked: () -> Unit`, `onPassClicked: () -> Unit`, `onQuitClicked: () -> Unit`.
- **Características:** Se habilita o deshabilita dinámicamente según la regla de si el jugador tiene movimientos válidos o si el pozo está vacío.

---

## 3. Beneficios de la Arquitectura de Componentes
- **Separación de Responsabilidades:** Los componentes UI no contienen reglas de negocio ni lógica de Firebase.
- **Testabilidad:** Cada componente puede probarse individualmente con Compose Previews y tests de UI.
- **Mantenibilidad:** Modificaciones visuales en las fichas o tablero se propagan automáticamente a toda la aplicación sin afectar la lógica del juego.
