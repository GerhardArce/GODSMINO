package com.example.godsmino.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class RandomEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val effectType: String = ""
)
