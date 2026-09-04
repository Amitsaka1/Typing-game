package com.example.bubbletype.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bubbletype.audio.BubbleAudioPlayer
import com.example.bubbletype.data.SaveManager
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    saveManager: SaveManager,
    audioPlayer: BubbleAudioPlayer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifetimeStats = remember { saveManager.loadLifetimeStats() }

    var sound by remember { mutableStateOf(lifetimeStats.soundEnabled) }
    var music by remember { mutableStateOf(lifetimeStats.musicEnabled) }
    var vibration by remember { mutableStateOf(lifetimeStats.vibrationEnabled) }
    var particleQuality by remember { mutableStateOf(lifetimeStats.particleQuality) }
    var animationQuality by remember { mutableStateOf(lifetimeStats.animationQuality) }

    var showResetDialog by remember { mutableStateOf(false) }

    fun persist() {
        saveManager.updateSettings(
            sound = sound,
            music = music,
            vibration = vibration,
            particleQuality = particleQuality,
            animationQuality = animationQuality
        )
        audioPlayer.soundEnabled = sound
        audioPlayer.musicEnabled = music
        if (!music) {
            audioPlayer.stopBackgroundMusic()
        }
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
            .testTag("settings_screen")
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
                    modifier = Modifier.testTag("btn_back_from_settings")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BubbleTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SETTINGS",
                    color = BubbleCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Audio & Feedback Card
            Text(
                text = "AUDIO & FEEDBACK",
                color = BubbleTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = BubbleSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(BubbleBorder, BubbleBorder.copy(alpha = 0.5f)))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingToggle(
                        label = "Sound Effects (Pop & Chimes)",
                        checked = sound,
                        onCheckedChange = {
                            sound = it
                            persist()
                        },
                        tag = "switch_sound"
                    )
                    HorizontalDivider(color = BubbleBorder, thickness = 0.5.dp)
                    SettingToggle(
                        label = "Arcade Background Music",
                        checked = music,
                        onCheckedChange = {
                            music = it
                            persist()
                        },
                        tag = "switch_music"
                    )
                    HorizontalDivider(color = BubbleBorder, thickness = 0.5.dp)
                    SettingToggle(
                        label = "Haptic Vibration Feedback",
                        checked = vibration,
                        onCheckedChange = {
                            vibration = it
                            persist()
                        },
                        tag = "switch_vibration"
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Graphics & Performance Card
            Text(
                text = "GRAPHICS & PERFORMANCE",
                color = BubbleTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = BubbleSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(BubbleBorder, BubbleBorder.copy(alpha = 0.5f)))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Text("Particle Density Quality", color = BubbleTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("LOW", "MEDIUM", "HIGH").forEach { quality ->
                                val selected = particleQuality == quality
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        particleQuality = quality
                                        persist()
                                    },
                                    label = { Text(quality, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BubbleCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = BubbleBorder, thickness = 0.5.dp)

                    Column {
                        Text("Animation Quality", color = BubbleTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("NORMAL", "HIGH").forEach { quality ->
                                val selected = animationQuality == quality
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        animationQuality = quality
                                        persist()
                                    },
                                    label = { Text(quality, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BubbleCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Danger Zone: Reset Progress
            OutlinedButton(
                onClick = { showResetDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BubbleRed),
                border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.horizontalGradient(listOf(BubbleRed, BubbleRed))),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_reset_progress")
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = BubbleRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("RESET ALL GAME PROGRESS", fontWeight = FontWeight.Bold)
            }
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                containerColor = BubbleSurfaceElevated,
                title = { Text("Reset Progress?", color = BubbleRed, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "This will reset all unlocked levels (back to Level 1), best scores, and lifetime statistics. This action cannot be undone.",
                        color = BubbleTextPrimary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            saveManager.resetAllProgress()
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BubbleRed)
                    ) {
                        Text("YES, RESET", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("CANCEL", color = BubbleTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = BubbleTextPrimary, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = BubbleCyan,
                uncheckedThumbColor = BubbleTextSecondary,
                uncheckedTrackColor = BubbleBorder
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}
