package com.example.godsmino.data.model

enum class PowerType(val displayName: String, val description: String, val maxUses: Int) {
    WILDCARD("Comodín Divino", "Permite colocar cualquier ficha en el tablero sin restricciones de números durante este turno.", 2),
    SKIP("Salto Celestial", "Salta el turno del oponente para poder jugar otra vez de inmediato.", 1),
    SWAP("Intercambio Místico", "Elige una ficha de tu mano para intercambiarla por una ficha aleatoria del oponente.", 1),
    REVERSE("Inversión Temporal", "Invierte el orden de los turnos para desconcertar al rival.", 1)
}
