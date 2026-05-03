package com.antigravity.battleship.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GameViewModelTest {

    private lateinit var viewModel: GameViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = GameViewModel(app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is MAIN_MENU`() {
        assertEquals(GamePhase.MAIN_MENU, viewModel.currentPhase)
    }

    @Test
    fun `startSinglePlayer transitions to PLACEMENT phase`() {
        viewModel.startSinglePlayer()
        assertEquals(GamePhase.PLACEMENT, viewModel.currentPhase)
        assertTrue(viewModel.isSinglePlayer)
    }

    @Test
    fun `startSinglePlayer places all opponent ships`() {
        viewModel.startSinglePlayer()
        viewModel.opponentShips.forEach { ship ->
            assertTrue("${ship.name} should have coordinates", ship.coordinates.isNotEmpty())
            assertEquals(ship.size, ship.coordinates.size)
        }
    }

    @Test
    fun `placePlayerShip horizontal places ship correctly`() {
        viewModel.startSinglePlayer()
        viewModel.isHorizontal = true
        viewModel.placePlayerShip(0, 0)
        val carrier = viewModel.playerShips.first { it.name == "Carrier" }
        assertEquals(5, carrier.coordinates.size)
        // Horizontal: x increments, y stays
        assertEquals(0 to 0, carrier.coordinates[0])
        assertEquals(4 to 0, carrier.coordinates[4])
    }

    @Test
    fun `placePlayerShip vertical places ship correctly`() {
        viewModel.startSinglePlayer()
        viewModel.isHorizontal = false
        viewModel.placePlayerShip(0, 0)
        val carrier = viewModel.playerShips.first { it.name == "Carrier" }
        assertEquals(5, carrier.coordinates.size)
        // Vertical: x stays, y increments
        assertEquals(0 to 0, carrier.coordinates[0])
        assertEquals(0 to 4, carrier.coordinates[4])
    }

    @Test
    fun `placePlayerShip rejects out of bounds horizontal`() {
        viewModel.startSinglePlayer()
        viewModel.isHorizontal = true
        viewModel.placePlayerShip(8, 0) // Carrier size 5, x=8 would go to x=12
        val carrier = viewModel.playerShips.first { it.name == "Carrier" }
        assertTrue(carrier.coordinates.isEmpty())
    }

    @Test
    fun `placePlayerShip rejects out of bounds vertical`() {
        viewModel.startSinglePlayer()
        viewModel.isHorizontal = false
        viewModel.placePlayerShip(0, 8) // Carrier size 5, y=8 would go to y=12
        val carrier = viewModel.playerShips.first { it.name == "Carrier" }
        assertTrue(carrier.coordinates.isEmpty())
    }

    @Test
    fun `toggleRotation flips orientation`() {
        assertTrue(viewModel.isHorizontal)
        viewModel.toggleRotation()
        assertFalse(viewModel.isHorizontal)
        viewModel.toggleRotation()
        assertTrue(viewModel.isHorizontal)
    }

    @Test
    fun `onReady transitions to COMBAT`() {
        viewModel.startSinglePlayer()
        placeAllPlayerShips()
        viewModel.onReady()
        assertEquals(GamePhase.COMBAT, viewModel.currentPhase)
        assertTrue(viewModel.isPlayerTurn)
    }

    @Test
    fun `playerAttack on empty cell is a MISS`() = runTest {
        viewModel.startSinglePlayer()
        placeAllPlayerShips()
        viewModel.onReady()

        val (x, y) = findEmptyCell(viewModel.opponentGrid)
        viewModel.playerAttack(x, y)
        advanceUntilIdle()

        assertEquals(CellState.MISS, viewModel.opponentGrid[x][y])
        assertEquals(1, viewModel.missilesFired)
    }

    @Test
    fun `playerAttack on ship cell destroys ship`() = runTest {
        viewModel.startSinglePlayer()
        placeAllPlayerShips()
        viewModel.onReady()

        val ship = viewModel.opponentShips.first()
        ship.coordinates.forEach { (x, y) ->
            viewModel.isPlayerTurn = true
            viewModel.activeMissile = null
            viewModel.playerAttack(x, y)
            advanceUntilIdle()
        }

        assertTrue(ship.isDestroyed)
    }

    @Test
    fun `game over when all opponent ships destroyed`() = runTest {
        viewModel.startSinglePlayer()
        placeAllPlayerShips()
        viewModel.onReady()

        viewModel.opponentShips.forEach { ship ->
            ship.coordinates.forEach { (x, y) ->
                viewModel.isPlayerTurn = true
                viewModel.activeMissile = null
                viewModel.playerAttack(x, y)
                advanceUntilIdle()
            }
        }

        assertEquals(GamePhase.GAME_OVER, viewModel.currentPhase)
    }

    @Test
    fun `restart resets all state`() {
        viewModel.startSinglePlayer()
        viewModel.restart()
        assertEquals(GamePhase.MAIN_MENU, viewModel.currentPhase)
        assertEquals(0, viewModel.missilesFired)
        assertTrue(viewModel.playerShips.all { it.coordinates.isEmpty() })
    }

    @Test
    fun `cannot attack same cell twice`() = runTest {
        viewModel.startSinglePlayer()
        placeAllPlayerShips()
        viewModel.onReady()

        val (x, y) = findEmptyCell(viewModel.opponentGrid)
        viewModel.playerAttack(x, y)
        advanceUntilIdle()

        val missilesBefore = viewModel.missilesFired
        viewModel.isPlayerTurn = true
        viewModel.playerAttack(x, y)
        advanceUntilIdle()

        assertEquals(missilesBefore, viewModel.missilesFired)
    }

    // --- Helpers ---

    private fun placeAllPlayerShips() {
        viewModel.isHorizontal = true
        viewModel.placePlayerShip(0, 0) // Carrier (5)
        viewModel.placePlayerShip(0, 1) // Battleship (4)
        viewModel.placePlayerShip(0, 2) // Destroyer (3)
        viewModel.placePlayerShip(0, 3) // Submarine (3)
        viewModel.placePlayerShip(0, 4) // Patrol Boat (2)
    }

    private fun findEmptyCell(grid: Array<Array<CellState>>): Pair<Int, Int> {
        for (x in 0 until 10) {
            for (y in 0 until 10) {
                if (grid[x][y] == CellState.EMPTY) return x to y
            }
        }
        throw IllegalStateException("No empty cell found")
    }
}
