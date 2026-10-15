package com.example.godsmino.data.model

data class GameSession(
    val roomId: String = "room_default",
    val board: List<Tile> = emptyList(),
    val players: List<Player> = emptyList(),
    val currentTurnPlayerId: String = "",
    val boneyardSize: Int = 14,
    val status: GameStatus = GameStatus.WAITING,
    val activeEvent: RandomEvent? = null,
    val roundNumber: Int = 1,
    val isClockwise: Boolean = true
) {
    fun getLeftEnd(): Int? = board.firstOrNull()?.left
    fun getRightEnd(): Int? = board.lastOrNull()?.right
}
