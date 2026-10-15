package com.example.godsmino.data.model

data class Player(
    val id: String,
    val name: String,
    val score: Int = 0,
    val tiles: List<Tile> = emptyList(),
    val powerCounts: Map<PowerType, Int> = PowerType.entries.associateWith { it.maxUses }
)
