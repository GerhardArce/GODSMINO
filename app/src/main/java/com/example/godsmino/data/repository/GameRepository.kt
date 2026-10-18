package com.example.godsmino.data.repository

import com.example.godsmino.data.datasource.FirebaseGameDataSource
import com.example.godsmino.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class GameRepository(private val roomId: String = "room_godsmino_1", private val isHost: Boolean = true) {
    private val firebaseDataSource = FirebaseGameDataSource()
    private val _session = MutableStateFlow(createInitialSession(roomId, isHost))
    val session: Flow<GameSession> = _session.asStateFlow()

    private fun createInitialSession(roomId: String, isHost: Boolean): GameSession {
        val allTiles = mutableListOf<Tile>()
        for (i in 0..6) {
            for (j in i..6) {
                allTiles.add(Tile(left = i, right = j))
            }
        }
        allTiles.shuffle()

        val p1Tiles = allTiles.subList(0, 7).toList()
        val p2Tiles = allTiles.subList(7, 14).toList()
        val remainingBoneyard = allTiles.subList(14, 28).toList()

        val p1 = Player(id = "p1", name = "Jugador 1 (Tu)", tiles = p1Tiles, score = 0)
        val p2 = Player(id = "p2", name = if (isHost) "Oponente (Bot/Online)" else "Jugador 2", tiles = p2Tiles, score = 0)

        val initialSession = GameSession(
            roomId = roomId,
            board = emptyList(),
            players = listOf(p1, p2),
            currentTurnPlayerId = p1.id,
            boneyard = remainingBoneyard,
            status = GameStatus.PLAYING,
            activeEvent = RandomEvent("ev_init", "Inicio de Partida Online", "Sala [$roomId] conectada correctamente.", "INFO"),
            roundNumber = 1
        )

        firebaseDataSource.syncGameSession(roomId, initialSession)
        return initialSession
    }

    fun makeMove(playerId: String, tile: Tile, direction: BoardDirection, useWildcard: Boolean = false): Pair<Boolean, String?> {
        val current = _session.value
        if (current.currentTurnPlayerId != playerId) {
            return Pair(false, "No es tu turno actual. Espera al oponente.")
        }
        if (current.status != GameStatus.PLAYING) {
            return Pair(false, "La partida ha finalizado o no esta activa.")
        }

        val player = current.players.find { it.id == playerId } ?: return Pair(false, "Jugador no encontrado.")
        
        val matchingTile = player.tiles.find {
            (it.left == tile.left && it.right == tile.right) || (it.left == tile.right && it.right == tile.left)
        }

        if (matchingTile == null && !useWildcard) {
            return Pair(false, "No posees esta ficha en tu mano.")
        }

        val leftEnd = current.getLeftEnd()
        val rightEnd = current.getRightEnd()

        val tileToCheck = matchingTile ?: tile
        if (!useWildcard && current.board.isNotEmpty()) {
            val canConnectLeft = leftEnd != null && (tileToCheck.left == leftEnd || tileToCheck.right == leftEnd)
            val canConnectRight = rightEnd != null && (tileToCheck.left == rightEnd || tileToCheck.right == rightEnd)

            val isValid = when (direction) {
                BoardDirection.LEFT, BoardDirection.UP -> canConnectLeft
                BoardDirection.RIGHT, BoardDirection.DOWN -> canConnectRight
            }
            if (!isValid) {
                return Pair(false, "Jugada invalida: La ficha no coincide con los extremos del tablero.")
            }
        }

        val newBoard = if (current.board.isEmpty()) {
            listOf(tileToCheck)
        } else when (direction) {
            BoardDirection.RIGHT, BoardDirection.DOWN -> {
                val lastRight = current.getRightEnd() ?: tileToCheck.left
                val alignedTile = if (tileToCheck.left == lastRight) {
                    tileToCheck
                } else if (tileToCheck.right == lastRight) {
                    tileToCheck.copy(left = tileToCheck.right, right = tileToCheck.left)
                } else {
                    tileToCheck
                }
                current.board + alignedTile
            }
            BoardDirection.LEFT, BoardDirection.UP -> {
                val firstLeft = current.getLeftEnd() ?: tileToCheck.right
                val alignedTile = if (tileToCheck.right == firstLeft) {
                    tileToCheck
                } else if (tileToCheck.left == firstLeft) {
                    tileToCheck.copy(left = tileToCheck.right, right = tile.left)
                } else {
                    tileToCheck
                }
                listOf(alignedTile) + current.board
            }
        }

        val updatedTiles = if (useWildcard) {
            val target = player.tiles.firstOrNull { (it.left == tile.left && it.right == tile.right) || (it.left == tile.right && it.right == tile.left) }
            if (target != null) player.tiles.filterNot { it.id == target.id } else player.tiles
        } else {
            if (matchingTile != null) player.tiles.filterNot { it.id == matchingTile.id } else player.tiles
        }

        val isFinished = updatedTiles.isEmpty()
        val nextTurnId = current.players.first { it.id != playerId }.id

        val updatedPlayers = current.players.map {
            if (it.id == playerId) it.copy(tiles = updatedTiles) else it
        }

        var finalPlayers = updatedPlayers
        val finalStatus = if (isFinished) GameStatus.FINISHED else current.status

        if (isFinished) {
            val opponentTilesSum = updatedPlayers.filter { it.id != playerId }.sumOf { p -> p.tiles.sumOf { it.sumPoints() } }
            finalPlayers = updatedPlayers.map { p ->
                if (p.id == playerId) p.copy(score = p.score + opponentTilesSum + 50) else p
            }
        }

        val possibleEvent = if (!isFinished && Random.nextFloat() < 0.20f) {
            generateRandomEvent()
        } else current.activeEvent

        val newSession = current.copy(
            board = newBoard,
            players = finalPlayers,
            currentTurnPlayerId = if (isFinished) playerId else nextTurnId,
            status = finalStatus,
            activeEvent = possibleEvent
        )

        _session.value = newSession
        firebaseDataSource.syncGameSession(newSession.roomId, newSession)
        return Pair(true, null)
    }

    private fun generateRandomEvent(): RandomEvent {
        val events = listOf(
            RandomEvent("ev_pozo", "Tormenta de Fichas", "El pozo se agita. Se incrementan las oportunidades de juego.", "POZO"),
            RandomEvent("ev_loca", "Ficha Loca", "Las fichas dobles valen el doble de puntos esta ronda.", "SCORE"),
            RandomEvent("ev_bonus", "Bendicion Divina", "Bonus de 20 puntos aplicado al jugador activo.", "SCORE"),
            RandomEvent("ev_inv", "Inversion Subita", "El sentido del turno se ha invertido.", "TURN")
        )
        return events.random()
    }

    fun drawTile(playerId: String): Pair<Boolean, String?> {
        val current = _session.value
        if (current.currentTurnPlayerId != playerId) return Pair(false, "No es tu turno.")
        if (current.boneyard.isEmpty()) return Pair(false, "El pozo esta vacio. No quedan fichas para robar.")
        if (current.status != GameStatus.PLAYING) return Pair(false, "Partida inactiva.")

        val player = current.players.find { it.id == playerId } ?: return Pair(false, "Jugador no encontrado.")
        
        val drawnTile = current.boneyard.first()
        val newBoneyard = current.boneyard.drop(1)

        val updatedTiles = player.tiles + drawnTile
        val updatedPlayers = current.players.map { if (it.id == playerId) it.copy(tiles = updatedTiles) else it }

        val newSession = current.copy(
            players = updatedPlayers,
            boneyard = newBoneyard,
            activeEvent = RandomEvent("ev_draw", "Robo en el Pozo", "${player.name} ha robado una ficha del pozo.", "INFO")
        )
        _session.value = newSession
        firebaseDataSource.syncGameSession(newSession.roomId, newSession)
        return Pair(true, null)
    }

    fun usePower(playerId: String, powerType: PowerType): Pair<Boolean, String?> {
        val current = _session.value
        if (current.status != GameStatus.PLAYING) return Pair(false, "Partida inactiva.")
        val player = current.players.find { it.id == playerId } ?: return Pair(false, "Jugador no encontrado.")
        val currentCount = player.powerCounts[powerType] ?: 0
        if (currentCount <= 0) return Pair(false, "No te quedan cargas de este poder.")

        val updatedPowers = player.powerCounts.toMutableMap()
        updatedPowers[powerType] = currentCount - 1

        val updatedPlayers = current.players.map { if (it.id == playerId) it.copy(powerCounts = updatedPowers) else it }

        var nextTurn = current.currentTurnPlayerId
        var activeEvent = current.activeEvent

        when (powerType) {
            PowerType.SKIP -> {
                nextTurn = playerId
                activeEvent = RandomEvent("ev_skip", "Poder Salto", "${player.name} ha saltado el turno del rival.", "POWER")
            }
            PowerType.REVERSE -> {
                nextTurn = playerId
                activeEvent = RandomEvent("ev_rev", "Poder Inversion", "El orden de la partida ha sido invertido.", "POWER")
            }
            PowerType.WILDCARD -> {
                activeEvent = RandomEvent("ev_wild", "Comodin Activo", "${player.name} puede colocar cualquier ficha sin restriccion.", "POWER")
            }
            PowerType.SWAP -> {
                val opponent = current.players.firstOrNull { it.id != playerId }
                if (opponent != null && opponent.tiles.isNotEmpty() && player.tiles.isNotEmpty()) {
                    val myTileToGive = player.tiles.first()
                    val oppTileToGet = opponent.tiles.random()

                    val newMyTiles = player.tiles.filterNot { it.id == myTileToGive.id } + oppTileToGet
                    val newOppTiles = opponent.tiles.filterNot { it.id == oppTileToGet.id } + myTileToGive

                    val swappedPlayers = updatedPlayers.map { p ->
                        when (p.id) {
                            playerId -> p.copy(tiles = newMyTiles, powerCounts = updatedPowers)
                            opponent.id -> p.copy(tiles = newOppTiles)
                            else -> p
                        }
                    }
                    val newSession = current.copy(
                        players = swappedPlayers,
                        activeEvent = RandomEvent("ev_swap", "Intercambio Mistico", "Se ha intercambiado una ficha con el oponente.", "POWER")
                    )
                    _session.value = newSession
                    firebaseDataSource.syncGameSession(newSession.roomId, newSession)
                    return Pair(true, null)
                }
            }
        }

        val newSession = current.copy(
            players = updatedPlayers,
            currentTurnPlayerId = nextTurn,
            activeEvent = activeEvent
        )
        _session.value = newSession
        firebaseDataSource.syncGameSession(newSession.roomId, newSession)
        return Pair(true, null)
    }

    fun useSwapPowerWithTile(playerId: String, tileToGive: Tile): Pair<Boolean, String?> {
        val current = _session.value
        if (current.status != GameStatus.PLAYING) return Pair(false, "Partida inactiva.")
        val player = current.players.find { it.id == playerId } ?: return Pair(false, "Jugador no encontrado.")
        val currentCount = player.powerCounts[PowerType.SWAP] ?: 0
        if (currentCount <= 0) return Pair(false, "No te quedan cargas de Intercambio Mistico.")

        val opponent = current.players.firstOrNull { it.id != playerId } ?: return Pair(false, "No hay oponente.")
        if (opponent.tiles.isEmpty()) return Pair(false, "El oponente no tiene fichas para intercambiar.")

        val updatedPowers = player.powerCounts.toMutableMap()
        updatedPowers[PowerType.SWAP] = currentCount - 1

        val updatedPlayers = current.players.map { if (it.id == playerId) it.copy(powerCounts = updatedPowers) else it }

        val myMatching = player.tiles.find { 
            (it.left == tileToGive.left && it.right == tileToGive.right) || 
            (it.left == tileToGive.right && it.right == tileToGive.left) 
        } ?: player.tiles.first()

        val oppTileToGet = opponent.tiles.random()

        val newMyTiles = player.tiles.filterNot { it.id == myMatching.id } + oppTileToGet
        val newOppTiles = opponent.tiles.filterNot { it.id == oppTileToGet.id } + myMatching

        val swappedPlayers = updatedPlayers.map { p ->
            when (p.id) {
                playerId -> p.copy(tiles = newMyTiles, powerCounts = updatedPowers)
                opponent.id -> p.copy(tiles = newOppTiles)
                else -> p
            }
        }

        val newSession = current.copy(
            players = swappedPlayers,
            activeEvent = RandomEvent("ev_swap", "Intercambio Mistico", "${player.name} intercambio su ficha [${myMatching.left}|${myMatching.right}] por una ficha secreta del rival.", "POWER")
        )
        _session.value = newSession
        firebaseDataSource.syncGameSession(newSession.roomId, newSession)
        return Pair(true, null)
    }

    fun restartGame() {
        _session.value = createInitialSession(roomId, isHost)
    }
}
