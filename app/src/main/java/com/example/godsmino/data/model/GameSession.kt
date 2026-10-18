package com.example.godsmino.data.model

data class GameSession(
    val roomId: String = "room_default",
    val board: List<Tile> = emptyList(),
    val players: List<Player> = emptyList(),
    val currentTurnPlayerId: String = "",
    val boneyard: List<Tile> = emptyList(),
    val status: GameStatus = GameStatus.WAITING,
    val activeEvent: RandomEvent? = null,
    val roundNumber: Int = 1,
    val isClockwise: Boolean = true
) {
    val boneyardSize: Int get() = boneyard.size
    fun getLeftEnd(): Int? = board.firstOrNull()?.left
    fun getRightEnd(): Int? = board.lastOrNull()?.right
}
