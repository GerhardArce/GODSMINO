package com.example.godsmino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.godsmino.data.repository.AuthRepository
import com.example.godsmino.ui.screen.GameScreen
import com.example.godsmino.ui.screen.LobbyScreen
import com.example.godsmino.ui.screen.LoginScreen
import com.example.godsmino.ui.screen.StatisticsScreen
import com.example.godsmino.ui.theme.GODSMINOTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GODSMINOTheme {
                val context = LocalContext.current
                val authRepository = remember { AuthRepository(context) }
                val currentUser by authRepository.currentUser.collectAsState()

                var currentScreen by remember { mutableStateOf("lobby") } // "login", "lobby", "game", "stats"
                var currentRoomId by remember { mutableStateOf("room_godsmino_1") }
                var isHostUser by remember { mutableStateOf(true) }

                if (currentUser == null) {
                    LoginScreen(
                        authRepository = authRepository,
                        onLoginSuccess = {
                            currentScreen = "lobby"
                        }
                    )
                } else {
                    when (currentScreen) {
                        "game" -> GameScreen(
                            roomId = currentRoomId,
                            isHost = isHostUser,
                            onBackToLobby = { currentScreen = "lobby" }
                        )
                        "stats" -> StatisticsScreen(onBack = { currentScreen = "lobby" })
                        else -> LobbyScreen(
                            currentUser = currentUser,
                            onStartGame = { roomId, isHost ->
                                currentRoomId = roomId
                                isHostUser = isHost
                                currentScreen = "game"
                            },
                            onOpenStats = { currentScreen = "stats" },
                            onLogout = {
                                authRepository.logout()
                                currentScreen = "login"
                            }
                        )
                    }
                }
            }
        }
    }
}
