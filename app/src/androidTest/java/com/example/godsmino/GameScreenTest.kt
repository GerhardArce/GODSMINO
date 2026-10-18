package com.example.godsmino

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.godsmino.ui.screen.GameScreen
import com.example.godsmino.ui.screen.LobbyScreen
import com.example.godsmino.ui.theme.GODSMINOTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLobbyScreenDisplaysOptions() {
        composeTestRule.setContent {
            GODSMINOTheme {
                LobbyScreen(onStartGame = { _, _ -> })
            }
        }

        composeTestRule.onNodeWithText("GODSMINO").assertExists()
        composeTestRule.onNodeWithText("Partida Rapida vs Bot").assertExists()
        composeTestRule.onNodeWithText("Crear Sala Online").assertExists()
        composeTestRule.onNodeWithText("Unirse a Sala Online").assertExists()
    }

    @Test
    fun testGameScreenLaunchesSuccessfully() {
        composeTestRule.setContent {
            GODSMINOTheme {
                GameScreen(roomId = "test_room", isHost = true)
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Sala: test_room").assertExists()
        composeTestRule.onNodeWithText("Robar Pozo").assertExists()
    }
}
