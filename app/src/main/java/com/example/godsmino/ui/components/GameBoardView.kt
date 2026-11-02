package com.example.godsmino.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.godsmino.data.model.Tile

@Composable
fun GameBoardView(
    board: List<Tile>,
    themeIndex: Int = 0,
    modifier: Modifier = Modifier,
    onToggleTheme: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val (colors, borderColor, themeName) = when (themeIndex) {
        1 -> Triple(
            listOf(Color(0xFF37474F), Color(0xFF263238), Color(0xFF102027)),
            Color(0xFF546E7A),
            "Obsidiana"
        )
        2 -> Triple(
            listOf(Color(0xFF5D4037), Color(0xFF4E342E), Color(0xFF3E2723)),
            Color(0xFF8D6E63),
            "Caoba"
        )
        3 -> Triple(
            listOf(Color(0xFF1565C0), Color(0xFF0D47A1), Color(0xFF01579B)),
            Color(0xFF42A5F5),
            "Oceano"
        )
        else -> Triple(
            listOf(Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF09330C)),
            Color(0xFF4CAF50),
            "Clasico"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.radialGradient(colors = colors))
            .border(3.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Theme switcher badge top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onToggleTheme() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Tema: $themeName",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )
        }

        if (board.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Tablero de Domino",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Selecciona una ficha de tu mano para colocarla",
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp
                )
            }
        } else {
            // Box to overlay the floating indicators on top of the scrolling row
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 40.dp), // Give space so tiles don't hide completely behind indicators at the absolute edges
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Chain Rendering
                    board.forEach { tile ->
                        val isHorizontalTile = !tile.isDouble()

                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn()
                        ) {
                            DominoTileView(
                                tile = tile,
                                isHorizontal = isHorizontalTile
                            )
                        }
                    }
                }

                // Left Floating Indicator
                val leftValue = board.firstOrNull()?.left?.toString() ?: ""
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                        .width(36.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                        .clickable { coroutineScope.launch { scrollState.animateScrollTo(0) } },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "◀", color = Color(0xFFFFD700), fontSize = 10.sp)
                        Text(text = leftValue, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }

                // Right Floating Indicator
                val rightValue = board.lastOrNull()?.right?.toString() ?: ""
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .width(36.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                        .clickable { coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) } },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "▶", color = Color(0xFFFFD700), fontSize = 10.sp)
                        Text(text = rightValue, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}
