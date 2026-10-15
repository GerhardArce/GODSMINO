package com.example.godsmino.data.model

import java.util.UUID

data class Tile(
    val id: String = UUID.randomUUID().toString(),
    val left: Int,
    val right: Int
) {
    fun isDouble(): Boolean = left == right
    fun sumPoints(): Int = left + right
}
