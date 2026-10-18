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
                    text = "Arrastra o selecciona una ficha de tu mano para colocarla",
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Drop Target Left Zone
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(84.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFD700).copy(alpha = 0.25f))
                        .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Izq", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Chain Rendering
                board.forEach { tile ->
                    val isHorizontalTile = !tile.isDouble()

                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn()
                    ) {
                        DominoTileView(
                            tile = tile,
                            isHorizontal = isHorizontalTile,
                            isDraggable = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Drop Target Right Zone
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(84.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFD700).copy(alpha = 0.25f))
                        .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Der", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
