package com.example.bubbletype.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class BubbleAudioPlayer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sampleRate = 22050

    var soundEnabled: Boolean = true
    var musicEnabled: Boolean = true

    private var musicJob: Job? = null

    // Pre-calculated sound buffers
    private val popBuffer = generateTone(480f, 780f, 0.09f)
    private val wrongBuffer = generateBuzz(130f, 95f, 0.14f)
    private val missBuffer = generateTone(280f, 110f, 0.12f)

    fun playPop(combo: Int = 1) {
        if (!soundEnabled) return
        scope.launch {
            // Pitch scales up delightfully with higher combos!
            val pitchMultiplier = (1.0f + (combo.coerceAtMost(30) * 0.035f))
            val buffer = if (combo > 3) {
                generateChime(500f * pitchMultiplier, 850f * pitchMultiplier, 0.11f)
            } else {
                popBuffer
            }
            playPcm(buffer)
        }
    }

    fun playWrong() {
        if (!soundEnabled) return
        scope.launch {
            playPcm(wrongBuffer)
        }
    }

    fun playMiss() {
        if (!soundEnabled) return
        scope.launch {
            playPcm(missBuffer)
        }
    }

    fun playLevelComplete() {
        if (!soundEnabled) return
        scope.launch {
            val chordNotes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f) // C5, E5, G5, C6
            for (freq in chordNotes) {
                playPcm(generateTone(freq, freq * 1.05f, 0.15f, 0.5f))
                delay(90)
            }
        }
    }

    fun startBackgroundMusic() {
        if (!musicEnabled) return
        stopBackgroundMusic()
        musicJob = scope.launch {
            // Uplifting arcade bass/arpeggio loop
            val chords = listOf(
                floatArrayOf(261.63f, 329.63f, 392.00f), // C
                floatArrayOf(220.00f, 261.63f, 329.63f), // Am
                floatArrayOf(174.61f, 220.00f, 261.63f), // F
                floatArrayOf(196.00f, 246.94f, 293.66f)  // G
            )
            while (isActive && musicEnabled) {
                for (chord in chords) {
                    for (note in chord) {
                        if (!isActive || !musicEnabled) break
                        playPcm(generateTone(note, note * 0.99f, 0.22f, 0.16f))
                        delay(240)
                    }
                }
            }
        }
    }

    fun stopBackgroundMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    private fun playPcm(buffer: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            scope.launch {
                delay((buffer.size.toDouble() / sampleRate * 1000).toLong() + 50)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun generateTone(startFreq: Float, endFreq: Float, durationSec: Float, volume: Float = 0.45f): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val freq = startFreq + (endFreq - startFreq) * t
            val envelope = exp(-3.5 * t) * volume
            phase += 2.0 * PI * freq / sampleRate
            val sample = sin(phase) * envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateChime(startFreq: Float, endFreq: Float, durationSec: Float): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        var phase1 = 0.0
        var phase2 = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val freq = startFreq + (endFreq - startFreq) * t
            val envelope = exp(-4.0 * t) * 0.45
            phase1 += 2.0 * PI * freq / sampleRate
            phase2 += 2.0 * PI * (freq * 1.5) / sampleRate // 5th harmonic
            val sample = (sin(phase1) * 0.7 + sin(phase2) * 0.3) * envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateBuzz(startFreq: Float, endFreq: Float, durationSec: Float): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val freq = startFreq + (endFreq - startFreq) * t
            val envelope = exp(-2.5 * t) * 0.40
            phase += 2.0 * PI * freq / sampleRate
            // Square-ish wave for buzz
            val sample = if (sin(phase) > 0) envelope else -envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    fun release() {
        stopBackgroundMusic()
        scope.cancel()
    }
}
