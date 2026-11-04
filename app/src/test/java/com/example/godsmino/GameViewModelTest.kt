package com.example.godsmino

import com.example.godsmino.data.model.GameStatus
import com.example.godsmino.data.model.PowerType
import com.example.godsmino.ui.viewmodel.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createActiveGameViewModel(): GameViewModel {
        val viewModel = GameViewModel()
        viewModel.startRoom("room_offline_bot", true)
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun testWaitingRoomAndStartGame() = runTest {
        val viewModel = GameViewModel()
        viewModel.startRoom("room_test_123", true, "HostPlayer")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(GameStatus.WAITING, state.session.status)
        assertEquals(1, state.session.players.size)

        // Guest joins
        viewModel.joinOnlineRoom("GuestPlayer")
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(2, state.session.players.size)

        // Host starts game
        viewModel.startGameSession()
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(GameStatus.PLAYING, state.session.status)
        assertEquals(7, state.session.players[0].tiles.size)
    }

    @Test
    fun testViewModelInitialization() = runTest {
        val viewModel = createActiveGameViewModel()
        val state = viewModel.uiState.value

        assertNotNull(state.session)
        assertEquals(2, state.session.players.size)
        assertEquals(7, state.session.players[0].tiles.size)
        assertEquals(7, state.session.players[1].tiles.size)
    }

    @Test
    fun testTileSelection() = runTest {
        val viewModel = createActiveGameViewModel()
        val firstTile = viewModel.uiState.value.session.players[0].tiles[0]

        viewModel.selectTile(firstTile)
        assertEquals(firstTile, viewModel.uiState.value.selectedTile)

        viewModel.selectTile(firstTile) // toggle off
        assertNull(viewModel.uiState.value.selectedTile)
    }

    @Test
    fun testToggleBoardTheme() = runTest {
        val viewModel = createActiveGameViewModel()
        val initialTheme = viewModel.uiState.value.boardThemeIndex

        viewModel.toggleBoardTheme()
        assertEquals((initialTheme + 1) % 4, viewModel.uiState.value.boardThemeIndex)
    }

    @Test
    fun testRequestPowerDialog() = runTest {
        val viewModel = createActiveGameViewModel()

        viewModel.requestUsePower(PowerType.WILDCARD)
        assertEquals(PowerType.WILDCARD, viewModel.uiState.value.pendingPowerDialog)

        viewModel.dismissPowerDialog()
        assertNull(viewModel.uiState.value.pendingPowerDialog)
    }

    @Test
    fun testDrawTile() = runTest {
        val viewModel = createActiveGameViewModel()

        // Drawing when player has valid moves should result in an error per domino rules
        viewModel.drawTile()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testForfeitGame() = runTest {
        val viewModel = createActiveGameViewModel()

        viewModel.forfeitGame()
        testDispatcher.scheduler.advanceUntilIdle()

        val status = viewModel.uiState.value.session.status
        assertEquals(GameStatus.FINISHED, status)
    }

    @Test
    fun testRestartGame() = runTest {
        val viewModel = createActiveGameViewModel()

        viewModel.forfeitGame()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.restartGame()
        testDispatcher.scheduler.advanceUntilIdle()

        val status = viewModel.uiState.value.session.status
        assertEquals(GameStatus.PLAYING, status)
    }
}
