# Especificaciones de Diseño y Reglas del Juego - GODSMINO

## 1. Introducción y Visión General
**GODSMINO** es un juego de dominó multijugador en tiempo real desarrollado para Android utilizando Jetpack Compose, arquitectura MVVM y sincronización en la nube con Firebase. El juego combina las reglas tradicionales del dominó con mecánicas modernas de **poderes especiales** y **eventos aleatorios** que dinamizan las partidas.

---

## 2. Reglas del Juego

### 2.1. Configuración Inicial
- **Fichas:** El juego utiliza un juego estándar de dominó de doble seis (28 fichas en total, desde `[0,0]` hasta `[6,6]`).
- **Jugadores:** Partidas de 2 a 4 jugadores.
- **Mano Inicial:** Cada jugador recibe exactamente **7 fichas** al inicio de la partida.
- **Pozo (Boneyard):** Las fichas restantes forman el pozo del cual se roban fichas cuando no hay movimientos válidos.
- **Turnos:** La partida se desarrolla por turnos estrictamente controlados. Se lleva registro permanente del turno actual.

### 2.2. Dinámica del Turno y Validación de Movimientos
1. En su turno, el jugador selecciona una ficha de su mano.
2. El sistema valida si la ficha coincide con alguno de los extremos abiertos del tablero de dominó según las reglas clásicas (por ejemplo, el número de puntos de un extremo debe coincidir con el de la ficha jugada).
3. Si el movimiento es válido, la ficha se coloca en el tablero y se actualizan los extremos abiertos.
4. **Sin movimientos válidos:** Si el jugador no posee ninguna ficha que coincida con los extremos, **debe tomar una ficha del pozo**.
   - Si tras robar la ficha tiene un movimiento válido, puede jugarlo (o guardarlo según la variante de reglas).
   - Si no puede jugar y el pozo está vacío, **pierde el turno automáticamente**.

### 2.3. Sistema de Puntuación y Fin de Partida
- **Objetivo:** Obtener el mayor puntaje al finalizar la partida.
- **Condiciones de Finalización:**
  1. Un jugador se queda sin fichas en su mano (Dominó).
  2. Ningún jugador tiene movimientos posibles y el pozo está vacío (Cierre / Tranca).
  3. Se alcanza el **número máximo de rondas** establecido.
- **Cálculo de Puntuación:**
  - Al finalizar por quedarse sin fichas, el ganador recibe puntos basados en la suma de los puntos de las fichas restantes en las manos de los oponentes.
  - Al finalizar por tranca, gana el jugador con la menor suma de puntos en sus fichas (o el que tenga mayor puntaje acumulado según la ronda).
- **Desempate:** En caso de empate en puntaje, gana **quien haya utilizado menos poderes especiales** durante la partida.

---

## 3. Poderes Especiales
Cada jugador dispone de un conjunto limitado de poderes especiales que puede activar **antes de ejecutar su movimiento**:

1. **Comodín:**
   - *Efecto:* Permite colocar una ficha en el tablero sin necesidad de cumplir con la coincidencia en uno de los extremos.
   - *Usos limitados:* Máximo 1 o 2 usos por partida.
2. **Salto (Skip):**
   - *Efecto:* Hace que el siguiente jugador en el orden pierda su turno, volviendo a tocar el turno al usuario actual (o saltando al siguiente según configuración).
   - *Usos limitados:* Máximo 1 uso por partida.
3. **Intercambio (Swap):**
   - *Efecto:* Permite intercambiar una ficha propia con una ficha aleatoria o seleccionada de la mano del oponente.
   - *Usos limitados:* Máximo 1 uso por partida.
4. **Inversión (Reverse):**
   - *Efecto:* Cambia el sentido de los turnos (de horario a antihorario o viceversa en partidas de 3+ jugadores).
   - *Usos limitados:* Máximo 1 uso por partida.

---

## 4. Eventos Aleatorios
En turnos determinados, el sistema puede disparar de forma aleatoria un **evento especial** que modifica temporalmente las reglas de la ronda:
- **Tormenta en el Pozo:** Permite a todos los jugadores robar una ficha extra inmediatamente.
- **Impuesto al Puntaje:** Reduce temporalmente los puntos de bonificación de todos los jugadores en un 20% durante esa ronda.
- **Ficha Loca:** Una ficha seleccionada al azar en la mano de todos los jugadores duplica su valor negativo si se quedan con ella al final.
- **Inversión Súbita:** Cambia automáticamente el sentido de los turnos.

---

## 5. Sincronización en Tiempo Real con Firebase
- **Firestore / Realtime Database:** Almacena el estado global de la sesión de juego (`GameSession`).
- **Datos Sincronizados:**
  - Estado del tablero (secuencia de fichas colocadas y extremos activos).
  - Estado de las manos de los jugadores (ocultas para oponentes, visibles en conteo).
  - Turno actual, dirección y temporizador.
  - Poderes disponibles restantes por jugador.
  - Puntuaciones acumuladas en tiempo real.
  - Eventos aleatorios activos.

---

## 6. Gestión de Usuarios y Controles de Partida
- **Autenticación:** Opcional mediante Google Sign-In / Credential Manager o correo y contraseña.
- **Controles de Partida:**
  - Botón para abandonar la partida (registra derrota o abandono).
  - Botón para reiniciar la partida (disponible para el host de la sala).
  - Visualización permanente de puntajes, turno activo y estado de conexión.
