package com.example.godsmino.data.repository

import com.example.godsmino.data.datasource.FirebaseGameDataSource
import com.example.godsmino.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.random.Random

class GameRepository(
    private val roomId: String = "room_godsmino_1", 
    private val isHost: Boolean = true,
    hostName: String = "Anfitrión",
    ioDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) {
    private val firebaseDataSource = FirebaseGameDataSource()
    private val scope = CoroutineScope(ioDispatcher)
    private val _session = MutableStateFlow(
        if (isHost) {
            createInitialSession(
                roomId,
                hostName.ifBlank { "Anfitrión" },
                if (roomId == "room_offline_bot") "Bot (IA)" else "Esperando Oponente..."
            )
        } else {
            GameSession(roomId = roomId)
        }
    )
    val session: StateFlow<GameSession> = _session.asStateFlow()

    init {
        scope.launch {
            try {
                if (!isHost) {
                    val remoteSession = firebaseDataSource.getExistingSession(roomId)
                    if (remoteSession != null) {
                        _session.value = remoteSession
                    }
                }

                firebaseDataSource.observeGameSession(roomId).collect { updatedSession ->
                    if (updatedSession != null) {
                        val currentStatus = _session.value.status
                        if (currentStatus == GameStatus.PLAYING && updatedSession.status == GameStatus.WAITING) {
                            return@collect
                        }
                        if (updatedSession != _session.value) {
                            _session.value = updatedSession
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun cleanup() {
        scope.cancel()
    }

    private fun createInitialSession(
        roomId: String, 
        p1Name: String, 
        p2Name: String, 
        p1Score: Int = 0, 
        p2Score: Int = 0, 
        nextRound: Int = 1
    ): GameSession {
        val allTiles = mutableListOf<Tile>()
        for (i in 0..6) {
            for (j in i..6) {
                allTiles.add(Tile(left = i, right = j))
            }
        }
        allTiles.shuffle()

        val isBotGame = roomId == "room_offline_bot"

        val p1Tiles = allTiles.subList(0, 7).toList()
        val p2Tiles = allTiles.subList(7, 14).toList()
        val remainingBoneyard = if (isBotGame) allTiles.subList(14, 28).toList() else allTiles.subList(7, 28).toList()

        val cleanP1Name = p1Name.ifBlank { "Anfitrión" }
        val cleanP2Name = p2Name.ifBlank { if (isBotGame) "Bot (IA)" else "Esperando Oponente..." }

        val p1 = Player(id = "p1", name = cleanP1Name, tiles = p1Tiles, score = p1Score)
        val p2 = Player(id = "p2", name = cleanP2Name, tiles = p2Tiles, score = p2Score)

        val initialSession = GameSession(
            roomId = roomId,
            board = emptyList(),
            players = if (isBotGame) listOf(p1, p2) else listOf(p1),
            currentTurnPlayerId = p1.id,
            boneyard = remainingBoneyard,
            status = if (isBotGame) GameStatus.PLAYING else GameStatus.WAITING,
            activeEvent = RandomEvent("ev_init", "Inicio de Partida", if (isBotGame) "Partida vs Bot iniciada." else "Esperando oponente en sala [$roomId].", "INFO"),
            roundNumber = nextRound
        )

        firebaseDataSource.syncGameSession(roomId, initialSession)
        return initialSession
    }

    fun joinOnlineRoom(playerName: String): Pair<Boolean, String?> {
        val current = _session.value
        val cleanP2Name = playerName.ifBlank { "Jugador 2" }

        val existingP1 = current.players.find { it.id == "p1" }
        val existingP2 = current.players.find { it.id == "p2" }

        val allTiles = mutableListOf<Tile>()
        for (i in 0..6) {
            for (j in i..6) {
                allTiles.add(Tile(left = i, right = j))
            }
        }
        allTiles.shuffle()

        val p1 = if (existingP1 != null && existingP1.tiles.isNotEmpty()) {
            existingP1
        } else {
            Player(id = "p1", name = "Anfitrión", tiles = allTiles.subList(0, 7))
        }

        val p2Tiles = if (existingP2 != null && existingP2.tiles.isNotEmpty()) {
            existingP2.tiles
        } else if (current.boneyard.size >= 7) {
            current.boneyard.take(7)
        } else {
            allTiles.subList(7, 14)
        }

        val newBoneyard = if (existingP2 == null && current.boneyard.size >= 7) {
            current.boneyard.drop(7)
        } else if (current.boneyard.isEmpty()) {
            allTiles.subList(14, 28)
        } else {
            current.boneyard
        }

        val p2 = existingP2?.copy(name = cleanP2Name) ?: Player(id = "p2", name = cleanP2Name, tiles = p2Tiles, score = 0)

        val updatedPlayers = listOf(p1, p2)
        val newSession = current.copy(
            players = updatedPlayers,
            boneyard = newBoneyard,
            activeEvent = RandomEvent("ev_join", "Jugador Conectado", "$cleanP2Name se ha unido a la sala.", "INFO")
        )
        _session.value = newSession
        firebaseDataSource.syncGameSession(roomId, newSession)
        return Pair(true, null)
    }

    fun startGameSession(): Pair<Boolean, String?> {
        val current = _session.value
        if (current.players.size < 2 && current.roomId != "room_offline_bot") {
            return Pair(false, "Se necesita al menos 1 oponente para iniciar.")
        }
        val newSession = current.copy(
            status = GameStatus.PLAYING,
            activeEvent = RandomEvent("ev_start", "Partida Iniciada", "¡Que comience el duelo de Godsmino!", "INFO")
        )
        _session.value = newSession
        firebaseDataSource.syncGameSession(roomId, newSession)
        return Pair(true, null)
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
                    tileToCheck.copy(left = tileToCheck.right, right = tileToCheck.left)
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

    fun passTurn(playerId: String): Pair<Boolean, String?> {
        val current = _session.value
        if (current.currentTurnPlayerId != playerId) return Pair(false, "No es tu turno.")
        if (current.status != GameStatus.PLAYING) return Pair(false, "Partida inactiva.")

        val player = current.players.find { it.id == playerId } ?: return Pair(false, "Jugador no encontrado.")
        val nextTurnId = current.players.firstOrNull { it.id != playerId }?.id ?: playerId

        val newSession = current.copy(
            currentTurnPlayerId = nextTurnId,
            activeEvent = RandomEvent("ev_pass", "Paso de Turno", "${player.name} ha pasado el turno.", "INFO")
        )

        val evaluatedSession = evaluateTranca(newSession)
        _session.value = evaluatedSession
        firebaseDataSource.syncGameSession(evaluatedSession.roomId, evaluatedSession)
        return Pair(true, null)
    }

    fun usePower(playerId: String, powerType: PowerType): Pair<Boolean, String?> {
        val current = _session.value
        if (current.status != GameStatus.PLAYING) return Pair(false, "Partida inactiva.")
        val player = current.players.find { it.id == playerId } ?: return Pair(false, "Jugador no encontrado.")
        val currentCount = player.powerCounts[powerType.name] ?: 0
        if (currentCount <= 0) return Pair(false, "No te quedan cargas de este poder.")

        val updatedPowers = player.powerCounts.toMutableMap()
        updatedPowers[powerType.name] = currentCount - 1

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
            else -> {}
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
        val currentCount = player.powerCounts[PowerType.SWAP.name] ?: 0
        if (currentCount <= 0) return Pair(false, "No te quedan cargas de Intercambio Mistico.")

        val opponent = current.players.firstOrNull { it.id != playerId } ?: return Pair(false, "No hay oponente.")
        if (opponent.tiles.isEmpty()) return Pair(false, "El oponente no tiene fichas para intercambiar.")

        val updatedPowers = player.powerCounts.toMutableMap()
        updatedPowers[PowerType.SWAP.name] = currentCount - 1

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

    private fun evaluateTranca(session: GameSession): GameSession {
        if (session.status != GameStatus.PLAYING || session.board.isEmpty() || session.boneyard.isNotEmpty()) {
            return session
        }

        val leftEnd = session.getLeftEnd()
        val rightEnd = session.getRightEnd()

        val p1 = session.players.getOrNull(0) ?: return session
        val p2 = session.players.getOrNull(1) ?: return session

        val p1CanPlay = p1.tiles.any { t -> (leftEnd != null && (t.left == leftEnd || t.right == leftEnd)) || (rightEnd != null && (t.right == rightEnd || t.left == rightEnd)) }
        val p2CanPlay = p2.tiles.any { t -> (leftEnd != null && (t.left == leftEnd || t.right == leftEnd)) || (rightEnd != null && (t.right == rightEnd || t.left == rightEnd)) }

        if (!p1CanPlay && !p2CanPlay) {
            val p1Sum = p1.tiles.sumOf { if (session.activeEvent?.id == "ev_loca" && it.isDouble()) it.sumPoints() * 2 else it.sumPoints() }
            val p2Sum = p2.tiles.sumOf { if (session.activeEvent?.id == "ev_loca" && it.isDouble()) it.sumPoints() * 2 else it.sumPoints() }

            val p1PowersUsed = 5 - p1.powerCounts.values.sum()
            val p2PowersUsed = 5 - p2.powerCounts.values.sum()

            val (winnerId, desc) = when {
                p1Sum < p2Sum -> Pair(p1.id, "¡Tranca de juego! Ganó ${p1.name} por menor acumulado de puntos ($p1Sum vs $p2Sum).")
                p2Sum < p1Sum -> Pair(p2.id, "¡Tranca de juego! Ganó ${p2.name} por menor acumulado de puntos ($p2Sum vs $p1Sum).")
                else -> {
                    when {
                        p1PowersUsed < p2PowersUsed -> Pair(p1.id, "¡Tranca y empate de puntos! Ganó ${p1.name} por usar menos poderes ($p1PowersUsed vs $p2PowersUsed).")
                        p2PowersUsed < p1PowersUsed -> Pair(p2.id, "¡Tranca y empate de puntos! Ganó ${p2.name} por usar menos poderes ($p2PowersUsed vs $p1PowersUsed).")
                        else -> Pair(p1.id, "¡Tranca y empate perfecto en puntos y poderes!")
                    }
                }
            }

            val opponentTilesSum = session.players.firstOrNull { it.id != winnerId }?.tiles?.sumOf { t -> 
                if (session.activeEvent?.id == "ev_loca" && t.isDouble()) t.sumPoints() * 2 else t.sumPoints() 
            } ?: 0

            val updatedPlayers = session.players.map { p ->
                if (p.id == winnerId) p.copy(score = p.score + opponentTilesSum + 50) else p
            }

            return session.copy(
                status = GameStatus.FINISHED,
                players = updatedPlayers,
                currentTurnPlayerId = winnerId,
                activeEvent = RandomEvent("ev_tranca", "¡Tranca de Juego!", desc, "INFO")
            )
        }
        return session
    }

    fun forfeitGame(playerId: String): Pair<Boolean, String?> {
        val current = _session.value
        if (current.status != GameStatus.PLAYING) return Pair(false, "Partida inactiva.")

        val winner = current.players.firstOrNull { it.id != playerId } ?: return Pair(false, "Oponente no encontrado.")
        val updatedPlayers = current.players.map {
            if (it.id == winner.id) it.copy(score = it.score + 100) else it
        }

        val newSession = current.copy(
            status = GameStatus.FINISHED,
            players = updatedPlayers,
            currentTurnPlayerId = winner.id,
            activeEvent = RandomEvent("ev_forfeit", "Abandono de Partida", "Un jugador ha abandonado la partida.", "INFO")
        )
        _session.value = newSession
        firebaseDataSource.syncGameSession(roomId, newSession)
        return Pair(true, null)
    }

    fun restartGame() {
        _session.value = createInitialSession(roomId, "Anfitrión", "Bot (IA)")
    }
}
