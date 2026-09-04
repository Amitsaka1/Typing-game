package com.example.bubbletype.model

enum class SpawnPattern {
    RANDOM,
    LANES,
    LEFT_RIGHT_ALTERNATE,
    WAVE,
    CENTER_HEAVY,
    BURST_DUO,
    ZIGZAG
}

enum class DifficultyPhase(
    val phaseNumber: Int,
    val nameTitle: String,
    val description: String,
    val speedMultiplier: Float,
    val spawnRateMultiplier: Float,
    val bubbleSizeMultiplier: Float
) {
    PHASE_1(1, "Easy", "Slow & Large Bubbles", 1.0f, 1.0f, 1.15f),
    PHASE_2(2, "Normal", "Steady Flow", 1.25f, 1.25f, 1.05f),
    PHASE_3(3, "Brisk", "Faster Cadence", 1.55f, 1.55f, 0.95f),
    PHASE_4(4, "Rapid", "High Velocity", 1.9f, 1.85f, 0.88f),
    PHASE_5(5, "Frenzy", "Multitasking Speed", 2.3f, 2.2f, 0.80f),
    PHASE_6(6, "OVERDRIVE", "Maximum Arcade Density", 2.75f, 2.6f, 0.72f);

    companion object {
        fun fromMatchSeconds(elapsedSeconds: Float): DifficultyPhase {
            return when {
                elapsedSeconds < 20f -> PHASE_1
                elapsedSeconds < 40f -> PHASE_2
                elapsedSeconds < 60f -> PHASE_3
                elapsedSeconds < 80f -> PHASE_4
                elapsedSeconds < 100f -> PHASE_5
                else -> PHASE_6
            }
        }
    }
}

data class LevelConfig(
    val level: Int,
    val baseSpeed: Float, // Normalized Y per second
    val spawnInterval: Float, // Seconds between spawns
    val maxConcurrentBubbles: Int,
    val bubbleRadiusDp: Float,
    val letterPool: String,
    val spawnPattern: SpawnPattern,
    val targetScoreForStar3: Int,
    val targetScoreForStar2: Int
)

object LevelGenerator {
    // Letter pools progressing from easiest home-row / common to full tricky alphabet
    private const val COMMON_LETTERS = "ETAOINSHRDLCUMWFGYPB"
    private const val HOME_ROW = "ASDFGHJKLQWERTYUIOP"
    private const val FULL_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val TRICKY_LETTERS = "QZXJKVBPYW"

    fun getLevelConfig(level: Int): LevelConfig {
        val clampedLevel = level.coerceIn(1, 500)
        val progress = (clampedLevel - 1) / 499.0f // 0.0 at level 1, 1.0 at level 500

        // Base fall speed: level 1 is gentle (0.10f = ~10 seconds to fall), level 500 is intense (0.35f = ~2.8s to fall)
        val baseSpeed = 0.09f + progress * 0.26f

        // Spawn interval: level 1 is spacious (~1.8s), level 500 is rapid (~0.55s)
        val spawnInterval = (1.75f - progress * 1.15f).coerceAtLeast(0.50f)

        // Max simultaneous bubbles on screen: 3 at lvl 1 up to 10 at lvl 500
        val maxBubbles = (3 + (progress * 7).toInt()).coerceIn(3, 10)

        // Bubble radius: 42dp at level 1, down to 32dp at level 500
        val bubbleRadiusDp = 42f - progress * 10f

        // Letter pool distribution
        val letterPool = when {
            clampedLevel <= 10 -> "AEIOURSTLN"
            clampedLevel <= 30 -> COMMON_LETTERS
            clampedLevel <= 100 -> HOME_ROW + COMMON_LETTERS
            clampedLevel <= 250 -> FULL_ALPHABET
            clampedLevel <= 400 -> FULL_ALPHABET + TRICKY_LETTERS
            else -> FULL_ALPHABET + TRICKY_LETTERS + "QZXJK"
        }

        // Spawn pattern rotation across levels
        val pattern = when ((clampedLevel - 1) % 7) {
            0 -> SpawnPattern.RANDOM
            1 -> SpawnPattern.LEFT_RIGHT_ALTERNATE
            2 -> SpawnPattern.LANES
            3 -> SpawnPattern.CENTER_HEAVY
            4 -> SpawnPattern.WAVE
            5 -> SpawnPattern.BURST_DUO
            else -> SpawnPattern.ZIGZAG
        }

        // Star score benchmarks for 2-minute match
        val expectedSpawns = (120f / spawnInterval).toInt()
        val targetStar2 = (expectedSpawns * 0.55f).toInt().coerceAtLeast(20)
        val targetStar3 = (expectedSpawns * 0.85f).toInt().coerceAtLeast(35)

        return LevelConfig(
            level = clampedLevel,
            baseSpeed = baseSpeed,
            spawnInterval = spawnInterval,
            maxConcurrentBubbles = maxBubbles,
            bubbleRadiusDp = bubbleRadiusDp,
            letterPool = letterPool,
            spawnPattern = pattern,
            targetScoreForStar3 = targetStar3,
            targetScoreForStar2 = targetStar2
        )
    }
}
