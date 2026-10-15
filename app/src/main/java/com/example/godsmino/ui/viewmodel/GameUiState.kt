package com.example.godsmino.ui.viewmodel

import com.example.godsmino.data.model.GameSession
import com.example.godsmino.data.model.PowerType
import com.example.godsmino.data.model.Tile

data class GameUiState(
    val session: GameSession = GameSession(),
    val selectedTile: Tile? = null,
    val selectedPower: PowerType? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
