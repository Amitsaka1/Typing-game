package com.example.bubbletype.model

data class MatchResult(
    val level: Int,
    val score: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val missedCount: Int,
    val totalSpawned: Int,
    val highestCombo: Int,
    val accuracy: Float,
    val matchDurationSeconds: Float = 120f,
    val stars: Int,
    val isNewHighScore: Boolean = false,
    val nextLevelUnlocked: Boolean = false
) {
    // Words Per Minute estimated at 5 chars per word
    val estimatedWpm: Int
        get() = ((correctCount / 5f) / (matchDurationSeconds / 60f)).toInt().coerceAtLeast(0)

    val charsPerMinute: Int
        get() = (correctCount / (matchDurationSeconds / 60f)).toInt().coerceAtLeast(0)
}

data class LifetimeStats(
    val unlockedLevel: Int = 1,
    val highestScore: Int = 0,
    val highestCombo: Int = 0,
    val bestAccuracy: Float = 0f,
    val totalGamesPlayed: Int = 0,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val totalMissed: Int = 0,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val particleQuality: String = "HIGH", // HIGH, MEDIUM, LOW
    val animationQuality: String = "HIGH"
)
