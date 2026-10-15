package com.example.godsmino.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.godsmino.data.model.BoardDirection
import com.example.godsmino.data.model.GameStatus
import com.example.godsmino.data.model.Player
import com.example.godsmino.ui.components.*
import com.example.godsmino.ui.viewmodel.GameViewModel
import com.example.godsmino.util.SoundManager
import kotlin.math.abs

@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val session = uiState.session
    val haptic = LocalHapticFeedback.current

    val currentUser = session.players.firstOrNull { it.id == session.currentTurnPlayerId } ?: session.players.firstOrNull()
    val opponent = session.players.firstOrNull { it.id != currentUser?.id } ?: session.players.getOrNull(1)

    LaunchedEffect(session.status) {
        if (session.status == GameStatus.FINISHED) {
            SoundManager.playVictorySound()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D2818), Color(0xFF04150C), Color(0xFF010A05))
                )
            )
    ) {
        ConfettiOverlay(visible = session.status == GameStatus.FINISHED)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP SECTION: Opponent Info
            if (opponent != null) {
                OpponentHeaderView(opponent = opponent, isTurn = session.currentTurnPlayerId == opponent.id)
            }

            // Random Event Banner
            RandomEventBanner(event = session.activeEvent)

            // 2. CENTER SECTION: Board + Powers Sidebar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    GameBoardView(
                        board = session.board,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (currentUser != null) {
                    Column(
                        modifier = Modifier
                            .width(85.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF121212).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Poderes",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        PowerControlsVerticalView(
                            powers = currentUser.powerCounts,
                            selectedPower = uiState.selectedPower,
                            onPowerSelected = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.usePower(it)
                            }
                        )
                    }
                }
            }

            // 3. BOTTOM SECTION: Draggable Player Hand & 4 Direction Action Chips
            if (session.status == GameStatus.FINISHED) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉 ¡DOMINÓ! ¡Partida Finalizada!",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                SoundManager.playClickSound()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.restartGame()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text(text = "🔄 Jugar Nueva Partida", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (currentUser != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF000000).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🖐️ Arrastra hacia Arriba / Abajo / Izq / Der para colocar",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Pozo: 📦 ${session.boneyardSize}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFD700)
                        )
                    }

                    // Draggable Player Hand
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(currentUser.tiles) { tile ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically { it / 2 }
                            ) {
                                DominoTileView(
                                    tile = tile,
                                    isSelected = uiState.selectedTile == tile,
                                    isDraggable = true,
                                    onClick = {
                                        SoundManager.playClickSound()
                                        viewModel.selectTile(tile)
                                    },
                                    onDragEnd = { offset ->
                                        // Smart 4-Direction Drag Threshold Detection
                                        viewModel.selectTile(tile)
                                        val direction = when {
                                            offset.y < -120f && abs(offset.y) > abs(offset.x) -> BoardDirection.UP
                                            offset.y > 60f && abs(offset.y) > abs(offset.x) -> BoardDirection.DOWN
                                            offset.x < -60f -> BoardDirection.LEFT
                                            offset.x > 60f -> BoardDirection.RIGHT
                                            else -> BoardDirection.UP
                                        }
                                        SoundManager.playTileClack()
                                        viewModel.playTile(direction)
                                    }
                                )
                            }
                        }
                    }

                    // 4 Direction Quick Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                SoundManager.playDrawTile()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.drawTile()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "Robar Pozo", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                SoundManager.playTileClack()
                                viewModel.playTile(BoardDirection.UP)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(text = "⬆️ Arriba", fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                SoundManager.playTileClack()
                                viewModel.playTile(BoardDirection.DOWN)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(text = "⬇️ Abajo", fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                SoundManager.playTileClack()
                                viewModel.playTile(BoardDirection.LEFT)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(text = "◄ Izq", fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                SoundManager.playTileClack()
                                viewModel.playTile(BoardDirection.RIGHT)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(text = "Der ►", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OpponentHeaderView(opponent: Player, isTurn: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isTurn) Color(0xFF2E7D32).copy(alpha = 0.9f) else Color(0xFF1E1E1E).copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFFFA000), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = opponent.name.take(1), fontWeight = FontWeight.Bold, color = Color.White)
            }
            Column {
                Text(
                    text = opponent.name + if (isTurn) " ⏳ (Pensando...)" else "",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "Puntaje: ${opponent.score}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(minOf(opponent.tiles.size, 7)) {
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(24.dp)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
                )
            }
            if (opponent.tiles.size > 7) {
                Text(text = "+${opponent.tiles.size - 7}", color = Color.White, fontSize = 10.sp)
            }
        }
    }
}
