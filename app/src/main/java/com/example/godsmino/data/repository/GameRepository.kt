package com.example.godsmino.data.repository

import com.example.godsmino.data.datasource.FirebaseGameDataSource
import com.example.godsmino.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlin.random.Random

class GameRepository(private val roomId: String = "room_godsmino_1", private val isHost: Boolean = true) {
    private val firebaseDataSource = FirebaseGameDataSource()
    private val _session = MutableStateFlow(GameSession(roomId = roomId))
    val session: Flow<GameSession> = _session.asStateFlow()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            val remoteSession = firebaseDataSource.getExistingSession(roomId)
            if (remoteSession == null && isHost) {
                val initial = createInitialSession(roomId, "Jugador 1 (Tu)", "Oponente (Bot/Online)")
                _session.value = initial
            } else if (remoteSession != null) {
                _session.value = remoteSession
            }

            firebaseDataSource.observeGameSession(roomId).collect { updatedSession ->
                if (updatedSession != null) {
                    _session.value = updatedSession
                }
            }
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

        val p1Tiles = allTiles.subList(0, 7).toList()
        val p2Tiles = allTiles.subList(7, 14).toList()
        val remainingBoneyard = allTiles.subList(14, 28).toList()

        val p1 = Player(id = "p1", name = p1Name, tiles = p1Tiles, score = p1Score)
        val p2 = Player(id = "p2", name = p2Name, tiles = p2Tiles, score = p2Score)

        val initialSession = GameSession(
            roomId = roomId,
            board = emptyList(),
            players = listOf(p1, p2),
            currentTurnPlayerId = p1.id,
            boneyard = remainingBoneyard,
            status = GameStatus.PLAYING,
            activeEvent = RandomEvent("ev_init", "Inicio de Partida Online", "Sala [$roomId] conectada correctamente.", "INFO"),
            roundNumber = nextRound
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
                    tileToCheck.copy(left = tileToCheck.right, right = tileToCheck.left)
                } else {
                    tileToCheck
                }
                listOf(alignedTile) + current.board
            }
        }

        val updatedTiles = if (useWildcard) {
            val target = player.tiles.firstOrNull { (it.left == tile.left && it.right == tile.right) || (it.left == tile.right && it.right == tile.left) }
            if (target != null) player.tiles.filterNot { it.id == target.id } else return Pair(false, "Ficha objetivo no encontrada en la mano.")
        } else {
            if (matchingTile != null) player.tiles.filterNot { it.id == matchingTile.id } else return Pair(false, "No posees esta ficha.")
        }

        val isFinished = updatedTiles.isEmpty()
        val nextTurnId = current.players.first { it.id != playerId }.id

        val updatedPlayers = current.players.map {
            if (it.id == playerId) it.copy(tiles = updatedTiles) else it
        }

        var finalPlayers = updatedPlayers
        val finalStatus = if (isFinished) GameStatus.FINISHED else current.status

        if (isFinished) {
            val opponentTilesSum = updatedPlayers.filter { it.id != playerId }.sumOf { p -> 
                p.tiles.sumOf { t -> 
                    if (current.activeEvent?.id == "ev_loca" && t.isDouble()) t.sumPoints() * 2 else t.sumPoints() 
                } 
            }
            finalPlayers = updatedPlayers.map { p ->
                if (p.id == playerId) p.copy(score = p.score + opponentTilesSum + 50) else p
            }
        }

        val possibleEvent = if (!isFinished && Random.nextFloat() < 0.20f) {
            generateRandomEvent()
        } else current.activeEvent

        var finalNextTurnId = nextTurnId
        if (possibleEvent?.id == "ev_bonus" && possibleEvent != current.activeEvent) {
            finalPlayers = finalPlayers.map { p ->
                if (p.id == playerId) p.copy(score = p.score + 20) else p
            }
        } else if (possibleEvent?.id == "ev_inv" && possibleEvent != current.activeEvent) {
            finalNextTurnId = playerId
        }
        
        val finalBoneyard = if (possibleEvent?.id == "ev_pozo" && possibleEvent != current.activeEvent) {
            current.boneyard.shuffled()
        } else {
            current.boneyard
        }

        val newSession = current.copy(
            board = newBoard,
            players = finalPlayers,
            boneyard = finalBoneyard,
            currentTurnPlayerId = if (isFinished) playerId else finalNextTurnId,
            status = finalStatus,
            activeEvent = possibleEvent
        )

        val evaluatedSession = evaluateTranca(newSession)
        _session.value = evaluatedSession
        firebaseDataSource.syncGameSession(evaluatedSession.roomId, evaluatedSession)
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
        
        val leftEnd = current.getLeftEnd()
        val rightEnd = current.getRightEnd()
        val canPlay = current.board.isEmpty() || player.tiles.any { t -> 
            (leftEnd != null && (t.left == leftEnd || t.right == leftEnd)) || 
            (rightEnd != null && (t.left == rightEnd || t.right == rightEnd)) 
        }
        if (canPlay) return Pair(false, "Tienes fichas jugables, no puedes robar del pozo.")
        
        val drawnTile = current.boneyard.first()
        val newBoneyard = current.boneyard.drop(1)

        val updatedTiles = player.tiles + drawnTile
        val updatedPlayers = current.players.map { if (it.id == playerId) it.copy(tiles = updatedTiles) else it }

        val newSession = current.copy(
            players = updatedPlayers,
            boneyard = newBoneyard,
            activeEvent = RandomEvent("ev_draw", "Robo en el Pozo", "${player.name} ha robado una ficha del pozo.", "INFO")
        )
        val evaluatedSession = evaluateTranca(newSession)
        _session.value = evaluatedSession
        firebaseDataSource.syncGameSession(evaluatedSession.roomId, evaluatedSession)
        return Pair(true, null)
    }

    fun passTurn(playerId: String): Pair<Boolean, String?> {
        val current = _session.value
        if (current.currentTurnPlayerId != playerId) return Pair(false, "No es tu turno.")
        if (current.boneyard.isNotEmpty()) return Pair(false, "Aun hay fichas en el pozo.")
        
        val player = current.players.find { it.id == playerId } ?: return Pair(false, "Jugador no encontrado.")
        val leftEnd = current.getLeftEnd()
        val rightEnd = current.getRightEnd()
        
        val canPlay = current.board.isEmpty() || player.tiles.any { t -> 
            (leftEnd != null && (t.left == leftEnd || t.right == leftEnd)) || 
            (rightEnd != null && (t.left == rightEnd || t.right == rightEnd)) 
        }
        
        if (canPlay) return Pair(false, "Tienes fichas jugables, no puedes pasar el turno.")
        
        val nextTurnId = current.players.first { it.id != playerId }.id
        val newSession = current.copy(
            currentTurnPlayerId = nextTurnId,
            activeEvent = RandomEvent("ev_pass", "Paso de Turno", "El jugador no tiene fichas validas y el pozo esta vacio.", "INFO")
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
                return Pair(false, "El Intercambio Mistico requiere seleccionar una ficha de tu mano.")
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
        } ?: return Pair(false, "Ficha seleccionada no valida.")

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

        val p1CanPlay = p1.tiles.any { t -> (leftEnd != null && (t.left == leftEnd || t.right == leftEnd)) || (rightEnd != null && (t.left == rightEnd || t.right == rightEnd)) }
        val p2CanPlay = p2.tiles.any { t -> (leftEnd != null && (t.left == leftEnd || t.right == leftEnd)) || (rightEnd != null && (t.left == rightEnd || t.right == rightEnd)) }

        if (!p1CanPlay && !p2CanPlay) {
            val p1Sum = p1.tiles.sumOf { if (session.activeEvent?.id == "ev_loca" && it.isDouble()) it.sumPoints() * 2 else it.sumPoints() }
            val p2Sum = p2.tiles.sumOf { if (session.activeEvent?.id == "ev_loca" && it.isDouble()) it.sumPoints() * 2 else it.sumPoints() }

            val p1PowersUsed = 5 - p1.powerCounts.values.sum()
            val p2PowersUsed = 5 - p2.powerCounts.values.sum()

            val (winnerId, desc) = when {
                p1Sum < p2Sum -> Pair(p1.id, "¡Tranca de juego! Ganó ${p1.name} por menor acumulado de puntos (${p1Sum} vs ${p2Sum}).")
                p2Sum < p1Sum -> Pair(p2.id, "¡Tranca de juego! Ganó ${p2.name} por menor acumulado de puntos (${p2Sum} vs ${p1Sum}).")
                else -> {
                    when {
                        p1PowersUsed < p2PowersUsed -> Pair(p1.id, "¡Tranca y empate de puntos! Ganó ${p1.name} por usar menos poderes (${p1PowersUsed} vs ${p2PowersUsed}).")
                        p2PowersUsed < p1PowersUsed -> Pair(p2.id, "¡Tranca y empate de puntos! Ganó ${p2.name} por usar menos poderes (${p2PowersUsed} vs ${p1PowersUsed}).")
                        else -> Pair(p1.id, "¡Tranca y empate perfecto en puntos y poderes!")
                    }
                }
            }

            val opponentTilesSum = session.players.filter { it.id != winnerId }.sumOf { p -> 
                p.tiles.sumOf { t -> 
                    if (session.activeEvent?.id == "ev_loca" && t.isDouble()) t.sumPoints() * 2 else t.sumPoints() 
                } 
            }

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
        firebaseDataSource.syncGameSession(newSession.roomId, newSession)
        return Pair(true, null)
    }

    fun restartGame() {
        val current = _session.value
        val p1 = current.players.find { it.id == "p1" }
        val p2 = current.players.find { it.id == "p2" }
        
        val p1Score = p1?.score ?: 0
        val p2Score = p2?.score ?: 0
        val p1Name = p1?.name ?: "Jugador 1"
        val p2Name = p2?.name ?: "Jugador 2"
        val nextRound = current.roundNumber + 1
        
        val newSession = createInitialSession(roomId, p1Name, p2Name, p1Score, p2Score, nextRound)
        _session.value = newSession
    }
}
