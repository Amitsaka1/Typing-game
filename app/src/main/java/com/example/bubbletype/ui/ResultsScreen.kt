package com.example.bubbletype.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bubbletype.model.MatchResult
import com.example.ui.theme.*

@Composable
fun ResultsScreen(
    result: MatchResult,
    onReplay: () -> Unit,
    onNextLevel: () -> Unit,
    onLevelSelect: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSuccess = result.score > 0
    val infiniteTransition = rememberInfiniteTransition(label = "unlock_sparkle")
    val unlockScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "unlock_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        BubbleDarkBg,
                        Color(0xFF190F36),
                        BubbleDarkBgEnd
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("results_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            // Header: "LEVEL COMPLETE!" or "TIME'S UP!"
            Text(
                text = if (isSuccess) "LEVEL COMPLETE!" else "TIME'S UP!",
                color = if (isSuccess) BubbleGold else BubbleOrange,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                modifier = Modifier.testTag("results_title")
            )

            Text(
                text = "LEVEL ${result.level} • 2-MINUTE MATCH",
                color = BubbleTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Star Rating (0 to 3 Stars)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..3) {
                    val earned = i <= result.stars
                    Icon(
                        imageVector = if (earned) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star $i",
                        tint = if (earned) BubbleGold else Color(0xFF4A4468),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Stats Card
            Card(
                colors = CardDefaults.cardColors(containerColor = BubbleSurfaceElevated.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(listOf(BubbleBorder, BubbleCyan.copy(alpha = 0.4f)))
                ),
                modifier = Modifier.fillMaxWidth().testTag("results_stats_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Big Final Score Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("FINAL SCORE", color = BubbleTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${result.score}",
                                color = BubbleYellow,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        if (result.isNewHighScore) {
                            Surface(
                                color = BubbleGold,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "★ NEW HIGH!",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = BubbleBorder, thickness = 1.dp)

                    // 4 Grid Stats: Correct, Wrong, Missed, Accuracy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(label = "CORRECT", value = "${result.correctCount}", color = BubbleGreen)
                        StatItem(label = "WRONG", value = "${result.wrongCount}", color = BubbleRed)
                        StatItem(label = "MISSED", value = "${result.missedCount}", color = BubbleOrange)
                        StatItem(
                            label = "ACCURACY",
                            value = "${result.accuracy.toInt()}%",
                            color = if (result.accuracy >= 85f) BubbleCyan else BubbleYellow
                        )
                    }

                    HorizontalDivider(color = BubbleBorder, thickness = 1.dp)

                    // Secondary Performance Stats: Highest Combo & Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(label = "HIGHEST COMBO", value = "x${result.highestCombo}", color = BubbleMagenta)
                        StatItem(label = "TYPING SPEED", value = "${result.estimatedWpm} WPM", color = BubbleCyan)
                        StatItem(label = "CHAR CADENCE", value = "${result.charsPerMinute} CPM", color = BubbleTextPrimary)
                    }
                }
            }

            // Level Unlock Banner (if next level unlocked!)
            if (result.nextLevelUnlocked) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = BubbleMagenta.copy(alpha = 0.20f)),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(BubbleMagenta, BubbleGold))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(unlockScale)
                        .testTag("results_unlock_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Unlocked", tint = BubbleGold, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "LEVEL ${result.level} ➔ LEVEL ${result.level + 1} UNLOCKED!",
                            color = BubbleTextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Next Level Button (if unlocked and < 500)
                if (result.level < 500 && isSuccess) {
                    Button(
                        onClick = onNextLevel,
                        colors = ButtonDefaults.buttonColors(containerColor = BubbleCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_next_level")
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next Level", tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PLAY NEXT LEVEL", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Replay Button
                Button(
                    onClick = onReplay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSuccess) BubbleSurfaceElevated else BubbleYellow
                    ),
                    border = BorderStroke(1.dp, BubbleGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_replay_level")
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Replay",
                        tint = if (isSuccess) BubbleGold else Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RETRY LEVEL ${result.level}",
                        color = if (isSuccess) BubbleGold else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Row: Level Select & Main Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onLevelSelect,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("btn_results_level_select")
                    ) {
                        Icon(Icons.Default.GridOn, contentDescription = "Levels", tint = BubbleCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LEVELS", color = BubbleTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onMainMenu,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("btn_results_main_menu")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Menu", tint = BubbleTextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("MAIN MENU", color = BubbleTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(label, color = BubbleTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}
