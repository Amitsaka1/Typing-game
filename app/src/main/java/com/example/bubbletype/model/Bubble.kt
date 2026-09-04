package com.example.bubbletype.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BubblePalette

enum class ParticleType {
    SPARK,
    BUBBLE_SHARD,
    STAR,
    SPLASH
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val radius: Float,
    var life: Float = 1.0f,
    val maxLife: Float = 0.6f,
    val type: ParticleType = ParticleType.SPARK
)

data class FloatingText(
    var x: Float,
    var y: Float,
    val text: String,
    val color: Color,
    var life: Float = 1.0f,
    val maxLife: Float = 0.8f,
    val vy: Float = -70f
)

data class Bubble(
    val id: Long,
    val letter: Char,
    var x: Float, // Normalized 0f..1f across playable width
    var y: Float, // Normalized 0f..1f (top to bottom)
    val baseRadiusDp: Float = 36f,
    val speed: Float, // Fall speed per second in normalized Y
    val colorIndex: Int = (0 until BubblePalette.size).random(),
    val wobblePhase: Float = (0f..6.28f).random(),
    val wobbleSpeed: Float = (1.5f..3.0f).random(),
    val wobbleAmplitude: Float = 0.015f,
    var rotation: Float = 0f,
    val rotationSpeed: Float = (-25f..25f).random(),
    var isPopped: Boolean = false,
    var popProgress: Float = 0f, // 0f to 1f
    var isMissed: Boolean = false,
    var missedProgress: Float = 0f
) {
    val color: Color
        get() = BubblePalette[colorIndex % BubblePalette.size]

    // Returns effective normalized X considering sine wobble
    fun getEffectiveX(time: Float): Float {
        val wobble = kotlin.math.sin(time * wobbleSpeed + wobblePhase) * wobbleAmplitude
        return (x + wobble).coerceIn(0.08f, 0.92f)
    }
}

private fun ClosedFloatingPointRange<Float>.random(): Float =
    start + (Math.random().toFloat() * (endInclusive - start))
