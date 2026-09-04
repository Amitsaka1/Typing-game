package com.example.bubbletype.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bubbletype.engine.BubbleTypeEngine
import com.example.bubbletype.model.MatchResult
import com.example.ui.theme.*

@Composable
fun GamePlayScreen(
    engine: BubbleTypeEngine,
    onGameOver: (MatchResult) -> Unit,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hudState by engine.hudState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var textInput by remember { mutableStateOf(TextFieldValue("")) }

    var lastPressedChar by remember { mutableStateOf<Char?>(null) }
    var showArcadeKeyboard by remember { mutableStateOf(true) }

    LaunchedEffect(lastPressedChar) {
        if (lastPressedChar != null) {
            kotlinx.coroutines.delay(120)
            lastPressedChar = null
        }
    }

    fun onCharTypedInternal(c: Char) {
        val upper = c.uppercaseChar()
        if (upper in 'A'..'Z') {
            lastPressedChar = upper
            engine.onCharTyped(upper)
        }
    }

    // Connect match finish callback
    DisposableEffect(engine) {
        engine.onMatchFinished = { result ->
            onGameOver(result)
        }
        onDispose {
            engine.onMatchFinished = null
        }
    }

    // Auto-focus software keyboard on start
    LaunchedEffect(engine.isRunning, engine.isPaused) {
        if (engine.isRunning && !engine.isPaused) {
            focusRequester.requestFocus()
        }
    }

    // 60FPS Game Engine loop
    var frameTrigger by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - lastTime) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
                lastTime = now
                engine.update(dt)
                frameTrigger = now
            }
        }
    }

    // Pulse animation for high combos
    val infiniteTransition = rememberInfiniteTransition(label = "combo_pulse")
    val comboScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (hudState.combo >= 5) 1.12f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "combo_scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BubbleDarkBg)
            .testTag("gameplay_screen")
    ) {
        // Invisible input field that catches mobile/hardware keyboard input reliably
        BasicTextField(
            value = textInput,
            onValueChange = { newValue ->
                val newText = newValue.text
                if (newText.isNotEmpty()) {
                    for (c in newText) {
                        onCharTypedInternal(c)
                    }
                }
                // Always clear back to empty string so typing is continuous
                textInput = TextFieldValue("")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.None,
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false
            ),
            keyboardActions = KeyboardActions.Default,
            modifier = Modifier
                .size(1.dp)
                .alpha(0.01f)
                .focusRequester(focusRequester)
                .testTag("hidden_keyboard_input")
        )

        // 1. Top HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Row 1: Level Badge, Timer, Pause Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level & Difficulty Badge
                Surface(
                    color = BubbleSurfaceElevated.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BubbleBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "LVL ${hudState.level}",
                            color = BubbleCyan,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• ${hudState.phase.nameTitle.uppercase()}",
                            color = when (hudState.phase.phaseNumber) {
                                1 -> BubbleGreen
                                2 -> BubbleCyan
                                3 -> BubbleYellow
                                4 -> BubbleOrange
                                5 -> BubbleRed
                                else -> BubbleMagenta
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                // Center Timer (2-minute countdown)
                Surface(
                    color = BubbleSurface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        1.5.dp,
                        if (hudState.remainingSeconds <= 20f) BubbleRed else BubbleCyan.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("match_timer_badge")
                ) {
                    val secondsInt = hudState.remainingSeconds.toInt()
                    val mins = secondsInt / 60
                    val secs = secondsInt % 60
                    val timeFormatted = String.format("%02d:%02d", mins, secs)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hudState.remainingSeconds <= 20f) BubbleRed else BubbleGreen
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = timeFormatted,
                            color = if (hudState.remainingSeconds <= 20f) BubbleRed else BubbleTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Pause Button
                IconButton(
                    onClick = { engine.setPause(true) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BubbleSurfaceElevated.copy(alpha = 0.85f))
                        .testTag("btn_pause")
                ) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = BubbleTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Score, Combo Display, Accuracy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score Box
                Column {
                    Text(
                        text = "SCORE",
                        color = BubbleTextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${hudState.score}",
                        color = BubbleYellow,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("hud_score")
                    )
                }

                // Combo Banner
                if (hudState.combo > 1) {
                    Surface(
                        color = BubbleSurfaceElevated.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, BubbleMagenta),
                        modifier = Modifier
                            .scale(comboScale)
                            .testTag("hud_combo_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "COMBO",
                                color = BubbleMagenta,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "x${hudState.combo}",
                                color = BubbleTextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            if (hudState.comboMultiplier > 1.0f) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    color = BubbleMagenta,
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        text = "${hudState.comboMultiplier}x",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Stats Chips (Accuracy, Correct, Wrong, Missed)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ACCURACY",
                        color = BubbleTextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${hudState.accuracy.toInt()}%",
                        color = when {
                            hudState.accuracy >= 90f -> BubbleGreen
                            hudState.accuracy >= 75f -> BubbleYellow
                            else -> BubbleRed
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("hud_accuracy")
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Text("✓ ${hudState.correctCount}", color = BubbleGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("✗ ${hudState.wrongCount}", color = BubbleRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("⤓ ${hudState.missedCount}", color = BubbleOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Middle Vertical Falling Bubbles Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
                .testTag("vertical_playfield")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (frameTrigger >= 0) {
                    BubbleCanvasRenderer.render(this, engine, size.width, size.height)
                }
            }
        }

        // 3. Bottom Arcade QWERTY Keyboard
        if (showArcadeKeyboard) {
            ArcadeQwertyKeyboard(
                lastPressedChar = lastPressedChar,
                onKeyPressed = { char ->
                    onCharTypedInternal(char)
                },
                onToggleSoftKeyboard = {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                },
                onToggleKeyboardVisibility = {
                    showArcadeKeyboard = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        } else {
            // Minimized Keyboard Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text("⌨ OPEN SOFT KEYBOARD", color = BubbleCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showArcadeKeyboard = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("SHOW KEYS", color = BubbleYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Pause Menu Dialog
        if (hudState.isPaused) {
            AlertDialog(
                onDismissRequest = { engine.setPause(false) },
                containerColor = BubbleSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = "PAUSED",
                        color = BubbleCyan,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Level ${hudState.level} • Score: ${hudState.score}",
                            color = BubbleTextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Resume Button
                        Button(
                            onClick = {
                                engine.setPause(false)
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BubbleCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_resume")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RESUME GAME", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Restart Button
                        OutlinedButton(
                            onClick = {
                                engine.restartCurrentLevel()
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_restart")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = BubbleYellow)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RETRY LEVEL", color = BubbleTextPrimary, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quit Button
                        TextButton(
                            onClick = onExitToMenu,
                            modifier = Modifier.fillMaxWidth().testTag("btn_quit_to_menu")
                        ) {
                            Text("QUIT TO MAIN MENU", color = BubbleRed, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@Composable
fun ArcadeQwertyKeyboard(
    lastPressedChar: Char?,
    onKeyPressed: (Char) -> Unit,
    onToggleSoftKeyboard: () -> Unit,
    onToggleKeyboardVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    val row1 = listOf('Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P')
    val row2 = listOf('A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L')
    val row3 = listOf('Z', 'X', 'C', 'V', 'B', 'N', 'M')

    Surface(
        color = BubbleSurface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = BorderStroke(1.dp, BubbleBorder),
        modifier = modifier.testTag("arcade_keyboard")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Row 1: 10 Keys
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (char in row1) {
                    ArcadeKeyButton(
                        char = char,
                        isPressed = lastPressedChar == char,
                        onClick = { onKeyPressed(char) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 2: 9 Keys Centered
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Spacer(modifier = Modifier.weight(0.5f))
                for (char in row2) {
                    ArcadeKeyButton(
                        char = char,
                        isPressed = lastPressedChar == char,
                        onClick = { onKeyPressed(char) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.weight(0.5f))
            }

            // Row 3: Soft Keyboard Switcher + 7 Keys + Hide Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Soft Keyboard trigger
                Surface(
                    onClick = onToggleSoftKeyboard,
                    color = BubbleSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BubbleCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp)
                        .testTag("btn_soft_keyboard")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "⌨",
                            fontSize = 16.sp,
                            color = BubbleCyan
                        )
                    }
                }

                for (char in row3) {
                    ArcadeKeyButton(
                        char = char,
                        isPressed = lastPressedChar == char,
                        onClick = { onKeyPressed(char) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Hide On-Screen Keyboard button
                Surface(
                    onClick = onToggleKeyboardVisibility,
                    color = BubbleSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BubbleBorder),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp)
                        .testTag("btn_hide_arcade_keyboard")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "⤓",
                            fontSize = 16.sp,
                            color = BubbleTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArcadeKeyButton(
    char: Char,
    isPressed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isPressed) BubbleCyan else BubbleSurfaceElevated
    val textColor = if (isPressed) Color.Black else BubbleTextPrimary
    val borderColor = if (isPressed) BubbleCyan else BubbleBorder

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .height(44.dp)
            .testTag("key_$char")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = char.toString(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
