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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
// import androidx.compose.ui.zIndex.zIndex
import com.example.godsmino.util.SoundManager

@Composable
fun GameScreen(
    roomId: String = "room_godsmino_1",
    isHost: Boolean = true,
    viewModel: GameViewModel = viewModel(),
    onBackToLobby: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val session = uiState.session
    val haptic = LocalHapticFeedback.current
    var showQuitDialog by remember { mutableStateOf(false) }

    // Always keep Local Player fixed at the bottom, and opponent fixed at the top
    val localPlayerId = viewModel.localPlayerId
    val player1 = session.players.firstOrNull { it.id == localPlayerId } ?: session.players.firstOrNull()
    val opponent = session.players.firstOrNull { it.id != localPlayerId } ?: session.players.getOrNull(1)
    val isMyTurn = session.currentTurnPlayerId == localPlayerId

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

    // Quit Match Confirmation Dialog
    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            containerColor = Color(0xFF1C1C1C),
            title = { Text("Abandonar Partida", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold) },
            text = { Text("¿Seguro que deseas abandonar la partida? Esto concedera la victoria al oponente.", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitDialog = false
                        viewModel.forfeitGame()
                        onBackToLobby()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Abandonar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            }
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
                .systemBarsPadding()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP SECTION: Opponent Info & Room ID + Abandon Button
            Column(
                modifier = Modifier.fillMaxWidth(), 
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Bar with Room info and Abandon button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (session.roomId == "room_offline_bot") "Local vs Bot" else "Sala: ${session.roomId}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.LightGray
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Ronda: ${session.roundNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37)
                        )
                        IconButton(
                            onClick = { showQuitDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Abandonar", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (opponent != null) {
                    OpponentHeaderView(opponent = opponent, isTurn = !isMyTurn)
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

            // 2. CENTER SECTION: Board 
            // Eliminado el Sidebar de poderes lateral para dar más espacio horizontal al tablero.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                GameBoardView(
                    board = session.board,
                    themeIndex = uiState.boardThemeIndex,
                    modifier = Modifier.fillMaxSize(),
                    onToggleTheme = { viewModel.toggleBoardTheme() }
                )
            }

            // 3. BOTTOM SECTION: Powers, Player Hand & Action Chips
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
                                // We exit the game and return to the lobby instead of trying to restart the flow
                                onBackToLobby()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Volver al Menú Principal", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (player1 != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                    // .zIndex(10f)
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
                            text = if (isMyTurn) "Tu Turno: Toca una ficha y elige Lado (◀ o ▶)" else "Esperando jugada del oponente...",
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

                    // Powers Header (Horizontal)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        player1.powerCounts.forEach { (power, count) ->
                            val isSelected = uiState.selectedPower == power
                            val color = if (count > 0) (if (isSelected) Color(0xFF4CAF50) else Color(0xFFD4AF37)) else Color.DarkGray
                            IconButton(
                                onClick = {
                                    if (count > 0) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.requestUsePower(power)
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF222222), CircleShape)
                                    .border(1.dp, color, CircleShape)
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Icon(power.icon, contentDescription = power.displayName, tint = color, modifier = Modifier.size(18.dp).align(Alignment.Center))
                                    if (count > 0) {
                                        Text(text = count.toString(), color = Color.White, fontSize = 9.sp, modifier = Modifier.offset(x = 6.dp, y = 6.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Draggable Player Hand
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
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
                                    isClickable = isMyTurn,
                                    onClick = {
                                        if (isMyTurn) {
                                            SoundManager.playClickSound()
                                            viewModel.selectTile(tile)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // 4 Direction Quick Buttons (Replaced with icon chips)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (session.boneyardSize > 0) {
                            Button(
                                onClick = {
                                    if (isMyTurn) {
                                        SoundManager.playDrawTile()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.drawTile()
                                    }
                                },
                                enabled = isMyTurn,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A880), disabledContainerColor = Color.DarkGray),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "Robar Ficha", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (isMyTurn) {
                                        SoundManager.playClickSound()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.passTurn()
                                    }
                                },
                                enabled = isMyTurn,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C), disabledContainerColor = Color.DarkGray),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Pasar Turno", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // 2 Direction Icon Buttons (Left and Right ONLY)
                        val buttonColor = Color(0xFF263238)
                        val highlightColor = Color(0xFF1B5E20)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            IconButton(
                                onClick = { if (isMyTurn) { SoundManager.playTileClack(); viewModel.playTile(BoardDirection.LEFT) } },
                                enabled = isMyTurn && uiState.selectedTile != null,
                                modifier = Modifier.background(if (uiState.selectedTile != null) highlightColor else buttonColor, RoundedCornerShape(8.dp)).size(48.dp)
                            ) {
                                Text("◀", color = Color.White, fontSize = 24.sp)
                            }
                            IconButton(
                                onClick = { if (isMyTurn) { SoundManager.playTileClack(); viewModel.playTile(BoardDirection.RIGHT) } },
                                enabled = isMyTurn && uiState.selectedTile != null,
                                modifier = Modifier.background(if (uiState.selectedTile != null) highlightColor else buttonColor, RoundedCornerShape(8.dp)).size(48.dp)
                            ) {
                                Text("▶", color = Color.White, fontSize = 24.sp)
                            }
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
