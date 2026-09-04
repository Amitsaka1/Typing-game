package com.example.bubbletype.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bubbletype.data.SaveManager
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LevelSelectScreen(
    saveManager: SaveManager,
    onSelectLevel: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unlockedLevel = remember { saveManager.getUnlockedLevel() }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    // 10 sections of 50 levels each: 1..50, 51..100, etc.
    val sectionIndex = remember { mutableIntStateOf((unlockedLevel - 1) / 50) }

    LaunchedEffect(unlockedLevel) {
        // Scroll near current unlocked level
        gridState.scrollToItem((unlockedLevel - 1).coerceAtLeast(0))
    }

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
            .testTag("level_select_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_back_from_levels")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BubbleTextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SELECT LEVEL",
                        color = BubbleCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "PROGRESS: $unlockedLevel / 500 UNLOCKED",
                        color = BubbleGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Quick Jump Button to current highest unlocked
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            gridState.animateScrollToItem((unlockedLevel - 1).coerceAtLeast(0))
                        }
                    },
                    modifier = Modifier.testTag("btn_jump_to_current")
                ) {
                    Text("CURRENT", color = BubbleCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tier/Section Horizontal Filter Chips (1-50, 51-100, ..., 451-500)
            ScrollableTabRow(
                selectedTabIndex = sectionIndex.intValue,
                containerColor = Color.Transparent,
                contentColor = BubbleCyan,
                edgePadding = 0.dp,
                divider = {}
            ) {
                for (s in 0 until 10) {
                    val startLvl = s * 50 + 1
                    val endLvl = (s + 1) * 50
                    val isUnlocked = unlockedLevel >= startLvl

                    Tab(
                        selected = sectionIndex.intValue == s,
                        onClick = {
                            sectionIndex.intValue = s
                            coroutineScope.launch {
                                gridState.animateScrollToItem(startLvl - 1)
                            }
                        },
                        text = {
                            Text(
                                text = "$startLvl-$endLvl",
                                color = if (sectionIndex.intValue == s) BubbleCyan else if (isUnlocked) BubbleTextSecondary else BubbleTextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (sectionIndex.intValue == s) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 500 Level Grid (4 columns)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("level_grid")
            ) {
                items(500) { index ->
                    val level = index + 1
                    val isUnlocked = level <= unlockedLevel
                    val isCurrent = level == unlockedLevel
                    val stars = if (isUnlocked) saveManager.getStarsForLevel(level) else 0
                    val bestScore = if (isUnlocked) saveManager.getBestScoreForLevel(level) else 0

                    LevelCard(
                        level = level,
                        isUnlocked = isUnlocked,
                        isCurrent = isCurrent,
                        stars = stars,
                        bestScore = bestScore,
                        onClick = {
                            if (isUnlocked) {
                                onSelectLevel(level)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCard(
    level: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    stars: Int,
    bestScore: Int,
    onClick: () -> Unit
) {
    val borderColor = when {
        isCurrent -> BubbleGold
        isUnlocked -> BubbleCyan.copy(alpha = 0.5f)
        else -> BubbleBorder.copy(alpha = 0.5f)
    }

    val backgroundColor = when {
        isCurrent -> BubbleSurfaceElevated
        isUnlocked -> BubbleSurface
        else -> Color(0xFF120E22)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clickable(enabled = isUnlocked, onClick = onClick)
            .testTag("level_card_$level")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isUnlocked) {
                Text(
                    text = "$level",
                    color = if (isCurrent) BubbleGold else BubbleTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                // Stars row
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 1..3) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i <= stars) BubbleGold else Color(0xFF3B3556),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Best score snippet
                if (bestScore > 0) {
                    Text(
                        text = "$bestScore",
                        color = BubbleYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (isCurrent) {
                    Text(
                        text = "READY",
                        color = BubbleGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = BubbleTextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$level",
                    color = BubbleTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
