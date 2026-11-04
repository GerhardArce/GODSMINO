package com.example.godsmino

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.godsmino.data.model.GameStatus
import com.example.godsmino.data.repository.AuthRepository
import com.example.godsmino.ui.screen.GameScreen
import com.example.godsmino.ui.screen.LobbyScreen
import com.example.godsmino.ui.screen.StatisticsScreen
import com.example.godsmino.ui.screen.WaitingRoomScreen
import com.example.godsmino.ui.theme.GODSMINOTheme
import com.example.godsmino.ui.viewmodel.GameViewModel

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

                if (currentUser == null) {
                    val intent = Intent(this, AuthActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    when (currentScreen) {
                        "waiting_room" -> {
                            LaunchedEffect(uiState.session.status) {
                                if (uiState.session.status == GameStatus.PLAYING) {
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
                            currentUser = currentUser,
                            onStartGame = { roomId, isHost ->
                                currentRoomId = roomId
                                isHostUser = isHost
                                gameViewModel.startRoom(roomId, isHost, currentUser?.displayName ?: "Jugador")
                                if (roomId == "room_offline_bot") {
                                    currentScreen = "game"
                                } else {
                                    currentScreen = "waiting_room"
                                }
                            },
                            onOpenStats = { currentScreen = "stats" },
                            onLogout = {
                                authRepository.logout()
                                val intent = Intent(this, AuthActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}
