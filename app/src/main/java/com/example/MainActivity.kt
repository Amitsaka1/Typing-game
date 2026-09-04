package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.bubbletype.audio.BubbleAudioPlayer
import com.example.bubbletype.data.SaveManager
import com.example.bubbletype.engine.BubbleTypeEngine
import com.example.bubbletype.model.MatchResult
import com.example.bubbletype.ui.*
import com.example.ui.theme.BubbleDarkBg
import com.example.ui.theme.MyApplicationTheme

enum class ScreenState {
    MAIN_MENU,
    PLAYING,
    RESULTS,
    LEVEL_SELECT,
    STATISTICS,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var saveManager: SaveManager
    private lateinit var audioPlayer: BubbleAudioPlayer
    private lateinit var engine: BubbleTypeEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        saveManager = SaveManager(applicationContext)
        audioPlayer = BubbleAudioPlayer()
        engine = BubbleTypeEngine(applicationContext, saveManager, audioPlayer)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BubbleDarkBg
                ) {
                    var currentScreen by remember { mutableStateOf(ScreenState.MAIN_MENU) }
                    var lastResult by remember { mutableStateOf<MatchResult?>(null) }

                    // Hardware back button navigation handling
                    BackHandler(enabled = currentScreen != ScreenState.MAIN_MENU) {
                        when (currentScreen) {
                            ScreenState.PLAYING -> {
                                engine.setPause(true)
                            }
                            ScreenState.RESULTS,
                            ScreenState.LEVEL_SELECT,
                            ScreenState.STATISTICS,
                            ScreenState.SETTINGS -> {
                                currentScreen = ScreenState.MAIN_MENU
                            }
                            else -> {}
                        }
                    }

                    when (currentScreen) {
                        ScreenState.MAIN_MENU -> {
                            MainMenuScreen(
                                saveManager = saveManager,
                                onPlay = {
                                    val currentUnlocked = saveManager.getUnlockedLevel()
                                    engine.setupLevel(currentUnlocked)
                                    engine.startMatch()
                                    currentScreen = ScreenState.PLAYING
                                },
                                onLevelSelect = {
                                    currentScreen = ScreenState.LEVEL_SELECT
                                },
                                onStatistics = {
                                    currentScreen = ScreenState.STATISTICS
                                },
                                onSettings = {
                                    currentScreen = ScreenState.SETTINGS
                                }
                            )
                        }

                        ScreenState.PLAYING -> {
                            GamePlayScreen(
                                engine = engine,
                                onGameOver = { result ->
                                    lastResult = result
                                    currentScreen = ScreenState.RESULTS
                                },
                                onExitToMenu = {
                                    engine.setPause(false)
                                    currentScreen = ScreenState.MAIN_MENU
                                }
                            )
                        }

                        ScreenState.RESULTS -> {
                            lastResult?.let { result ->
                                ResultsScreen(
                                    result = result,
                                    onReplay = {
                                        engine.restartCurrentLevel()
                                        currentScreen = ScreenState.PLAYING
                                    },
                                    onNextLevel = {
                                        engine.startNextLevel()
                                        currentScreen = ScreenState.PLAYING
                                    },
                                    onLevelSelect = {
                                        currentScreen = ScreenState.LEVEL_SELECT
                                    },
                                    onMainMenu = {
                                        currentScreen = ScreenState.MAIN_MENU
                                    }
                                )
                            } ?: run {
                                currentScreen = ScreenState.MAIN_MENU
                            }
                        }

                        ScreenState.LEVEL_SELECT -> {
                            LevelSelectScreen(
                                saveManager = saveManager,
                                onSelectLevel = { selectedLevel ->
                                    engine.setupLevel(selectedLevel)
                                    engine.startMatch()
                                    currentScreen = ScreenState.PLAYING
                                },
                                onBack = {
                                    currentScreen = ScreenState.MAIN_MENU
                                }
                            )
                        }

                        ScreenState.STATISTICS -> {
                            StatisticsScreen(
                                saveManager = saveManager,
                                onBack = {
                                    currentScreen = ScreenState.MAIN_MENU
                                }
                            )
                        }

                        ScreenState.SETTINGS -> {
                            SettingsScreen(
                                saveManager = saveManager,
                                audioPlayer = audioPlayer,
                                onBack = {
                                    currentScreen = ScreenState.MAIN_MENU
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::audioPlayer.isInitialized) {
            audioPlayer.stopBackgroundMusic()
        }
        if (::engine.isInitialized && engine.isRunning) {
            engine.setPause(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::audioPlayer.isInitialized) {
            audioPlayer.release()
        }
    }
}
