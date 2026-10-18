package com.example.godsmino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.godsmino.ui.screen.GameScreen
import com.example.godsmino.ui.screen.LobbyScreen
import com.example.godsmino.ui.screen.StatisticsScreen
import com.example.godsmino.ui.theme.GODSMINOTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GODSMINOTheme {
                var currentScreen by remember { mutableStateOf("lobby") } // "lobby", "game", "stats"
                var currentRoomId by remember { mutableStateOf("room_godsmino_1") }
                var isHostUser by remember { mutableStateOf(true) }

                when (currentScreen) {
                    "game" -> GameScreen(roomId = currentRoomId, isHost = isHostUser)
                    "stats" -> StatisticsScreen(onBack = { currentScreen = "lobby" })
                    else -> LobbyScreen(
                        onStartGame = { roomId, isHost ->
                            currentRoomId = roomId
                            isHostUser = isHost
                            currentScreen = "game"
                        },
                        onOpenStats = { currentScreen = "stats" }
                    )
                }
            }
        }
    }
}
