package com.example.godsmino.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
    roomId: String = "room_godsmino_1",
    isHost: Boolean = true,
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val session = uiState.session
    val haptic = LocalHapticFeedback.current

    // Always keep Player 1 (You) fixed at the bottom, and opponent fixed at the top
    val player1 = session.players.firstOrNull { it.id == "p1" } ?: session.players.firstOrNull()
    val opponent = session.players.firstOrNull { it.id != "p1" } ?: session.players.getOrNull(1)
    val isMyTurn = session.currentTurnPlayerId == "p1"

    LaunchedEffect(roomId) {
        viewModel.startRoom(roomId, isHost)
    }

    LaunchedEffect(session.status) {
        if (session.status == GameStatus.FINISHED) {
            SoundManager.playVictorySound()
        }
    }

    // Power Confirmation Dialog
    if (uiState.pendingPowerDialog != null && player1 != null) {
        PowerConfirmationDialog(
            powerType = uiState.pendingPowerDialog!!,
            playerTiles = player1.tiles,
            selectedSwapTile = uiState.selectedTileForSwap,
            onSelectSwapTile = { viewModel.selectSwapTile(it) },
            onConfirm = { viewModel.confirmUsePower() },
            onDismiss = { viewModel.dismissPowerDialog() }
        )
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
            // 1. TOP SECTION: Opponent Info & Room ID
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (opponent != null) {
                    OpponentHeaderView(opponent = opponent, isTurn = !isMyTurn)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sala: ${session.roomId}",
                        fontSize = 10.sp,
                        color = Color.LightGray.copy(alpha = 0.7f)
                    )
                    Text(
                        text = if (isMyTurn) "Tu Turno (Juega una ficha)" else "Turno del Oponente (Pensando...)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMyTurn) Color(0xFFD4AF37) else Color.LightGray
                    )
                }
            }

            // Error Message Banner if any
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (uiState.errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFB71C1C), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
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
                        themeIndex = uiState.boardThemeIndex,
                        modifier = Modifier.fillMaxSize(),
                        onToggleTheme = { viewModel.toggleBoardTheme() }
                    )
                }

                if (player1 != null) {
                    Column(
                        modifier = Modifier
                            .width(85.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF141414).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Poderes",
                            color = Color(0xFFD4AF37),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        PowerControlsVerticalView(
                            powers = player1.powerCounts,
                            selectedPower = uiState.selectedPower,
                            onPowerSelected = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.requestUsePower(it)
                            }
                        )
                    }
                }
            }

            // 3. BOTTOM SECTION: Draggable Player Hand & 4 Direction Action Chips
            if (session.status == GameStatus.FINISHED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141414))
                        .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "¡Partida Finalizada!",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFD4AF37)
                        )
                        Button(
                            onClick = {
                                SoundManager.playClickSound()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.restartGame()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Jugar Nueva Partida", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (player1 != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141414).copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isMyTurn) "Tu Turno: Arrastra o usa botones" else "Esperando jugada del oponente...",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (isMyTurn) Color(0xFFD4AF37) else Color.LightGray,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Pozo: ${session.boneyardSize}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFD4AF37)
                        )
                    }

                    // Draggable Player Hand
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(player1.tiles) { tile ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically { it / 2 }
                            ) {
                                DominoTileView(
                                    tile = tile,
                                    isSelected = uiState.selectedTile == tile,
                                    isDraggable = isMyTurn,
                                    onClick = {
                                        if (isMyTurn) {
                                            SoundManager.playClickSound()
                                            viewModel.selectTile(tile)
                                        }
                                    },
                                    onDragEnd = { offset ->
                                        if (isMyTurn) {
                                            val direction = when {
                                                offset.y < -70f && abs(offset.y) > abs(offset.x) -> BoardDirection.UP
                                                offset.y > 70f && abs(offset.y) > abs(offset.x) -> BoardDirection.DOWN
                                                offset.x < -60f -> BoardDirection.LEFT
                                                offset.x > 60f -> BoardDirection.RIGHT
                                                else -> BoardDirection.RIGHT
                                            }
                                            SoundManager.playTileClack()
                                            viewModel.playSpecificTile(tile, direction)
                                        }
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
                                if (isMyTurn) {
                                    SoundManager.playDrawTile()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.drawTile()
                                }
                            },
                            enabled = isMyTurn,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A880)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Robar Pozo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        Button(
                            onClick = {
                                if (isMyTurn) {
                                    SoundManager.playTileClack()
                                    viewModel.playTile(BoardDirection.UP)
                                }
                            },
                            enabled = isMyTurn,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Arriba", fontSize = 10.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                if (isMyTurn) {
                                    SoundManager.playTileClack()
                                    viewModel.playTile(BoardDirection.DOWN)
                                }
                            },
                            enabled = isMyTurn,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Abajo", fontSize = 10.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                if (isMyTurn) {
                                    SoundManager.playTileClack()
                                    viewModel.playTile(BoardDirection.LEFT)
                                }
                            },
                            enabled = isMyTurn,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Izquierda", fontSize = 10.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                if (isMyTurn) {
                                    SoundManager.playTileClack()
                                    viewModel.playTile(BoardDirection.RIGHT)
                                }
                            },
                            enabled = isMyTurn,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Derecha", fontSize = 10.sp, color = Color.White)
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
                color = if (isTurn) Color(0xFF1B5E20).copy(alpha = 0.9f) else Color(0xFF141414).copy(alpha = 0.9f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(1.dp, if (isTurn) Color(0xFFD4AF37) else Color(0xFF2C2C2C), RoundedCornerShape(10.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFF333333), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = opponent.name.take(1), fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
            }
            Column {
                Text(
                    text = opponent.name + if (isTurn) " (Pensando...)" else "",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD4AF37),
                    fontSize = 13.sp
                )
                Text(
                    text = "Puntaje: ${opponent.score}",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
