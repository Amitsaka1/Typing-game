package com.example.bubbletype.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.bubbletype.engine.BubbleTypeEngine
import com.example.bubbletype.model.Bubble
import com.example.bubbletype.model.Particle
import com.example.bubbletype.model.ParticleType
import com.example.ui.theme.BubbleDarkBg
import com.example.ui.theme.BubbleDarkBgEnd
import com.example.ui.theme.BubbleRed
import kotlin.random.Random

object BubbleCanvasRenderer {

    private val textPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val floatingTextPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    fun render(
        drawScope: DrawScope,
        engine: BubbleTypeEngine,
        width: Float,
        height: Float
    ) {
        val density = drawScope.density

        // Apply screen shake offset if active
        val shakeOffset = if (engine.screenShake > 0f) {
            val mag = engine.screenShake * 16f * density
            Offset(
                (Random.nextFloat() - 0.5f) * mag,
                (Random.nextFloat() - 0.5f) * mag
            )
        } else {
            Offset.Zero
        }

        // Draw arcade background gradient
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    BubbleDarkBg,
                    Color(0xFF140C2E),
                    BubbleDarkBgEnd
                )
            ),
            topLeft = Offset.Zero,
            size = Size(width, height)
        )

        // Draw subtle bottom danger/deadline indicator line
        val deadlineY = height * 0.94f
        drawScope.drawLine(
            color = Color(0x40FF5252),
            start = Offset(0f, deadlineY),
            end = Offset(width, deadlineY),
            strokeWidth = 2f * density
        )

        // Render Bubbles
        val currentTime = engine.matchTimeElapsed
        for (bubble in engine.activeBubbles) {
            renderBubble(drawScope, bubble, width, height, density, currentTime, shakeOffset)
        }

        // Render Particles
        for (p in engine.activeParticles) {
            renderParticle(drawScope, p, width, height, density, shakeOffset)
        }

        // Render Floating Score/Penalty Texts
        for (ft in engine.activeFloatingTexts) {
            val alpha = (ft.life / ft.maxLife).coerceIn(0f, 1f)
            val px = ft.x * width + shakeOffset.x
            val py = ft.y * height + shakeOffset.y
            floatingTextPaint.color = ft.color.copy(alpha = alpha).toArgb()
            floatingTextPaint.textSize = 20f * density
            floatingTextPaint.setShadowLayer(6f * density, 0f, 2f * density, 0x80000000.toInt())

            drawScope.drawContext.canvas.nativeCanvas.drawText(
                ft.text,
                px,
                py,
                floatingTextPaint
            )
        }

        // Render Red Flash Vignette on wrong typing
        if (engine.redVignetteFlash > 0f) {
            val flashAlpha = (engine.redVignetteFlash * 0.35f).coerceIn(0f, 0.4f)
            drawScope.drawRect(
                color = BubbleRed.copy(alpha = flashAlpha),
                topLeft = Offset.Zero,
                size = Size(width, height)
            )
        }
    }

    private fun renderBubble(
        drawScope: DrawScope,
        bubble: Bubble,
        width: Float,
        height: Float,
        density: Float,
        currentTime: Float,
        shakeOffset: Offset
    ) {
        val cx = bubble.getEffectiveX(currentTime) * width + shakeOffset.x
        val cy = bubble.y * height + shakeOffset.y
        val baseRadiusPx = bubble.baseRadiusDp * density

        if (bubble.isPopped) {
            // Popping burst animation: expanding ring + shrinking center
            val progress = bubble.popProgress
            val ringRadius = baseRadiusPx * (1f + progress * 0.8f)
            val alpha = (1f - progress).coerceIn(0f, 1f)

            // Expanding shockwave ring
            drawScope.drawCircle(
                color = bubble.color.copy(alpha = alpha * 0.85f),
                radius = ringRadius,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = (4f * (1f - progress)) * density)
            )

            // Fading inner flash
            drawScope.drawCircle(
                color = Color.White.copy(alpha = alpha * 0.5f),
                radius = baseRadiusPx * (1f - progress * 0.5f),
                center = Offset(cx, cy)
            )
            return
        }

        if (bubble.isMissed) {
            // Missed animation: pop downwards with fizzle
            val progress = bubble.missedProgress
            val alpha = (1f - progress).coerceIn(0f, 1f)
            drawScope.drawCircle(
                color = Color(0xFFFF5252).copy(alpha = alpha * 0.7f),
                radius = baseRadiusPx * (1f + progress * 0.4f),
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * density)
            )
            return
        }

        // Draw normal active floating bubble
        // 1. Outer soft glow
        drawScope.drawCircle(
            color = bubble.color.copy(alpha = 0.25f),
            radius = baseRadiusPx * 1.15f,
            center = Offset(cx, cy)
        )

        // 2. Main glossy body with radial gradient (lighter highlight in top-left)
        val highlightOffset = Offset(cx - baseRadiusPx * 0.30f, cy - baseRadiusPx * 0.30f)
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    bubble.color.copy(alpha = 0.90f),
                    bubble.color.copy(alpha = 0.65f),
                    bubble.color.copy(alpha = 0.40f)
                ),
                center = highlightOffset,
                radius = baseRadiusPx * 1.25f
            ),
            radius = baseRadiusPx,
            center = Offset(cx, cy)
        )

        // 3. Crisp luminous border ring
        drawScope.drawCircle(
            color = bubble.color.copy(alpha = 0.95f),
            radius = baseRadiusPx,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f * density)
        )

        // 4. Specular gloss shine in top-left (soap bubble glass effect)
        val specularCenter = Offset(cx - baseRadiusPx * 0.35f, cy - baseRadiusPx * 0.35f)
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.65f),
            radius = baseRadiusPx * 0.24f,
            center = specularCenter
        )

        // Subtle secondary reflection in bottom-right
        val secReflectCenter = Offset(cx + baseRadiusPx * 0.32f, cy + baseRadiusPx * 0.32f)
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = baseRadiusPx * 0.14f,
            center = secReflectCenter
        )

        // 5. Letter Text in center
        val letterStr = bubble.letter.toString()
        val fontSizePx = baseRadiusPx * 1.05f
        textPaint.textSize = fontSizePx

        // Text shadow for maximum legibility
        textPaint.color = 0xCC000000.toInt()
        textPaint.setShadowLayer(4f * density, 0f, 2f * density, 0xAA000000.toInt())

        val yPos = cy - (textPaint.descent() + textPaint.ascent()) / 2f

        // Draw shadow/outline first
        drawScope.drawContext.canvas.nativeCanvas.drawText(
            letterStr,
            cx,
            yPos,
            textPaint
        )

        // Draw crisp bright white core letter
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.clearShadowLayer()
        drawScope.drawContext.canvas.nativeCanvas.drawText(
            letterStr,
            cx,
            yPos,
            textPaint
        )
    }

    private fun renderParticle(
        drawScope: DrawScope,
        p: Particle,
        width: Float,
        height: Float,
        density: Float,
        shakeOffset: Offset
    ) {
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        val px = p.x * width + shakeOffset.x
        val py = p.y * height + shakeOffset.y
        val r = p.radius * density * alpha

        when (p.type) {
            ParticleType.SPARK -> {
                drawScope.drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = r,
                    center = Offset(px, py)
                )
            }
            ParticleType.SPLASH -> {
                drawScope.drawCircle(
                    color = p.color.copy(alpha = alpha * 0.8f),
                    radius = r,
                    center = Offset(px, py)
                )
            }
            else -> {
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = r * 0.7f,
                    center = Offset(px, py)
                )
            }
        }
    }
}
