package com.example.godsmino

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.godsmino.data.model.GameStatus
import com.example.godsmino.data.model.User
import com.example.godsmino.data.repository.AuthRepository
import com.example.godsmino.ui.components.CoinTossDialog
import com.example.godsmino.ui.screen.GameScreen
import com.example.godsmino.ui.screen.LobbyScreen
import com.example.godsmino.ui.screen.StatisticsScreen
import com.example.godsmino.ui.screen.WaitingRoomScreen
import com.example.godsmino.ui.theme.GODSMINOTheme
import com.example.godsmino.ui.viewmodel.GameViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GODSMINOTheme {
                val context = LocalContext.current
                val authRepository = remember { AuthRepository(context) }
                val currentUser by authRepository.currentUser.collectAsState()
                val gameViewModel: GameViewModel = viewModel()

                var currentScreen by remember { mutableStateOf("lobby") } // "lobby", "waiting_room", "game", "stats"
                var currentRoomId by remember { mutableStateOf("room_godsmino_1") }
                var isHostUser by remember { mutableStateOf(true) }

                val uiState by gameViewModel.uiState.collectAsState()

                val firebaseUser = try { FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null }
                val effectiveUser = currentUser ?: firebaseUser?.let {
                    User(uid = it.uid, email = it.email ?: "", displayName = it.displayName ?: it.email?.substringBefore("@") ?: "Jugador")
                }

                if (effectiveUser == null) {
                    val intent = Intent(this, AuthActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentScreen) {
                            "waiting_room" -> {
                                LaunchedEffect(uiState.session.status) {
                                    if (uiState.session.status == GameStatus.COIN_TOSS || uiState.session.status == GameStatus.PLAYING) {
                                        currentScreen = "game"
                                    }
                                }
                                WaitingRoomScreen(
                                    roomId = currentRoomId,
                                    isHost = isHostUser,
                                    session = uiState.session,
                                    onStartGame = {
                                        gameViewModel.startGameSession()
                                    },
                                    onLeaveRoom = {
                                        currentScreen = "lobby"
                                    }
                                )
                            }
                            "game" -> GameScreen(
                                roomId = currentRoomId,
                                isHost = isHostUser,
                                viewModel = gameViewModel,
                                onBackToLobby = { currentScreen = "lobby" }
                            )
                            "stats" -> StatisticsScreen(onBack = { currentScreen = "lobby" })
                            else -> LobbyScreen(
                                currentUser = effectiveUser,
                                onStartGame = { roomId, isHost ->
                                    currentRoomId = roomId
                                    isHostUser = isHost
                                    val realName = effectiveUser.displayName.takeIf { it.isNotBlank() && it != "Jugador" && it != "Jugador 1" && it != "Anfitrión" }
                                        ?: effectiveUser.email.substringBefore("@")
                                        ?: "Jugador"
                                    gameViewModel.startRoom(roomId, isHost, realName)
                                    if (roomId == "room_offline_bot") {
                                        currentScreen = "game"
                                    } else {
                                        currentScreen = "waiting_room"
                                    }
                                },
                                onOpenStats = { currentScreen = "stats" },
                                onLogout = {
                                    authRepository.logout()
                                    val intent = Intent(context, AuthActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            )
                        }

                        // Global Coin Toss Dialog Overlay covering the screen whenever COIN_TOSS is active
                        if (uiState.session.status == GameStatus.COIN_TOSS) {
                            CoinTossDialog(
                                onTossFinished = { winnerId ->
                                    if (isHostUser) {
                                        gameViewModel.finishCoinToss(winnerId)
                                    }
                                },
                                session = uiState.session
                            )
                        }
                    }
                }
            }
        }
    }
}
