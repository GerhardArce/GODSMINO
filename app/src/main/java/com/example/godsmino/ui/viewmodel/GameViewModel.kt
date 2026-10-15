package com.example.godsmino.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.godsmino.data.model.BoardDirection
import com.example.godsmino.data.model.GameStatus
import com.example.godsmino.data.model.PowerType
import com.example.godsmino.data.model.Tile
import com.example.godsmino.data.repository.GameRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val repository = GameRepository()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.session.collect { session ->
                _uiState.update { it.copy(session = session) }
                checkBotTurn(session.currentTurnPlayerId, session.status)
            }
        }
    }

    private fun checkBotTurn(currentTurnPlayerId: String, status: GameStatus) {
        if (currentTurnPlayerId == "p2" && status == GameStatus.PLAYING) {
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
            repository.usePower("p2", PowerType.SKIP)
        }
    }

    fun selectTile(tile: Tile) {
        _uiState.update { it.copy(selectedTile = if (it.selectedTile == tile) null else tile) }
    }

    fun selectPower(powerType: PowerType) {
        _uiState.update { it.copy(selectedPower = if (it.selectedPower == powerType) null else powerType) }
    }

    fun playTile(direction: BoardDirection) {
        val tile = _uiState.value.selectedTile ?: return
        val currentUserId = _uiState.value.session.currentTurnPlayerId
        if (currentUserId != "p1") return

        val useWildcard = _uiState.value.selectedPower == PowerType.WILDCARD

        repository.makeMove(currentUserId, tile, direction, useWildcard)
        _uiState.update { it.copy(selectedTile = null, selectedPower = null) }
    }

    fun drawTile() {
        val currentUserId = _uiState.value.session.currentTurnPlayerId
        if (currentUserId == "p1") {
            repository.drawTile(currentUserId)
        }
    }

    fun usePower(powerType: PowerType) {
        val currentUserId = _uiState.value.session.currentTurnPlayerId
        if (currentUserId == "p1") {
            repository.usePower(currentUserId, powerType)
            _uiState.update { it.copy(selectedPower = powerType) }
        }
    }

    fun restartGame() {
        repository.restartGame()
        _uiState.update { GameUiState() }
    }
}
