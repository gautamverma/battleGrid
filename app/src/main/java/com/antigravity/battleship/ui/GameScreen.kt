package com.antigravity.battleship.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigravity.battleship.ui.theme.NeonBlue
import com.antigravity.battleship.ui.theme.NeonPink
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val shakeAnim = remember { Animatable(0f) }
    val soundManager = remember { SoundManager() }
    
    LaunchedEffect(Unit) {
        viewModel.soundManager = soundManager
    }

    LaunchedEffect(viewModel.screenShakeTrigger) {
        if (viewModel.screenShakeTrigger > 0) {
            repeat(4) {
                shakeAnim.animateTo(15f, animationSpec = tween(40, easing = LinearEasing))
                shakeAnim.animateTo(-15f, animationSpec = tween(40, easing = LinearEasing))
            }
            shakeAnim.animateTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer(translationX = shakeAnim.value)
    ) {
        // Background Decorative Elements
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 60.dp.toPx()
            for (x in 0..(size.width / gridSpacing).toInt()) {
                drawLine(
                    color = Color.White.copy(alpha = 0.03f),
                    start = Offset(x * gridSpacing, 0f),
                    end = Offset(x * gridSpacing, size.height)
                )
            }
            for (y in 0..(size.height / gridSpacing).toInt()) {
                drawLine(
                    color = Color.White.copy(alpha = 0.03f),
                    start = Offset(0f, y * gridSpacing),
                    end = Offset(size.width, y * gridSpacing)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GRID STRIKE",
                        color = NeonBlue,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "TACTICAL INTERFACE",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        letterSpacing = 4.sp
                    )
                }
                
                if (viewModel.currentPhase == GamePhase.MAIN_MENU) {
                    IconButton(
                        onClick = { viewModel.currentPhase = GamePhase.SETTINGS },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = NeonBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main View Area
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = viewModel.currentPhase,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
                    }
                ) { phase ->
                    when (phase) {
                        GamePhase.MAIN_MENU -> MainMenu(
                            onSinglePlayer = { viewModel.startSinglePlayer() },
                            onViewScores = { viewModel.currentPhase = GamePhase.SCOREBOARD }
                        )
                        GamePhase.PLACEMENT -> PlacementScreen(
                            grid = viewModel.playerGrid,
                            isHorizontal = viewModel.isHorizontal,
                            onCellClick = { x, y -> viewModel.placePlayerShip(x, y) },
                            onRotate = { viewModel.toggleRotation() },
                            onReady = { viewModel.onReady() }
                        )
                        GamePhase.COMBAT -> CombatScreen(
                            playerGrid = viewModel.playerGrid,
                            opponentGrid = viewModel.opponentGrid,
                            activeMissile = viewModel.activeMissile,
                            battleLog = viewModel.battleLog,
                            onAttack = { x, y -> viewModel.playerAttack(x, y) }
                        )
                        GamePhase.GAME_OVER -> GameOverScreen(
                            statusText = viewModel.statusText,
                            onRestart = { viewModel.restart() }
                        )
                        GamePhase.SCOREBOARD -> ScoreboardScreen(
                            scores = viewModel.highScores,
                            onBack = { viewModel.currentPhase = GamePhase.MAIN_MENU }
                        )
                        GamePhase.SETTINGS -> SettingsScreen(
                            soundManager = soundManager,
                            onBack = { viewModel.currentPhase = GamePhase.MAIN_MENU }
                        )
                    }
                }
            }

            // Status Ticker
            if (viewModel.currentPhase != GamePhase.MAIN_MENU && viewModel.currentPhase != GamePhase.SETTINGS) {
                Spacer(modifier = Modifier.height(16.dp))
                StatusTicker(text = viewModel.statusText)
            }
        }
    }
}

@Composable
fun StatusTicker(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .background(NeonBlue.copy(alpha = 0.05f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(NeonBlue)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text.uppercase(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NeonBlue
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, color, RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun MainMenu(onSinglePlayer: () -> Unit, onViewScores: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SELECT MISSION TYPE",
            color = Color.Gray,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        GameButton(text = "SOLO CAMPAIGN", onClick = onSinglePlayer)
        Spacer(modifier = Modifier.height(16.dp))
        GameButton(text = "HIGH SCORES", onClick = onViewScores, color = Color.White.copy(alpha = 0.7f))
        
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "MULTIPLAYER ENCRYPTION ACTIVE",
            color = NeonPink.copy(alpha = 0.5f),
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun SettingsScreen(soundManager: SoundManager, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SYSTEM SETTINGS",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Audio Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AUDIO OUTPUT", color = NeonBlue, fontSize = 14.sp)
                    Switch(
                        checked = !soundManager.isMuted,
                        onCheckedChange = { soundManager.isMuted = !it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonBlue)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("SOUND PROFILE", color = NeonBlue, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoundPack.values().forEach { pack ->
                        val isSelected = soundManager.currentPack == pack
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) NeonBlue else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (isSelected) NeonBlue else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .clickable { soundManager.currentPack = pack }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pack.name,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        GameButton(text = "CONFIRM & BACK", onClick = onBack)
    }
}

@Composable
fun PlacementScreen(
    grid: Array<Array<CellState>>,
    isHorizontal: Boolean,
    onCellClick: (Int, Int) -> Unit,
    onRotate: () -> Unit,
    onReady: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .border(2.dp, NeonBlue, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            BattleshipGrid(grid = grid, isInteractable = true, showShips = true, onCellClick = onCellClick)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GameButton(
                text = if (isHorizontal) "⟷ HORIZONTAL" else "⟶ VERTICAL",
                onClick = onRotate,
                modifier = Modifier.weight(1f),
                color = NeonPink
            )
            GameButton(
                text = "INITIATE COMBAT",
                onClick = onReady,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CombatScreen(
    playerGrid: Array<Array<CellState>>,
    opponentGrid: Array<Array<CellState>>,
    activeMissile: MissileStrike?,
    battleLog: List<BattleLogEntry>,
    onAttack: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Target Grid
        Text("TARGET VECTOR", color = NeonPink, fontSize = 12.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(1f)
                .border(1.dp, NeonPink, RoundedCornerShape(4.dp))
                .padding(4.dp)
        ) {
            BattleshipGrid(
                grid = opponentGrid,
                isInteractable = true,
                showShips = false,
                missile = if (activeMissile?.isOpponentGrid == true) activeMissile else null,
                onCellClick = onAttack
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Defensive Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.size(120.dp)) {
                Text("DEFENSIVE STATUS", color = NeonBlue, fontSize = 10.sp, modifier = Modifier.padding(bottom = 4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        .padding(2.dp)
                ) {
                    BattleshipGrid(
                        grid = playerGrid,
                        isInteractable = false,
                        showShips = true,
                        missile = if (activeMissile?.isOpponentGrid == false) activeMissile else null,
                        onCellClick = { _, _ -> }
                    )
                }
            }
            
            // Minimal Log - showing last 3 events
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
                    .height(100.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                battleLog.take(3).reversed().forEach { entry ->
                    Text(
                        text = "> ${entry.text}",
                        color = if (entry.isCritical) NeonPink else Color.Green.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GameOverScreen(statusText: String, onRestart: () -> Unit) {
    val isVictory = statusText.contains("VICTORY")
    val color = if (isVictory) NeonBlue else Color.Red

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isVictory) "MISSION SUCCESS" else "MISSION FAILED",
            color = color,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = statusText.uppercase(),
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        GameButton(text = "RE-ENGAGE", onClick = onRestart, color = color)
    }
}

@Composable
fun ScoreboardScreen(scores: List<ScoreRecord>, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(scores) { record ->
                ScoreItem(record)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        GameButton(text = "RETURN TO HUB", onClick = onBack)
    }
}

@Composable
fun ScoreItem(record: ScoreRecord) {
    val dateStr = SimpleDateFormat("yyyy.MM.dd | HH:mm", Locale.getDefault()).format(Date(record.date))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("EFFICIENCY RATING", color = Color.Gray, fontSize = 8.sp)
            Text("${record.missilesUsed} STRIKES", color = NeonBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(dateStr, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun BattleshipGrid(
    grid: Array<Array<CellState>>,
    isInteractable: Boolean,
    showShips: Boolean,
    missile: MissileStrike? = null,
    onCellClick: (Int, Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cellPx = constraints.maxWidth / 10f
        val missileProgress = remember { Animatable(0f) }
        
        LaunchedEffect(missile) {
            if (missile != null) {
                missileProgress.snapTo(0f)
                missileProgress.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
            }
        }

        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(isInteractable) {
                if (isInteractable) {
                    detectTapGestures { offset ->
                        val x = (offset.x / (size.width / 10f)).toInt()
                        val y = (offset.y / (size.height / 10f)).toInt()
                        if (x in 0..9 && y in 0..9) onCellClick(x, y)
                    }
                }
            }
        ) {
            // Grid background
            drawRect(Color(0xFF0A0A0A))

            // Grid Lines
            for (i in 0..10) {
                drawLine(Color.White.copy(alpha = 0.1f), Offset(i * cellPx, 0f), Offset(i * cellPx, size.height), 1f)
                drawLine(Color.White.copy(alpha = 0.1f), Offset(0f, i * cellPx), Offset(size.width, i * cellPx), 1f)
            }

            // Draw Cells
            for (x in 0..9) {
                for (y in 0..9) {
                    val state = grid[x][y]
                    val rectSize = Size(cellPx - 4f, cellPx - 4f)
                    val topLeft = Offset(x * cellPx + 2f, y * cellPx + 2f)
                    val centerX = topLeft.x + rectSize.width / 2
                    val centerY = topLeft.y + rectSize.height / 2

                    when (state) {
                        CellState.SHIP -> {
                            if (showShips) {
                                val path = Path().apply {
                                    moveTo(centerX, topLeft.y + 6f)
                                    lineTo(topLeft.x + rectSize.width - 6f, topLeft.y + rectSize.height - 6f)
                                    lineTo(centerX, topLeft.y + rectSize.height - 10f)
                                    lineTo(topLeft.x + 6f, topLeft.y + rectSize.height - 6f)
                                    close()
                                }
                                drawPath(path, color = NeonBlue.copy(alpha = 0.2f))
                                drawPath(path, color = NeonBlue, style = Stroke(2f))
                                drawCircle(color = NeonBlue, radius = 2f, center = Offset(centerX, centerY))
                            }
                        }
                        CellState.HIT -> {
                            drawRect(
                                brush = Brush.radialGradient(listOf(Color.Red, Color.Transparent)),
                                topLeft = topLeft,
                                size = rectSize
                            )
                            drawLine(color = Color.White, start = topLeft, end = Offset(topLeft.x + rectSize.width, topLeft.y + rectSize.height), strokeWidth = 2f)
                            drawLine(color = Color.White, start = Offset(topLeft.x + rectSize.width, topLeft.y), end = Offset(topLeft.x, topLeft.y + rectSize.height), strokeWidth = 2f)
                        }
                        CellState.MISS -> {
                            drawCircle(color = Color.White.copy(alpha = 0.2f), radius = cellPx / 8, center = Offset(centerX, centerY))
                        }
                        CellState.EMPTY -> {}
                    }
                }
            }

            // Animated Missile
            missile?.let {
                val start = Offset(it.startX * size.width, it.startY * size.height)
                val target = Offset(it.targetX * cellPx + cellPx/2, it.targetY * cellPx + cellPx/2)
                val currentPos = Offset(
                    start.x + (target.x - start.x) * missileProgress.value,
                    start.y + (target.y - start.y) * missileProgress.value
                )

                drawCircle(
                    brush = Brush.radialGradient(listOf(Color.Yellow, Color.Transparent)),
                    radius = 25f * (1f - missileProgress.value + 0.5f),
                    center = currentPos
                )
                drawCircle(color = Color.White, radius = 5f, center = currentPos)
                drawLine(color = NeonPink.copy(alpha = 0.2f), start = start, end = currentPos, strokeWidth = 2f)
            }
        }
    }
}
