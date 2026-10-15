package com.example.godsmino.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    modifier: Modifier = Modifier,
    onDropLeft: () -> Unit = {},
    onDropRight: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF09330C))
                )
            )
            .border(3.dp, Color(0xFF388E3C), RoundedCornerShape(20.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (board.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🎲 Tablero de Dominó Real",
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
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Drop Target Left Zone
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFD700).copy(alpha = 0.25f))
                        .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "◄", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Serpentine Chain Rendering
                board.forEachIndexed { index, tile ->
                    val isDouble = tile.isDouble()
                    // Doubles are crosswise (perpendicular / vertical)
                    val isHorizontalTile = !isDouble

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
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFD700).copy(alpha = 0.25f))
                        .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "►", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        }
    }
}
