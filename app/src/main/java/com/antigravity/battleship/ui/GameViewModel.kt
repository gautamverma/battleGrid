package com.antigravity.battleship.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

enum class GamePhase { MAIN_MENU, PLACEMENT, COMBAT, GAME_OVER, SCOREBOARD, SETTINGS }
enum class CellState { EMPTY, SHIP, MISS, HIT }

data class Ship(
    val id: Int,
    val name: String,
    val size: Int,
    var coordinates: List<Pair<Int, Int>> = emptyList(),
    var isDestroyed: Boolean = false
)

data class MissileStrike(
    val startX: Float,
    val startY: Float,
    val targetX: Int,
    val targetY: Int,
    val isOpponentGrid: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ScoreRecord(
    val missilesUsed: Int,
    val date: Long = System.currentTimeMillis()
)

data class BattleLogEntry(
    val text: String,
    val time: String,
    val isCritical: Boolean = false
)

class GameViewModel : ViewModel() {
    var currentPhase by mutableStateOf(GamePhase.MAIN_MENU)
    var isSinglePlayer by mutableStateOf(false)
    var statusText by mutableStateOf("Welcome to Grid Strike")
    
    var playerGrid by mutableStateOf(Array(10) { Array(10) { CellState.EMPTY } })
    var opponentGrid by mutableStateOf(Array(10) { Array(10) { CellState.EMPTY } })
    
    var playerShips = listOf(
        Ship(1, "Carrier", 5),
        Ship(2, "Battleship", 4),
        Ship(3, "Destroyer", 3),
        Ship(4, "Submarine", 3),
        Ship(5, "Patrol Boat", 2)
    )
    
    var opponentShips = listOf(
        Ship(10, "Carrier", 5),
        Ship(20, "Battleship", 4),
        Ship(30, "Destroyer", 3),
        Ship(40, "Submarine", 3),
        Ship(50, "Patrol Boat", 2)
    )

    var isPlayerTurn by mutableStateOf(true)
    var missilesFired by mutableStateOf(0)
    
    var screenShakeTrigger by mutableStateOf(0)
    var activeMissile by mutableStateOf<MissileStrike?>(null)
    
    var soundManager: SoundManager? = null
    
    val battleLog = mutableStateListOf<BattleLogEntry>()

    private val _highScores = mutableStateListOf<ScoreRecord>()
    val highScores: List<ScoreRecord> get() = _highScores.sortedBy { it.missilesUsed }

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun startSinglePlayer() {
        isSinglePlayer = true
        currentPhase = GamePhase.PLACEMENT
        statusText = "COMMANDER: DEPLOY FLEET"
        missilesFired = 0
        battleLog.clear()
        setupOpponentShips()
    }

    private fun setupOpponentShips() {
        opponentGrid = Array(10) { Array(10) { CellState.EMPTY } }
        opponentShips.forEach { ship ->
            ship.coordinates = emptyList()
            ship.isDestroyed = false
            var placed = false
            while (!placed) {
                val horizontal = Random.nextBoolean()
                val x = Random.nextInt(10)
                val y = Random.nextInt(10)
                if (canPlaceShip(opponentGrid, x, y, ship.size, horizontal)) {
                    val coords = mutableListOf<Pair<Int, Int>>()
                    for (i in 0 until ship.size) {
                        val cx = if (horizontal) x + i else x
                        val cy = if (horizontal) y else y + i
                        opponentGrid[cx][cy] = CellState.SHIP
                        coords.add(cx to cy)
                    }
                    ship.coordinates = coords
                    placed = true
                }
            }
        }
    }

    private fun canPlaceShip(grid: Array<Array<CellState>>, x: Int, y: Int, size: Int, horizontal: Boolean): Boolean {
        for (i in 0 until size) {
            val cx = if (horizontal) x + i else x
            val cy = if (horizontal) y else y + i
            if (cx >= 10 || cy >= 10 || grid[cx][cy] != CellState.EMPTY) return false
        }
        return true
    }

    fun placePlayerShip(x: Int, y: Int) {
        val ship = playerShips.firstOrNull { it.coordinates.isEmpty() } ?: return
        if (canPlaceShip(playerGrid, x, y, ship.size, true)) {
            val coords = mutableListOf<Pair<Int, Int>>()
            val newGrid = playerGrid.map { it.copyOf() }.toTypedArray()
            for (i in 0 until ship.size) {
                newGrid[x + i][y] = CellState.SHIP
                coords.add(x + i to y)
            }
            ship.coordinates = coords
            playerGrid = newGrid
            
            val nextShip = playerShips.firstOrNull { it.coordinates.isEmpty() }
            if (nextShip != null) {
                statusText = "DEPLOY: ${nextShip.name} [${nextShip.size}]"
            } else {
                statusText = "FLEET READY. ENGAGE?"
            }
        }
    }

    fun onReady() {
        currentPhase = GamePhase.COMBAT
        isPlayerTurn = true
        statusText = "AWAITING TARGET SELECTION"
        addToLog("System Online. Battle Engaged.")
    }

    private fun addToLog(text: String, isCritical: Boolean = false) {
        battleLog.add(0, BattleLogEntry(text, timeFormatter.format(Date()), isCritical))
    }

    fun playerAttack(x: Int, y: Int) {
        if (!isPlayerTurn || opponentGrid[x][y] == CellState.HIT || opponentGrid[x][y] == CellState.MISS || activeMissile != null) return
        
        viewModelScope.launch {
            isPlayerTurn = false
            missilesFired++
            soundManager?.playLaunch()
            addToLog("Strike Auth: [$x, $y]")
            activeMissile = MissileStrike(0.5f, 1.1f, x, y, true)
            delay(800)
            activeMissile = null
            
            val newGrid = opponentGrid.map { it.copyOf() }.toTypedArray()
            if (newGrid[x][y] == CellState.SHIP) {
                val hitShip = opponentShips.find { ship -> ship.coordinates.contains(x to y) }
                if (hitShip != null && !hitShip.isDestroyed) {
                    hitShip.isDestroyed = true
                    hitShip.coordinates.forEach { (cx, cy) ->
                        newGrid[cx][cy] = CellState.HIT
                    }
                    statusText = "TARGET NEUTRALIZED: ${hitShip.name}"
                    addToLog("${hitShip.name} DESTROYED", true)
                    soundManager?.playHit()
                    soundManager?.playDestroyed()
                    screenShakeTrigger++
                }
            } else {
                newGrid[x][y] = CellState.MISS
                statusText = "KINETIC IMPACT: NO HIT"
                soundManager?.playMiss()
            }
            opponentGrid = newGrid
            
            checkGameOver()
            if (currentPhase == GamePhase.COMBAT) {
                delay(1000)
                botMove()
            }
        }
    }

    private fun botMove() {
        var x: Int
        var y: Int
        do {
            x = Random.nextInt(10)
            y = Random.nextInt(10)
        } while (playerGrid[x][y] == CellState.HIT || playerGrid[x][y] == CellState.MISS)

        viewModelScope.launch {
            soundManager?.playLaunch()
            addToLog("EVASIVE MANEUVERS: INCOMING")
            activeMissile = MissileStrike(0.5f, -0.1f, x, y, false)
            delay(800)
            activeMissile = null
            
            val newGrid = playerGrid.map { it.copyOf() }.toTypedArray()
            if (newGrid[x][y] == CellState.SHIP) {
                val hitShip = playerShips.find { ship -> ship.coordinates.contains(x to y) }
                if (hitShip != null && !hitShip.isDestroyed) {
                    hitShip.isDestroyed = true
                    hitShip.coordinates.forEach { (cx, cy) ->
                        newGrid[cx][cy] = CellState.HIT
                    }
                    statusText = "HULL BREACH: ${hitShip.name}"
                    addToLog("HULL BREACH: ${hitShip.name} LOST", true)
                    soundManager?.playHit()
                    soundManager?.playDestroyed()
                    screenShakeTrigger++
                }
            } else {
                newGrid[x][y] = CellState.MISS
                statusText = "OPPONENT MISSED"
                soundManager?.playMiss()
            }
            playerGrid = newGrid
            
            checkGameOver()
            if (currentPhase == GamePhase.COMBAT) {
                isPlayerTurn = true
            }
        }
    }

    private fun checkGameOver() {
        val playerLost = playerShips.all { it.isDestroyed }
        val opponentLost = opponentShips.all { it.isDestroyed }

        if (playerLost) {
            currentPhase = GamePhase.GAME_OVER
            statusText = "FLEET TERMINATED"
            addToLog("Fleet Neutralized. System Shutdown.")
        } else if (opponentLost) {
            currentPhase = GamePhase.GAME_OVER
            statusText = "SECTOR SECURED"
            addToLog("All Targets Eliminated. Victory Achieved.")
            saveScore(missilesFired)
        }
    }

    private fun saveScore(missiles: Int) {
        _highScores.add(ScoreRecord(missiles))
    }

    fun restart() {
        playerGrid = Array(10) { Array(10) { CellState.EMPTY } }
        opponentGrid = Array(10) { Array(10) { CellState.EMPTY } }
        playerShips.forEach { 
            it.coordinates = emptyList()
            it.isDestroyed = false
        }
        opponentShips.forEach { 
            it.coordinates = emptyList()
            it.isDestroyed = false
        }
        currentPhase = GamePhase.MAIN_MENU
        statusText = "Grid Strike OS v1.0"
        activeMissile = null
        missilesFired = 0
        battleLog.clear()
    }
}
