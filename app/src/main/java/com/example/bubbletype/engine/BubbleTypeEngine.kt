package com.example.bubbletype.engine

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.graphics.Color
import com.example.bubbletype.audio.BubbleAudioPlayer
import com.example.bubbletype.data.SaveManager
import com.example.bubbletype.model.*
import com.example.ui.theme.BubbleGreen
import com.example.ui.theme.BubbleOrange
import com.example.ui.theme.BubbleRed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin
import kotlin.random.Random

data class EngineHudState(
    val remainingSeconds: Float = 120f,
    val score: Int = 0,
    val combo: Int = 0,
    val comboMultiplier: Float = 1.0f,
    val highestCombo: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val missedCount: Int = 0,
    val accuracy: Float = 100f,
    val level: Int = 1,
    val phase: DifficultyPhase = DifficultyPhase.PHASE_1,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val screenShake: Float = 0f,
    val redVignetteFlash: Float = 0f
)

class BubbleTypeEngine(
    val context: Context,
    val saveManager: SaveManager,
    val audioPlayer: BubbleAudioPlayer
) {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    var currentLevel: Int = 1
        private set

    lateinit var levelConfig: LevelConfig
        private set

    // Active entities
    val activeBubbles = mutableListOf<Bubble>()
    val activeParticles = mutableListOf<Particle>()
    val activeFloatingTexts = mutableListOf<FloatingText>()

    // Match metrics
    var matchDurationSeconds: Float = 120f
    var timeRemaining: Float = 120f
    var matchTimeElapsed: Float = 0f

    var score: Int = 0
    var combo: Int = 0
    var highestCombo: Int = 0
    var correctCount: Int = 0
    var wrongCount: Int = 0
    var missedCount: Int = 0
    var totalSpawned: Int = 0

    var isRunning: Boolean = false
    var isPaused: Boolean = false
    var isGameOver: Boolean = false

    // Screen feedback intensities
    var screenShake: Float = 0f
    var redVignetteFlash: Float = 0f

    // Spawn control
    private var spawnTimer: Float = 0f
    private var nextBubbleId: Long = 1L
    private var patternCounter: Int = 0
    private var recentSpawnX: Float = 0.5f

    // Particle pool limit based on setting
    var maxParticleCount: Int = 120

    var onMatchFinished: ((MatchResult) -> Unit)? = null

    private val _hudState = MutableStateFlow(EngineHudState())
    val hudState: StateFlow<EngineHudState> = _hudState.asStateFlow()

    init {
        loadSettings()
        setupLevel(saveManager.getUnlockedLevel())
    }

    fun loadSettings() {
        val stats = saveManager.loadLifetimeStats()
        audioPlayer.soundEnabled = stats.soundEnabled
        audioPlayer.musicEnabled = stats.musicEnabled
        maxParticleCount = when (stats.particleQuality) {
            "LOW" -> 40
            "MEDIUM" -> 80
            else -> 140
        }
    }

    fun setupLevel(level: Int) {
        currentLevel = level.coerceIn(1, 500)
        levelConfig = LevelGenerator.getLevelConfig(currentLevel)
        resetMatch()
    }

    fun startMatch() {
        resetMatch()
        isRunning = true
        isPaused = false
        isGameOver = false
        audioPlayer.startBackgroundMusic()
        emitHud()
    }

    fun restartCurrentLevel() {
        setupLevel(currentLevel)
        startMatch()
    }

    fun startNextLevel() {
        val next = (currentLevel + 1).coerceAtMost(500)
        setupLevel(next)
        startMatch()
    }

    fun setPause(paused: Boolean) {
        isPaused = paused
        if (paused) {
            audioPlayer.stopBackgroundMusic()
        } else {
            audioPlayer.startBackgroundMusic()
        }
        emitHud()
    }

    private fun resetMatch() {
        timeRemaining = matchDurationSeconds
        matchTimeElapsed = 0f
        score = 0
        combo = 0
        highestCombo = 0
        correctCount = 0
        wrongCount = 0
        missedCount = 0
        totalSpawned = 0
        spawnTimer = 0.4f // quick initial spawn
        screenShake = 0f
        redVignetteFlash = 0f
        patternCounter = 0

        activeBubbles.clear()
        activeParticles.clear()
        activeFloatingTexts.clear()
    }

    // Process player keyboard typing (instant detection, A-Z case-insensitive)
    fun onCharTyped(rawChar: Char) {
        if (!isRunning || isPaused || isGameOver) return

        val char = rawChar.uppercaseChar()
        if (char !in 'A'..'Z') return

        // Search active bubbles for matching letter that hasn't popped or missed
        // Pick the candidate with the highest Y value (nearest to bottom / most urgent!)
        val targetBubble = activeBubbles
            .filter { !it.isPopped && !it.isMissed && it.letter == char }
            .maxByOrNull { it.y }

        if (targetBubble != null) {
            // Correct typing!
            handleCorrectHit(targetBubble)
        } else {
            // Wrong typing!
            handleWrongTyping(char)
        }
        emitHud()
    }

    private fun handleCorrectHit(bubble: Bubble) {
        bubble.isPopped = true
        bubble.popProgress = 0.01f
        correctCount++
        combo++
        if (combo > highestCombo) highestCombo = combo

        // Combo multiplier: 1x, 1.5x at 5+, 2x at 10+, 2.5x at 20+, 3x at 35+
        val multiplier = getComboMultiplier(combo)
        val pointsToAdd = (1 * multiplier).toInt().coerceAtLeast(1)
        score += pointsToAdd

        // Visual floating text
        val textBonus = if (pointsToAdd > 1) "+$pointsToAdd (${multiplier}x)" else "+$pointsToAdd"
        activeFloatingTexts.add(
            FloatingText(
                x = bubble.getEffectiveX(matchTimeElapsed),
                y = bubble.y,
                text = textBonus,
                color = BubbleGreen
            )
        )

        // Spawn burst particles
        spawnBubblePopParticles(bubble)

        // Audio & Haptics
        audioPlayer.playPop(combo)
        vibrate(20)
    }

    private fun handleWrongTyping(char: Char) {
        wrongCount++
        combo = 0 // Break combo

        // Subtract 1 point, clamp to 0
        score = (score - 1).coerceAtLeast(0)

        // Feedback: screen shake & red flash
        screenShake = 1.0f
        redVignetteFlash = 0.85f

        // Floating error text
        activeFloatingTexts.add(
            FloatingText(
                x = 0.5f,
                y = 0.65f,
                text = "-1 [ $char ]",
                color = BubbleRed
            )
        )

        audioPlayer.playWrong()
        vibrate(55)
    }

    private fun handleBubbleMissed(bubble: Bubble) {
        bubble.isMissed = true
        bubble.missedProgress = 0.01f
        missedCount++
        combo = 0 // Break combo

        // Subtract 1 point, clamp to 0
        score = (score - 1).coerceAtLeast(0)

        // Spawn splash particles at bottom
        spawnBottomSplashParticles(bubble)

        // Floating text
        activeFloatingTexts.add(
            FloatingText(
                x = bubble.getEffectiveX(matchTimeElapsed),
                y = 0.94f,
                text = "MISSED! -1",
                color = BubbleOrange
            )
        )

        audioPlayer.playMiss()
        vibrate(40)
    }

    fun getComboMultiplier(currentCombo: Int): Float {
        return when {
            currentCombo >= 35 -> 3.0f
            currentCombo >= 20 -> 2.5f
            currentCombo >= 10 -> 2.0f
            currentCombo >= 5 -> 1.5f
            else -> 1.0f
        }
    }

    fun calculateAccuracy(): Float {
        val total = correctCount + wrongCount + missedCount
        return if (total == 0) 100.0f else (correctCount.toFloat() / total * 100.0f).coerceIn(0f, 100f)
    }

    // Main 60FPS update tick
    fun update(dt: Float) {
        if (!isRunning || isPaused || isGameOver) return

        // Advance match timer
        timeRemaining -= dt
        matchTimeElapsed += dt

        if (timeRemaining <= 0f) {
            timeRemaining = 0f
            finishMatch()
            return
        }

        // Current phase within 2-minute match (0-20s, 20-40s, 40-60s, 60-80s, 80-100s, 100-120s)
        val phase = DifficultyPhase.fromMatchSeconds(matchTimeElapsed)

        // Spawning logic
        spawnTimer -= dt
        val adjustedSpawnInterval = (levelConfig.spawnInterval / phase.spawnRateMultiplier).coerceAtLeast(0.35f)
        val maxBubbles = (levelConfig.maxConcurrentBubbles * phase.spawnRateMultiplier * 0.75f).toInt().coerceIn(3, 12)

        if (spawnTimer <= 0f && activeBubbles.count { !it.isPopped && !it.isMissed } < maxBubbles) {
            spawnTimer = adjustedSpawnInterval
            spawnBubble(phase)
        }

        // Update active bubbles
        val speedMultiplier = phase.speedMultiplier
        val iterator = activeBubbles.iterator()
        while (iterator.hasNext()) {
            val bubble = iterator.next()

            if (bubble.isPopped) {
                bubble.popProgress += dt * 5.0f // fast burst
                if (bubble.popProgress >= 1.0f) {
                    iterator.remove()
                }
            } else if (bubble.isMissed) {
                bubble.missedProgress += dt * 4.0f
                if (bubble.missedProgress >= 1.0f) {
                    iterator.remove()
                }
            } else {
                // Fall downwards
                bubble.y += bubble.speed * speedMultiplier * dt
                bubble.rotation += bubble.rotationSpeed * dt

                // Check bottom threshold (y >= 0.95f)
                if (bubble.y >= 0.94f) {
                    handleBubbleMissed(bubble)
                }
            }
        }

        // Update particles
        val pIter = activeParticles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt / p.maxLife
            if (p.life <= 0f) {
                pIter.remove()
            }
        }

        // Update floating texts
        val tIter = activeFloatingTexts.iterator()
        while (tIter.hasNext()) {
            val ft = tIter.next()
            ft.y += (ft.vy * 0.0015f) * dt
            ft.life -= dt / ft.maxLife
            if (ft.life <= 0f) {
                tIter.remove()
            }
        }

        // Decay screen effects
        if (screenShake > 0f) {
            screenShake = (screenShake - dt * 4.5f).coerceAtLeast(0f)
        }
        if (redVignetteFlash > 0f) {
            redVignetteFlash = (redVignetteFlash - dt * 3.5f).coerceAtLeast(0f)
        }

        emitHud()
    }

    private fun spawnBubble(phase: DifficultyPhase) {
        val letter = levelConfig.letterPool.random()
        val radius = levelConfig.bubbleRadiusDp * phase.bubbleSizeMultiplier

        // Determine X coordinate based on pattern to prevent unplayable overlapping
        val xPos = getNextSpawnX(levelConfig.spawnPattern)

        val newBubble = Bubble(
            id = nextBubbleId++,
            letter = letter,
            x = xPos,
            y = -0.06f, // Start slightly above screen
            baseRadiusDp = radius,
            speed = levelConfig.baseSpeed * (0.9f + Random.nextFloat() * 0.2f)
        )
        activeBubbles.add(newBubble)
        totalSpawned++
    }

    private fun getNextSpawnX(pattern: SpawnPattern): Float {
        patternCounter++
        val minX = 0.12f
        val maxX = 0.88f

        return when (pattern) {
            SpawnPattern.LANES -> {
                // 5 distinct lanes: 0.15, 0.32, 0.50, 0.68, 0.85
                val lanes = floatArrayOf(0.16f, 0.33f, 0.50f, 0.67f, 0.84f)
                lanes[patternCounter % lanes.size]
            }
            SpawnPattern.LEFT_RIGHT_ALTERNATE -> {
                if (patternCounter % 2 == 0) {
                    (minX..0.45f).random()
                } else {
                    (0.55f..maxX).random()
                }
            }
            SpawnPattern.CENTER_HEAVY -> {
                val r = Random.nextFloat()
                if (r < 0.65f) (0.35f..0.65f).random() else (minX..maxX).random()
            }
            SpawnPattern.WAVE -> {
                val wave = (sin(patternCounter * 0.8) * 0.35 + 0.5).toFloat()
                wave.coerceIn(minX, maxX)
            }
            SpawnPattern.BURST_DUO, SpawnPattern.ZIGZAG, SpawnPattern.RANDOM -> {
                // Pick random X that avoids recent X to reduce clustering
                var candidate = (minX..maxX).random()
                if (kotlin.math.abs(candidate - recentSpawnX) < 0.15f) {
                    candidate = if (candidate > 0.5f) candidate - 0.25f else candidate + 0.25f
                }
                recentSpawnX = candidate.coerceIn(minX, maxX)
                recentSpawnX
            }
        }
    }

    private fun spawnBubblePopParticles(bubble: Bubble) {
        if (activeParticles.size >= maxParticleCount) return
        val count = if (maxParticleCount <= 50) 8 else 14
        val centerX = bubble.getEffectiveX(matchTimeElapsed)
        val centerY = bubble.y

        for (i in 0 until count) {
            val angle = (i.toFloat() / count) * 6.283f + Random.nextFloat() * 0.5f
            val speed = 0.15f + Random.nextFloat() * 0.35f
            activeParticles.add(
                Particle(
                    x = centerX,
                    y = centerY,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed - 0.05f,
                    color = bubble.color,
                    radius = (2.5f..5.5f).random(),
                    life = 1.0f,
                    maxLife = 0.45f + Random.nextFloat() * 0.25f,
                    type = ParticleType.SPARK
                )
            )
        }
    }

    private fun spawnBottomSplashParticles(bubble: Bubble) {
        if (activeParticles.size >= maxParticleCount) return
        val count = 8
        val centerX = bubble.getEffectiveX(matchTimeElapsed)

        for (i in 0 until count) {
            val vx = (-0.25f..0.25f).random()
            val vy = (-0.30f..-0.08f).random() // Burst upwards from bottom
            activeParticles.add(
                Particle(
                    x = centerX,
                    y = 0.94f,
                    vx = vx,
                    vy = vy,
                    color = BubbleOrange,
                    radius = (2f..4.5f).random(),
                    life = 1.0f,
                    maxLife = 0.4f,
                    type = ParticleType.SPLASH
                )
            )
        }
    }

    private fun finishMatch() {
        isRunning = false
        isGameOver = true
        audioPlayer.stopBackgroundMusic()

        val accuracy = calculateAccuracy()
        val stars = when {
            accuracy >= 94f && score >= levelConfig.targetScoreForStar3 -> 3
            accuracy >= 80f && score >= levelConfig.targetScoreForStar2 -> 2
            score > 0 -> 1
            else -> 0
        }

        val prevHighScore = saveManager.getBestScoreForLevel(currentLevel)
        val isNewHigh = score > prevHighScore

        val matchResult = MatchResult(
            level = currentLevel,
            score = score,
            correctCount = correctCount,
            wrongCount = wrongCount,
            missedCount = missedCount,
            totalSpawned = totalSpawned,
            highestCombo = highestCombo,
            accuracy = accuracy,
            matchDurationSeconds = matchDurationSeconds,
            stars = stars,
            isNewHighScore = isNewHigh,
            nextLevelUnlocked = false // will be determined by saveManager
        )

        val unlockedNext = saveManager.saveMatchResult(matchResult)
        val finalResult = matchResult.copy(nextLevelUnlocked = unlockedNext)

        audioPlayer.playLevelComplete()
        emitHud()
        onMatchFinished?.invoke(finalResult)
    }

    private fun emitHud() {
        _hudState.value = EngineHudState(
            remainingSeconds = timeRemaining,
            score = score,
            combo = combo,
            comboMultiplier = getComboMultiplier(combo),
            highestCombo = highestCombo,
            correctCount = correctCount,
            wrongCount = wrongCount,
            missedCount = missedCount,
            accuracy = calculateAccuracy(),
            level = currentLevel,
            phase = DifficultyPhase.fromMatchSeconds(matchTimeElapsed),
            isGameOver = isGameOver,
            isPaused = isPaused,
            screenShake = screenShake,
            redVignetteFlash = redVignetteFlash
        )
    }

    private fun vibrate(millis: Long) {
        val stats = saveManager.loadLifetimeStats()
        if (!stats.vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(millis)
            }
        } catch (_: Exception) {}
    }
}

private fun ClosedFloatingPointRange<Float>.random(): Float =
    start + (Random.nextFloat() * (endInclusive - start))
