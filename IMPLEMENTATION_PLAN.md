# Plan de Implementación - GODSMINO

Este documento detalla las fases de desarrollo necesarias para implementar completamente el juego de dominó **GODSMINO** cumpliendo con todos los requisitos funcionales, técnicos y métricas de evaluación.

---

## Fases de Desarrollo

### Fase 1: Modelos de Datos y Estructura Base (1.0 Punto - Base & Login)
- [ ] Definir clases de datos: `Tile`, `Player`, `GameSession`, `Power`, `RandomEvent`.
- [ ] Configurar Firebase Auth (Google Sign-In / Credential Manager o Email/Password).
- [ ] Configurar Firebase Firestore / Realtime Database para persistencia de salas y sesiones.

### Fase 2: Lógica de Negocio y Arquitectura MVVM (0.5 Puntos - Eventos y Flujo)
- [ ] Implementar `GameRepository` para conexión con Firebase.
- [ ] Implementar `GameViewModel` con `StateFlow` para gestión de estado reactivo.
- [ ] Implementar reglas de validación de movimientos de dominó (coincidencia de extremos).
- [ ] Implementar sistema de pozo (robar ficha cuando no hay movimientos válidos).
- [ ] Implementar condiciones de fin de partida (sin fichas, pozo vacío sin movimientos, max rondas).

### Fase 3: Poderes Especiales y Eventos Aleatorios (0.5 Puntos - Dinámica)
- [ ] Implementar lógica de poderes especiales (Comodín, Salto, Intercambio, Inversión) con control de usos limitados.
- [ ] Implementar generador de eventos aleatorios y aplicación de efectos temporales.
- [ ] Implementar lógica de desempate (menor cantidad de poderes usados).

### Fase 4: Interfaz de Usuario con Jetpack Compose y Componentes (1.0 Punto - UI & Componentes)
- [ ] Crear componente `DominoTileView` (renderizado de fichas).
- [ ] Crear componente `GameBoardView` (tablero interactivo).
- [ ] Crear componente `ScoreboardView` (marcador permanente de turno y puntaje).
- [ ] Crear componente `PowerControlsView` (panel de poderes).
- [ ] Crear pantallas de Login, Lobby y GameScreen.

### Fase 5: Pruebas Unitarias y Documentación (1.0 Punto - Tests, Informe y Código Limpio)
- [ ] Escribir pruebas unitarias (`GameViewModelTest`) para validar reglas de juego, turnos y cálculo de puntuación.
- [ ] Documentar el código con KDoc y preparar los informes de arquitectura y diseño.
