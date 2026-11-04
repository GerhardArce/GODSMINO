package com.example.godsmino.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.godsmino.data.model.BoardDirection
import com.example.godsmino.data.model.GameStatus
import com.example.godsmino.data.model.PowerType
import com.example.godsmino.data.model.Tile
import com.example.godsmino.data.repository.GameRepository
import com.example.godsmino.util.GameStatsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import kotlinx.coroutines.Job

class GameViewModel(application: Application = Application()) : AndroidViewModel(application) {
    private var repository = GameRepository("room_godsmino_1", true)
    private var statsRecorded = false
    private var observeJob: Job? = null
    var localPlayerId = "p1"
        private set

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        observeRepository()
    }

    fun startRoom(roomId: String, isHost: Boolean, playerName: String = "Jugador") {
        repository.cleanup()
        localPlayerId = if (isHost) "p1" else "p2"
        repository = GameRepository(roomId, isHost, hostName = playerName)
        statsRecorded = false
        _uiState.update { GameUiState(session = repository.session.value) }
        observeRepository(playerName)
    }

    private fun observeRepository(playerName: String = "Jugador") {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.session.collect { session ->
                _uiState.update { current ->
                    if (current.session.status == GameStatus.PLAYING && session.status == GameStatus.WAITING) {
                        current
                    } else {
                        current.copy(session = session)
                    }
                }

                if (localPlayerId == "p2" && session.status == GameStatus.WAITING && session.players.none { it.id == "p2" }) {
                    repository.joinOnlineRoom(playerName)
                }

                if (session.status == GameStatus.FINISHED && !statsRecorded) {
                    statsRecorded = true
                    try {
                        val p1 = session.players.find { it.id == localPlayerId }
                        val opponent = session.players.find { it.id != localPlayerId }
                        val isWin = session.currentTurnPlayerId == localPlayerId
                        val score = p1?.score ?: 0
                        val opponentName = opponent?.name?.takeIf { 
                            it.isNotBlank() && it != "Esperando Oponente..." && it != "Oponente" 
                        } ?: if (session.roomId == "room_offline_bot") "Bot (IA)" else "Oponente Online"
                        val rounds = session.roundNumber

                        GameStatsManager.recordMatch(
                            context = getApplication(),
                            isWin = isWin,
                            score = score,
                            opponentName = opponentName,
                            rounds = rounds
                        )
                    } catch (_: Exception) {}
                } else if (session.status != GameStatus.FINISHED) {
                    statsRecorded = false
                }
                checkBotTurn(session.currentTurnPlayerId, session.status, session.roomId)
            }
        }
    }

    fun joinOnlineRoom(playerName: String) {
        viewModelScope.launch {
            val (success, error) = repository.joinOnlineRoom(playerName)
            if (!success) {
                _uiState.update { it.copy(errorMessage = error) }
            } else {
                _uiState.update { it.copy(session = repository.session.value, errorMessage = null) }
            }
        }
    }

    fun startCoinToss() {
        val (success, error) = repository.startCoinToss()
        if (!success) {
            _uiState.update { it.copy(errorMessage = error) }
        } else {
            _uiState.update { it.copy(session = repository.session.value, errorMessage = null) }
        }
    }

    fun finishCoinToss(winnerPlayerId: String) {
        val (success, error) = repository.finishCoinToss(winnerPlayerId)
        if (!success) {
            _uiState.update { it.copy(errorMessage = error) }
        } else {
            _uiState.update { it.copy(session = repository.session.value, errorMessage = null) }
        }
    }

    fun startGameSession() {
        startCoinToss()
    }

    private fun checkBotTurn(currentTurnPlayerId: String, status: GameStatus, roomId: String) {
        if (roomId == "room_offline_bot" && currentTurnPlayerId == "p2" && status == GameStatus.PLAYING) {
            viewModelScope.launch {
                delay(1200)
                triggerBotMove()
            }
        }
    }

    private fun triggerBotMove() {
        val session = _uiState.value.session
        val bot = session.players.find { it.id == "p2" } ?: return
        if (session.currentTurnPlayerId != "p2") return

        val leftEnd = session.getLeftEnd()
        val rightEnd = session.getRightEnd()

        val validTile = bot.tiles.firstOrNull { tile ->
            session.board.isEmpty() ||
            (leftEnd != null && (tile.left == leftEnd || tile.right == leftEnd)) ||
            (rightEnd != null && (tile.left == rightEnd || tile.right == rightEnd))
        }

        if (validTile != null) {
            val direction = if (rightEnd != null && (validTile.left == rightEnd || validTile.right == rightEnd)) BoardDirection.RIGHT else BoardDirection.LEFT
            repository.makeMove("p2", validTile, direction = direction)
        } else if (session.boneyardSize > 0) {
            repository.drawTile("p2")
        } else {
            val (success, _) = repository.usePower("p2", PowerType.SKIP)
            if (!success) {
                repository.passTurn("p2")
            }
        }
    }

    fun selectTile(tile: Tile) {
        _uiState.update { it.copy(selectedTile = if (it.selectedTile == tile) null else tile) }
    }

    fun selectPower(powerType: PowerType) {
        _uiState.update { it.copy(selectedPower = if (it.selectedPower == powerType) null else powerType) }
    }

    fun requestUsePower(powerType: PowerType) {
        val currentUserId = _uiState.value.session.currentTurnPlayerId
        if (currentUserId != localPlayerId) {
            _uiState.update { it.copy(errorMessage = "No es tu turno para usar poderes.") }
            return
        }
        val player = _uiState.value.session.players.find { it.id == currentUserId } ?: return
        if ((player.powerCounts[powerType.name] ?: 0) <= 0) {
            _uiState.update { it.copy(errorMessage = "No te quedan cargas de este poder.") }
            return
        }
        _uiState.update {
            it.copy(
                pendingPowerDialog = powerType,
                selectedTileForSwap = player.tiles.firstOrNull()
            )
        }
    }

    fun selectSwapTile(tile: Tile) {
        _uiState.update { it.copy(selectedTileForSwap = tile) }
    }

    fun confirmUsePower() {
        val powerType = _uiState.value.pendingPowerDialog ?: return
        val currentUserId = _uiState.value.session.currentTurnPlayerId
        val tileToSwap = _uiState.value.selectedTileForSwap

        val (success, error) = if (powerType == PowerType.SWAP && tileToSwap != null) {
            repository.useSwapPowerWithTile(currentUserId, tileToSwap)
        } else {
            repository.usePower(currentUserId, powerType)
        }

        if (!success) {
            _uiState.update { it.copy(errorMessage = error, pendingPowerDialog = null) }
        } else {
            _uiState.update {
                it.copy(
                    pendingPowerDialog = null,
                    selectedTileForSwap = null,
                    selectedPower = if (powerType == PowerType.WILDCARD) PowerType.WILDCARD else null,
                    errorMessage = null
                )
            }
        }
    }

    fun dismissPowerDialog() {
        _uiState.update { it.copy(pendingPowerDialog = null, selectedTileForSwap = null) }
    }

    fun playSpecificTile(tile: Tile, direction: BoardDirection) {
        val currentUserId = _uiState.value.session.currentTurnPlayerId
        if (currentUserId != localPlayerId) {
            _uiState.update { it.copy(errorMessage = "No es tu turno actual.") }
            return
        }

        val useWildcard = _uiState.value.selectedPower == PowerType.WILDCARD
        val (success, error) = repository.makeMove(currentUserId, tile, direction, useWildcard)
        if (!success) {
            _uiState.update { it.copy(errorMessage = error) }
        } else {
            _uiState.update { it.copy(selectedTile = null, selectedPower = null, errorMessage = null) }
        }
    }

    fun playTile(direction: BoardDirection) {
        val tile = _uiState.value.selectedTile
        if (tile == null) {
            _uiState.update { it.copy(errorMessage = "Selecciona una ficha de tu mano primero.") }
            return
        }
        playSpecificTile(tile, direction)
    }

    fun drawTile() {
        val currentUserId = _uiState.value.session.currentTurnPlayerId
        if (currentUserId == localPlayerId) {
            val (success, error) = repository.drawTile(currentUserId)
            if (!success) {
                _uiState.update { it.copy(errorMessage = error) }
            } else {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    fun passTurn() {
        val currentUserId = _uiState.value.session.currentTurnPlayerId
        if (currentUserId == localPlayerId) {
            val (success, error) = repository.passTurn(currentUserId)
            if (!success) {
                _uiState.update { it.copy(errorMessage = error) }
            } else {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun toggleBoardTheme() {
        _uiState.update { it.copy(boardThemeIndex = (it.boardThemeIndex + 1) % 4) }
    }

    fun restartGame() {
        repository.restartGame()
        _uiState.update { GameUiState(session = repository.session.value) }
    }

    fun forfeitGame() {
        repository.forfeitGame(localPlayerId)
        _uiState.update { GameUiState(session = repository.session.value) }
    }

    override fun onCleared() {
        repository.cleanup()
        super.onCleared()
    }
}
