package com.example.bubbletype.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bubbletype.data.SaveManager
import com.example.ui.theme.*

@Composable
fun StatisticsScreen(
    saveManager: SaveManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = saveManager.loadLifetimeStats()
    val totalInputs = stats.totalCorrect + stats.totalWrong + stats.totalMissed
    val overallAccuracy = if (totalInputs == 0) 0f else (stats.totalCorrect.toFloat() / totalInputs * 100f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        BubbleDarkBg,
                        Color(0xFF160F30),
                        BubbleDarkBgEnd
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("statistics_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_back_from_stats")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BubbleTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LIFETIME STATISTICS",
                    color = BubbleCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Unlocked Level Progression Card
            Card(
                colors = CardDefaults.cardColors(containerColor = BubbleSurfaceElevated),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(BubbleCyan, BubbleMagenta))
                ),
                modifier = Modifier.fillMaxWidth().testTag("stats_progress_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LEVEL REACHED", color = BubbleTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Level ${stats.unlockedLevel}",
                                color = BubbleGold,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = "${(stats.unlockedLevel / 500f * 100).toInt()}% Done",
                            color = BubbleCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { stats.unlockedLevel / 500f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = BubbleCyan,
                        trackColor = BubbleBorder
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Core Records Card
            Text(
                text = "MATCH RECORDS",
                color = BubbleTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = BubbleSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(listOf(BubbleBorder, BubbleBorder.copy(alpha = 0.5f)))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    StatRow(label = "Highest Single-Game Score", value = "${stats.highestScore}", color = BubbleYellow)
                    HorizontalDivider(color = BubbleBorder, thickness = 0.5.dp)
                    StatRow(label = "Highest Combo Streak", value = "x${stats.highestCombo}", color = BubbleMagenta)
                    HorizontalDivider(color = BubbleBorder, thickness = 0.5.dp)
                    StatRow(label = "Peak Game Accuracy", value = "${stats.bestAccuracy.toInt()}%", color = BubbleGreen)
                    HorizontalDivider(color = BubbleBorder, thickness = 0.5.dp)
                    StatRow(label = "Total Games Played", value = "${stats.totalGamesPlayed}", color = BubbleTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Typing Totals Card
            Text(
                text = "TYPING ACCUMULATION",
                color = BubbleTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = BubbleSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(listOf(BubbleBorder, BubbleBorder.copy(alpha = 0.5f)))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    StatRow(label = "Total Correct Letters Popped", value = "${stats.totalCorrect}", color = BubbleGreen)
                    HorizontalDivider(color = BubbleBorder, thickness = 0.5.dp)
                    StatRow(label = "Total Wrong Typing Strikes", value = "${stats.totalWrong}", color = BubbleRed)
                    HorizontalDivider(color = BubbleBorder, thickness = 0.5.dp)
                    StatRow(label = "Total Bubbles Missed", value = "${stats.totalMissed}", color = BubbleOrange)
                    HorizontalDivider(color = BubbleBorder, thickness = 0.5.dp)
                    StatRow(label = "Lifetime Overall Accuracy", value = "${overallAccuracy.toInt()}%", color = BubbleCyan)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = BubbleTextSecondary, fontSize = 13.sp)
        Text(text = value, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}
