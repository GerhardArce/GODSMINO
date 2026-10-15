package com.example.godsmino.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

@Composable
fun ConfettiOverlay(visible: Boolean) {
    if (!visible) return

    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Magenta, Color.Cyan, Color.White)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        repeat(120) {
            val x = Random.nextFloat() * width
            val y = Random.nextFloat() * height
            val radius = Random.nextFloat() * 8f + 4f
            val color = colors.random()

            drawCircle(
                color = color,
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}
