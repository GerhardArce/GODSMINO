package com.example.godsmino.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.godsmino.data.model.Tile
import com.example.godsmino.util.SoundManager

@Composable
fun DominoTileView(
    tile: Tile,
    isSelected: Boolean = false,
    isHorizontal: Boolean = false,
    isDraggable: Boolean = false,
    onClick: () -> Unit = {},
    onDragEnd: (Offset) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isDragging || isSelected) 1.28f else 1.0f,
        label = "tileScale"
    )

    // High-end professional physical ivory bone gradient
    val backgroundColor = if (isSelected || isDragging) {
        Brush.verticalGradient(
            listOf(Color(0xFFFFFFF8), Color(0xFFFFF8E1), Color(0xFFFFECB3))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFFFCFBF9), Color(0xFFF5EFEB), Color(0xFFE8E1D5))
        )
    }

    val borderColor = if (isSelected || isDragging) Color(0xFFD4AF37) else Color(0xFFB5A898)
    val borderWidth = if (isSelected || isDragging) 2.5.dp else 1.2.dp

    val width = if (isHorizontal) 104.dp else 58.dp
    val height = if (isHorizontal) 58.dp else 104.dp

    Box(
        modifier = Modifier
            .zIndex(if (isDragging) 9999f else 0f)
            .graphicsLayer {
                translationX = offsetX
                translationY = offsetY
            }
            .padding(3.dp)
            .width(width)
            .height(height)
            .scale(scale)
            .shadow(if (isDragging || isSelected) 24.dp else 6.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .then(
                if (isDraggable) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                SoundManager.playClickSound()
                                onClick()
                            },
                            onDragEnd = {
                                isDragging = false
                                SoundManager.playTileClack()
                                onDragEnd(Offset(offsetX, offsetY))
                                offsetX = 0f
                                offsetY = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                offsetX = 0f
                                offsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        if (isHorizontal) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left half
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    DominoHalfValue(value = tile.left)
                }
                // Center Divider with Realistic Brass Spinner Pin
                Box(
                    modifier = Modifier.width(2.5.dp).fillMaxHeight().background(Color(0xFFC5A880)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(9.dp)) {
                        drawCircle(color = Color(0xFF6D5226), radius = size.minDimension / 2f)
                        drawCircle(color = Color(0xFFD4AF37), radius = size.minDimension / 3.2f)
                        drawCircle(color = Color(0xFFFFF9C4), radius = size.minDimension / 6f)
                    }
                }
                // Right half
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    DominoHalfValue(value = tile.right)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top half
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DominoHalfValue(value = tile.left)
                }
                // Center Divider with Realistic Brass Spinner Pin
                Box(
                    modifier = Modifier.height(2.5.dp).fillMaxWidth().background(Color(0xFFC5A880)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(9.dp)) {
                        drawCircle(color = Color(0xFF6D5226), radius = size.minDimension / 2f)
                        drawCircle(color = Color(0xFFD4AF37), radius = size.minDimension / 3.2f)
                        drawCircle(color = Color(0xFFFFF9C4), radius = size.minDimension / 6f)
                    }
                }
                // Bottom half
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DominoHalfValue(value = tile.right)
                }
            }
        }
    }
}

@Composable
fun DominoHalfValue(value: Int) {
    val pipColor = Color(0xFF161616)
    val pipHighlightColor = Color(0xFFFFFFFF)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val qx = w / 3.4f
        val qy = h / 3.4f
        val pipRadius = 4.8f

        fun drawPip(x: Float, y: Float) {
            // Drop shadow / depth for pip
            drawCircle(color = pipHighlightColor.copy(alpha = 0.5f), radius = pipRadius + 0.9f, center = Offset(x + 0.6f, y + 0.6f))
            // Main Pip
            drawCircle(color = pipColor, radius = pipRadius, center = Offset(x, y))
            // Specular highlight on pip
            drawCircle(color = Color.White.copy(alpha = 0.45f), radius = 1.3f, center = Offset(x - 1.2f, y - 1.2f))
        }

        when (value) {
            1 -> drawPip(cx, cy)
            2 -> {
                drawPip(qx, qy)
                drawPip(w - qx, h - qy)
            }
            3 -> {
                drawPip(qx, qy)
                drawPip(cx, cy)
                drawPip(w - qx, h - qy)
            }
            4 -> {
                drawPip(qx, qy)
                drawPip(w - qx, qy)
                drawPip(qx, h - qy)
                drawPip(w - qx, h - qy)
            }
            5 -> {
                drawPip(qx, qy)
                drawPip(w - qx, qy)
                drawPip(cx, cy)
                drawPip(qx, h - qy)
                drawPip(w - qx, h - qy)
            }
            6 -> {
                drawPip(qx, qy)
                drawPip(w - qx, qy)
                drawPip(qx, cy)
                drawPip(w - qx, cy)
                drawPip(qx, h - qy)
                drawPip(w - qx, h - qy)
            }
        }
    }
}
