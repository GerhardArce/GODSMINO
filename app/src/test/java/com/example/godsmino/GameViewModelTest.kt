package com.example.godsmino

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

    @Test
    fun testViewModelInitialization() = runTest {
        val viewModel = GameViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value

        assertNotNull(state.session)
        assertEquals(2, state.session.players.size)
        assertEquals(7, state.session.players[0].tiles.size)
        assertEquals(7, state.session.players[1].tiles.size)
    }

    @Test
    fun testTileSelection() = runTest {
        val viewModel = GameViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val firstTile = viewModel.uiState.value.session.players[0].tiles[0]

        viewModel.selectTile(firstTile)
        assertEquals(firstTile, viewModel.uiState.value.selectedTile)

        viewModel.selectTile(firstTile)
        assertNull(viewModel.uiState.value.selectedTile)
    }
}
