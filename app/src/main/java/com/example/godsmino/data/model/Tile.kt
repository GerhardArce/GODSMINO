package com.example.godsmino.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.UUID

@IgnoreExtraProperties
data class Tile(
    val id: String = UUID.randomUUID().toString(),
    val left: Int = 0,
    val right: Int = 0
) {
    @Exclude
    fun isDouble(): Boolean = left == right

    @Exclude
    fun sumPoints(): Int = left + right
}
