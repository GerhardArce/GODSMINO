package com.example.godsmino.data.repository

import com.example.godsmino.data.datasource.FirebaseGameDataSource
import com.example.godsmino.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class GameRepository {
    private val firebaseDataSource = FirebaseGameDataSource()
    private val _session = MutableStateFlow(createInitialSession())
    val session: Flow<GameSession> = _session.asStateFlow()

    private fun createInitialSession(): GameSession {
        val allTiles = mutableListOf<Tile>()
        for (i in 0..6) {
            for (j in i..6) {
                allTiles.add(Tile(left = i, right = j))
            }
        }
        allTiles.shuffle()

        val p1Tiles = allTiles.subList(0, 7).toList()
        val p2Tiles = allTiles.subList(7, 14).toList()
        val remainingBoneyard = allTiles.size - 14

        val p1 = Player(id = "p1", name = "Jugador 1 (Tú)", tiles = p1Tiles, score = 0)
        val p2 = Player(id = "p2", name = "Oponente (Bot)", tiles = p2Tiles, score = 0)

        val initialSession = GameSession(
            roomId = "room_godsmino_1",
            board = emptyList(),
            players = listOf(p1, p2),
            currentTurnPlayerId = p1.id,
            boneyardSize = remainingBoneyard,
            status = GameStatus.PLAYING,
            activeEvent = RandomEvent("ev_init", "✨ ¡Inicio de Partida!", "Arrastra tus fichas en 4 direcciones (Arriba, Abajo, Izquierda, Derecha).", "INFO"),
            roundNumber = 1
        )

        firebaseDataSource.syncGameSession(initialSession.roomId, initialSession)
        return initialSession
    }

    fun makeMove(playerId: String, tile: Tile, direction: BoardDirection, useWildcard: Boolean = false) {
        val current = _session.value
        if (current.currentTurnPlayerId != playerId || current.status != GameStatus.PLAYING) return

        val player = current.players.find { it.id == playerId } ?: return
        if (!player.tiles.contains(tile) && !useWildcard) return

        val leftEnd = current.getLeftEnd()
        val rightEnd = current.getRightEnd()

        if (!useWildcard && current.board.isNotEmpty()) {
            val matchesLeft = leftEnd != null && (tile.left == leftEnd || tile.right == leftEnd)
            val matchesRight = rightEnd != null && (tile.left == rightEnd || tile.right == rightEnd)
            if (!matchesLeft && !matchesRight) return
        }

        val newBoard = if (current.board.isEmpty()) {
            listOf(tile)
        } else when (direction) {
            BoardDirection.RIGHT, BoardDirection.DOWN -> {
                val lastRight = current.getRightEnd() ?: tile.left
                val alignedTile = if (tile.left == lastRight) tile else tile.copy(left = tile.right, right = tile.left)
                current.board + alignedTile
            }
            BoardDirection.LEFT, BoardDirection.UP -> {
                val firstLeft = current.getLeftEnd() ?: tile.right
                val alignedTile = if (tile.right == firstLeft) tile else tile.copy(left = tile.right, right = tile.left)
                listOf(alignedTile) + current.board
            }
        }

        val updatedTiles = if (useWildcard) {
            player.tiles.firstOrNull { it.id == tile.id }?.let { player.tiles - it } ?: player.tiles
        } else {
            player.tiles.filterNot { it.id == tile.id }
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
    }

    private fun generateRandomEvent(): RandomEvent {
        val events = listOf(
            RandomEvent("ev_pozo", "⛈️ Tormenta de Fichas", "El pozo se agita. Se incrementan las oportunidades.", "POZO"),
            RandomEvent("ev_loca", "🤪 Ficha Loca", "Las fichas dobles valen el doble de puntos esta ronda.", "SCORE"),
            RandomEvent("ev_bonus", "⭐ Bendición Divina", "¡Bonus de +20 puntos para el jugador activo!", "SCORE"),
            RandomEvent("ev_inv", "🔄 Inversión Súbita", "El sentido del turno se ha invertido.", "TURN")
        )
        return events.random()
    }

    fun drawTile(playerId: String) {
        val current = _session.value
        if (current.currentTurnPlayerId != playerId || current.boneyardSize <= 0 || current.status != GameStatus.PLAYING) return

        val player = current.players.find { it.id == playerId } ?: return
        val newTile = Tile(left = (0..6).random(), right = (0..6).random())

        val updatedTiles = player.tiles + newTile
        val updatedPlayers = current.players.map { if (it.id == playerId) it.copy(tiles = updatedTiles) else it }

        val newSession = current.copy(
            players = updatedPlayers,
            boneyardSize = current.boneyardSize - 1
        )
        _session.value = newSession
        firebaseDataSource.syncGameSession(newSession.roomId, newSession)
    }

    fun usePower(playerId: String, powerType: PowerType) {
        val current = _session.value
        if (current.status != GameStatus.PLAYING) return
        val player = current.players.find { it.id == playerId } ?: return
        val currentCount = player.powerCounts[powerType] ?: 0
        if (currentCount <= 0) return

        val updatedPowers = player.powerCounts.toMutableMap()
        updatedPowers[powerType] = currentCount - 1

        val updatedPlayers = current.players.map { if (it.id == playerId) it.copy(powerCounts = updatedPowers) else it }

        var nextTurn = current.currentTurnPlayerId
        var activeEvent = current.activeEvent

        when (powerType) {
            PowerType.SKIP -> {
                nextTurn = playerId
                activeEvent = RandomEvent("ev_skip", "⚡ ¡Poder Salto!", "${player.name} ha saltado el turno del oponente.", "POWER")
            }
            PowerType.REVERSE -> {
                activeEvent = RandomEvent("ev_rev", "🔄 ¡Poder Inversión!", "El sentido de la partida ha sido invertido.", "POWER")
            }
            PowerType.WILDCARD -> {
                activeEvent = RandomEvent("ev_wild", "🌟 ¡Comodín Activo!", "${player.name} puede colocar cualquier ficha sin restricción.", "POWER")
            }
            PowerType.SWAP -> {
                val opponent = current.players.firstOrNull { it.id != playerId }
                if (opponent != null && opponent.tiles.isNotEmpty() && player.tiles.isNotEmpty()) {
                    val myTileToGive = player.tiles.first()
                    val oppTileToGet = opponent.tiles.first()

                    val newMyTiles = player.tiles.drop(1) + oppTileToGet
                    val newOppTiles = opponent.tiles.drop(1) + myTileToGive

                    val swappedPlayers = updatedPlayers.map { p ->
                        when (p.id) {
                            playerId -> p.copy(tiles = newMyTiles, powerCounts = updatedPowers)
                            opponent.id -> p.copy(tiles = newOppTiles)
                            else -> p
                        }
                    }
                    val newSession = current.copy(
                        players = swappedPlayers,
                        activeEvent = RandomEvent("ev_swap", "🔀 ¡Intercambio Épico!", "¡Se ha intercambiado una ficha con el oponente!", "POWER")
                    )
                    _session.value = newSession
                    firebaseDataSource.syncGameSession(newSession.roomId, newSession)
                    return
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
    }

    fun restartGame() {
        _session.value = createInitialSession()
    }
}
