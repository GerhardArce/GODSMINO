package com.example.godsmino.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Player(
    val id: String = "",
    val name: String = "",
    val score: Int = 0,
    val tiles: List<Tile> = emptyList(),
    val powerCounts: Map<String, Int> = PowerType.entries.associate { it.name to it.maxUses }
)
