package com.example.bubbletype.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bubbletype.data.SaveManager
import com.example.ui.theme.*

private data class AmbientBubble(
    val xRatio: Float,
    val yOffsetPhase: Float,
    val radiusDp: Float,
    val color: Color,
    val speed: Float
)

@Composable
fun MainMenuScreen(
    saveManager: SaveManager,
    onPlay: () -> Unit,
    onLevelSelect: () -> Unit,
    onStatistics: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unlockedLevel = remember { saveManager.getUnlockedLevel() }

    // Ambient floating bubbles generator
    val ambientBubbles = remember {
        listOf(
            AmbientBubble(0.15f, 0.2f, 32f, BubbleCyan, 0.3f),
            AmbientBubble(0.82f, 0.7f, 44f, BubbleMagenta, 0.25f),
            AmbientBubble(0.35f, 0.5f, 26f, BubbleYellow, 0.35f),
            AmbientBubble(0.65f, 0.1f, 38f, BubbleGreen, 0.22f),
            AmbientBubble(0.20f, 0.9f, 28f, BubblePurple, 0.28f),
            AmbientBubble(0.88f, 0.35f, 34f, BubbleOrange, 0.32f),
            AmbientBubble(0.50f, 0.8f, 22f, BubbleGold, 0.4f)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_time"
    )

    val playPulse by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "play_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        BubbleDarkBg,
                        Color(0xFF1B0F33),
                        BubbleDarkBgEnd
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("main_menu_screen")
    ) {
        // Ambient background drifting bubbles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            for (b in ambientBubbles) {
                val cycle = (time * b.speed + b.yOffsetPhase) % 1.0f
                val y = (1.0f - cycle) * h
                val x = b.xRatio * w + kotlin.math.sin(time + b.yOffsetPhase) * 20f
                val r = b.radiusDp * density

                // Soft glowing ambient bubble
                drawCircle(
                    color = b.color.copy(alpha = 0.18f),
                    radius = r,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = b.color.copy(alpha = 0.40f),
                    radius = r,
                    center = Offset(x, y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * density)
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top App Bar / Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BubbleSurfaceElevated.copy(alpha = 0.8f))
                        .testTag("btn_menu_settings")
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = BubbleTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Title and Logo Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Iridescent glowing Bubble Badge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    BubbleCyan.copy(alpha = 0.9f),
                                    BubbleMagenta.copy(alpha = 0.7f),
                                    BubbleDarkBg
                                )
                            )
                        )
                        .border(2.dp, BubbleCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B",
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "BUBBLE TYPE",
                    color = BubbleCyan,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "2-MINUTE ARCADE TYPING CHALLENGE",
                    color = BubbleMagenta,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Badge
                Surface(
                    color = BubbleSurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BubbleGold.copy(alpha = 0.7f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = BubbleGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CURRENT LEVEL: $unlockedLevel / 500",
                            color = BubbleTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Main Menu Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Big PLAY Button (Starts current level)
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = BubbleCyan),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .scale(playPulse)
                        .testTag("btn_menu_play")
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "PLAY LEVEL $unlockedLevel",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                }

                // Level Select Button
                Button(
                    onClick = onLevelSelect,
                    colors = ButtonDefaults.buttonColors(containerColor = BubbleSurfaceElevated),
                    border = BorderStroke(1.dp, BubbleBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_menu_level_select")
                ) {
                    Icon(Icons.Default.GridOn, contentDescription = "Level Select", tint = BubbleCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SELECT LEVEL (500)",
                        color = BubbleTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Statistics Button
                Button(
                    onClick = onStatistics,
                    colors = ButtonDefaults.buttonColors(containerColor = BubbleSurfaceElevated),
                    border = BorderStroke(1.dp, BubbleBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_menu_statistics")
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = "Statistics", tint = BubbleYellow)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "STATISTICS & RECORDS",
                        color = BubbleTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Footer note
            Text(
                text = "FAST TYPING • POP COMBOS • PROGRESSIVE SPEED",
                color = BubbleTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}
