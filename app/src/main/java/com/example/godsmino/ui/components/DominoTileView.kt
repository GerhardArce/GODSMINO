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
        targetValue = if (isDragging || isSelected) 1.22f else 1.0f,
        label = "tileScale"
    )

    val backgroundColor = if (isSelected || isDragging) {
        Brush.verticalGradient(listOf(Color(0xFFFFFDE7), Color(0xFFFFF59D)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFAFAFA), Color(0xFFE0E0E0)))
    }

    val borderColor = if (isSelected || isDragging) Color(0xFFFFD700) else Color(0xFF9E9E9E)

    val width = if (isHorizontal) 96.dp else 52.dp
    val height = if (isHorizontal) 52.dp else 96.dp

    Box(
        modifier = Modifier
            .zIndex(if (isDragging) 999f else 0f)
            .graphicsLayer {
                translationX = offsetX
                translationY = offsetY
            }
            .padding(2.dp)
            .width(width)
            .height(height)
            .scale(scale)
            .shadow(if (isSelected || isDragging) 16.dp else 4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
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
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left half
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    DominoHalfValue(value = tile.left)
                }
                // Vertical Divider line with Brass Spinner Pin
                Box(
                    modifier = Modifier.width(2.dp).fillMaxHeight().background(Color(0xFF757575)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(6.dp)) {
                        drawCircle(color = Color(0xFFB8860B), radius = size.minDimension / 2f)
                        drawCircle(color = Color(0xFFFFD700), radius = size.minDimension / 4f)
                    }
                }
                // Right half
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    DominoHalfValue(value = tile.right)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top half
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    DominoHalfValue(value = tile.left)
                }
                // Horizontal Divider line with Brass Spinner Pin
                Box(
                    modifier = Modifier.height(2.dp).fillMaxWidth().background(Color(0xFF757575)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(6.dp)) {
                        drawCircle(color = Color(0xFFB8860B), radius = size.minDimension / 2f)
                        drawCircle(color = Color(0xFFFFD700), radius = size.minDimension / 4f)
                    }
                }
                // Bottom half
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    DominoHalfValue(value = tile.right)
                }
            }
        }
    }
}

@Composable
fun DominoHalfValue(value: Int) {
    val dotColor = Color(0xFF212121)
    val dotRadius = 4f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val qx = w / 3.8f
        val qy = h / 3.8f

        fun drawDot(x: Float, y: Float) {
            drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
            drawCircle(color = Color.White.copy(alpha = 0.5f), radius = 1.2f, center = Offset(x - 1f, y - 1f))
        }

        when (value) {
            1 -> drawDot(cx, cy)
            2 -> {
                drawDot(qx, qy)
                drawDot(w - qx, h - qy)
            }
            3 -> {
                drawDot(qx, qy)
                drawDot(cx, cy)
                drawDot(w - qx, h - qy)
            }
            4 -> {
                drawDot(qx, qy)
                drawDot(w - qx, qy)
                drawDot(qx, h - qy)
                drawDot(w - qx, h - qy)
            }
            5 -> {
                drawDot(qx, qy)
                drawDot(w - qx, qy)
                drawDot(cx, cy)
                drawDot(qx, h - qy)
                drawDot(w - qx, h - qy)
            }
            6 -> {
                drawDot(qx, qy)
                drawDot(w - qx, qy)
                drawDot(qx, cy)
                drawDot(w - qx, cy)
                drawDot(qx, h - qy)
                drawDot(w - qx, h - qy)
            }
        }
    }
}
