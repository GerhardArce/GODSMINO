package com.example.godsmino.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector

enum class PowerType(
    val displayName: String,
    val description: String,
    val maxUses: Int,
    val icon: ImageVector
) {
    WILDCARD(
        "Comodín Divino",
        "Permite colocar cualquier ficha en el tablero sin restricciones de números durante este turno.",
        2,
        Icons.Default.Star
    ),
    SKIP(
        "Salto Celestial",
        "Salta el turno del oponente para poder jugar otra vez de inmediato.",
        1,
        Icons.Default.FastForward
    ),
    SWAP(
        "Intercambio Místico",
        "Elige una ficha de tu mano para intercambiarla por una ficha aleatoria del oponente.",
        1,
        Icons.Default.SwapHoriz
    ),
    REVERSE(
        "Inversión Temporal",
        "Invierte el orden de los turnos para desconcertar al rival.",
        1,
        Icons.Default.Refresh
    )
}
