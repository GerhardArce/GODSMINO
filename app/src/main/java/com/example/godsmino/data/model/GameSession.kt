package com.example.godsmino.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class GameSession(
    val roomId: String = "room_default",
    val board: List<Tile> = emptyList(),
    val players: List<Player> = emptyList(),
    val currentTurnPlayerId: String = "",
    val boneyard: List<Tile> = emptyList(),
    val status: GameStatus = GameStatus.WAITING,
    val activeEvent: RandomEvent? = null,
    val roundNumber: Int = 1
) {
    @get:Exclude
    val boneyardSize: Int get() = boneyard.size

    @Exclude
    fun getLeftEnd(): Int? = board.firstOrNull()?.left

    @Exclude
    fun getRightEnd(): Int? = board.lastOrNull()?.right
}
