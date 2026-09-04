package com.example.bubbletype.data

import android.content.Context
import android.content.SharedPreferences
import com.example.bubbletype.model.LifetimeStats
import com.example.bubbletype.model.MatchResult

class SaveManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bubble_type_save", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_UNLOCKED_LEVEL = "unlocked_level"
        private const val KEY_HIGHEST_SCORE = "highest_score"
        private const val KEY_HIGHEST_COMBO = "highest_combo"
        private const val KEY_BEST_ACCURACY = "best_accuracy"
        private const val KEY_TOTAL_GAMES = "total_games_played"
        private const val KEY_TOTAL_CORRECT = "total_correct"
        private const val KEY_TOTAL_WRONG = "total_wrong"
        private const val KEY_TOTAL_MISSED = "total_missed"

        private const val KEY_SOUND_ENABLED = "setting_sound"
        private const val KEY_MUSIC_ENABLED = "setting_music"
        private const val KEY_VIBRATION_ENABLED = "setting_vibration"
        private const val KEY_PARTICLE_QUALITY = "setting_particle_quality"
        private const val KEY_ANIMATION_QUALITY = "setting_animation_quality"

        private const val PREFIX_LEVEL_SCORE = "lvl_score_"
        private const val PREFIX_LEVEL_STARS = "lvl_stars_"
    }

    fun getUnlockedLevel(): Int {
        return prefs.getInt(KEY_UNLOCKED_LEVEL, 1).coerceIn(1, 500)
    }

    fun setUnlockedLevel(level: Int) {
        val current = getUnlockedLevel()
        if (level > current) {
            prefs.edit().putInt(KEY_UNLOCKED_LEVEL, level.coerceIn(1, 500)).apply()
        }
    }

    fun getBestScoreForLevel(level: Int): Int {
        return prefs.getInt(PREFIX_LEVEL_SCORE + level, 0)
    }

    fun getStarsForLevel(level: Int): Int {
        return prefs.getInt(PREFIX_LEVEL_STARS + level, 0)
    }

    fun saveMatchResult(result: MatchResult): Boolean {
        var unlockedNext = false
        val editor = prefs.edit()

        // Update Level High Score
        val prevScore = getBestScoreForLevel(result.level)
        if (result.score > prevScore) {
            editor.putInt(PREFIX_LEVEL_SCORE + result.level, result.score)
        }

        // Update Level Stars
        val prevStars = getStarsForLevel(result.level)
        if (result.stars > prevStars) {
            editor.putInt(PREFIX_LEVEL_STARS + result.level, result.stars)
        }

        // Level Unlock Progression: Completing match with score > 0 unlocks next level!
        if (result.score >= 0 && result.level == getUnlockedLevel() && result.level < 500) {
            val nextLvl = result.level + 1
            editor.putInt(KEY_UNLOCKED_LEVEL, nextLvl)
            unlockedNext = true
        }

        // Update Lifetime Records
        val totalGames = prefs.getInt(KEY_TOTAL_GAMES, 0) + 1
        editor.putInt(KEY_TOTAL_GAMES, totalGames)

        val totalCorrect = prefs.getInt(KEY_TOTAL_CORRECT, 0) + result.correctCount
        editor.putInt(KEY_TOTAL_CORRECT, totalCorrect)

        val totalWrong = prefs.getInt(KEY_TOTAL_WRONG, 0) + result.wrongCount
        editor.putInt(KEY_TOTAL_WRONG, totalWrong)

        val totalMissed = prefs.getInt(KEY_TOTAL_MISSED, 0) + result.missedCount
        editor.putInt(KEY_TOTAL_MISSED, totalMissed)

        val highestScore = maxOf(prefs.getInt(KEY_HIGHEST_SCORE, 0), result.score)
        editor.putInt(KEY_HIGHEST_SCORE, highestScore)

        val highestCombo = maxOf(prefs.getInt(KEY_HIGHEST_COMBO, 0), result.highestCombo)
        editor.putInt(KEY_HIGHEST_COMBO, highestCombo)

        val bestAccuracy = maxOf(prefs.getFloat(KEY_BEST_ACCURACY, 0f), result.accuracy)
        editor.putFloat(KEY_BEST_ACCURACY, bestAccuracy)

        editor.apply()
        return unlockedNext
    }

    fun loadLifetimeStats(): LifetimeStats {
        return LifetimeStats(
            unlockedLevel = getUnlockedLevel(),
            highestScore = prefs.getInt(KEY_HIGHEST_SCORE, 0),
            highestCombo = prefs.getInt(KEY_HIGHEST_COMBO, 0),
            bestAccuracy = prefs.getFloat(KEY_BEST_ACCURACY, 0f),
            totalGamesPlayed = prefs.getInt(KEY_TOTAL_GAMES, 0),
            totalCorrect = prefs.getInt(KEY_TOTAL_CORRECT, 0),
            totalWrong = prefs.getInt(KEY_TOTAL_WRONG, 0),
            totalMissed = prefs.getInt(KEY_TOTAL_MISSED, 0),
            soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true),
            musicEnabled = prefs.getBoolean(KEY_MUSIC_ENABLED, true),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, true),
            particleQuality = prefs.getString(KEY_PARTICLE_QUALITY, "HIGH") ?: "HIGH",
            animationQuality = prefs.getString(KEY_ANIMATION_QUALITY, "HIGH") ?: "HIGH"
        )
    }

    fun updateSettings(
        sound: Boolean,
        music: Boolean,
        vibration: Boolean,
        particleQuality: String,
        animationQuality: String
    ) {
        prefs.edit()
            .putBoolean(KEY_SOUND_ENABLED, sound)
            .putBoolean(KEY_MUSIC_ENABLED, music)
            .putBoolean(KEY_VIBRATION_ENABLED, vibration)
            .putString(KEY_PARTICLE_QUALITY, particleQuality)
            .putString(KEY_ANIMATION_QUALITY, animationQuality)
            .apply()
    }

    fun resetAllProgress() {
        prefs.edit().clear().apply()
    }
}
