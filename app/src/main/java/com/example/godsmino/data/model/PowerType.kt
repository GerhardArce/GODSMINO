package com.example.godsmino.data.model

enum class PowerType(val displayName: String, val maxUses: Int) {
    WILDCARD("Comodín", 2),
    SKIP("Salto", 1),
    SWAP("Intercambio", 1),
    REVERSE("Inversión", 1)
}
