package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.bubbletype.audio.BubbleAudioPlayer
import com.example.bubbletype.data.SaveManager
import com.example.bubbletype.engine.BubbleTypeEngine
import com.example.bubbletype.model.Bubble
import com.example.bubbletype.model.DifficultyPhase
import com.example.bubbletype.model.LevelGenerator
import com.example.bubbletype.model.MatchResult
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Bubble Type", appName)
  }

  @Test
  fun `level generator supports 500 levels with progression`() {
    val lvl1 = LevelGenerator.getLevelConfig(1)
    val lvl500 = LevelGenerator.getLevelConfig(500)

    assertEquals(1, lvl1.level)
    assertEquals(500, lvl500.level)

    // Higher level has faster speed and smaller bubbles
    assertTrue("Level 500 should be faster than Level 1", lvl500.baseSpeed > lvl1.baseSpeed)
    assertTrue("Level 500 bubble size should be smaller", lvl500.bubbleRadiusDp < lvl1.bubbleRadiusDp)
    assertTrue("Level 500 spawn interval should be faster", lvl500.spawnInterval < lvl1.spawnInterval)

    // Verify all 500 levels generate safely
    for (lvl in 1..500) {
      val config = LevelGenerator.getLevelConfig(lvl)
      assertTrue(config.letterPool.isNotEmpty())
      assertTrue(config.maxConcurrentBubbles >= 3)
    }
  }

  @Test
  fun `difficulty phases scale dynamically across 2 minute match`() {
    assertEquals(DifficultyPhase.PHASE_1, DifficultyPhase.fromMatchSeconds(0f))
    assertEquals(DifficultyPhase.PHASE_1, DifficultyPhase.fromMatchSeconds(19.9f))
    assertEquals(DifficultyPhase.PHASE_2, DifficultyPhase.fromMatchSeconds(20f))
    assertEquals(DifficultyPhase.PHASE_3, DifficultyPhase.fromMatchSeconds(40f))
    assertEquals(DifficultyPhase.PHASE_4, DifficultyPhase.fromMatchSeconds(60f))
    assertEquals(DifficultyPhase.PHASE_5, DifficultyPhase.fromMatchSeconds(80f))
    assertEquals(DifficultyPhase.PHASE_6, DifficultyPhase.fromMatchSeconds(100f))
    assertEquals(DifficultyPhase.PHASE_6, DifficultyPhase.fromMatchSeconds(119.9f))
  }

  @Test
  fun `engine letter matching, combos, and wrong typing logic`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val saveManager = SaveManager(context)
    val audioPlayer = BubbleAudioPlayer().apply {
      soundEnabled = false
      musicEnabled = false
    }
    val engine = BubbleTypeEngine(context, saveManager, audioPlayer)

    engine.startMatch()
    assertTrue(engine.isRunning)

    // Manually add test bubbles
    val bubbleA = Bubble(id = 1, letter = 'A', x = 0.3f, y = 0.5f, speed = 0.1f)
    val bubbleB = Bubble(id = 2, letter = 'B', x = 0.7f, y = 0.2f, speed = 0.1f)
    engine.activeBubbles.clear()
    engine.activeBubbles.add(bubbleA)
    engine.activeBubbles.add(bubbleB)

    // Test correct typing 'a' (case-insensitive)
    engine.onCharTyped('a')
    assertEquals(1, engine.correctCount)
    assertEquals(1, engine.combo)
    assertEquals(1, engine.score)
    assertTrue(bubbleA.isPopped)

    // Test correct typing 'B'
    engine.onCharTyped('B')
    assertEquals(2, engine.correctCount)
    assertEquals(2, engine.combo)
    assertEquals(2, engine.score)
    assertTrue(bubbleB.isPopped)

    // Test wrong typing 'Z' (not on screen)
    engine.onCharTyped('z')
    assertEquals(1, engine.wrongCount)
    assertEquals(0, engine.combo) // Breaks combo
    assertEquals(1, engine.score) // Subtracts 1 point

    // Test accuracy formula
    // Correct: 2, Wrong: 1, Missed: 0 -> 2 / 3 * 100 = 66.6%
    val accuracy = engine.calculateAccuracy()
    assertEquals(66.66f, accuracy, 1.0f)
  }

  @Test
  fun `save manager persists unlocked level and stats`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val saveManager = SaveManager(context)

    val testResult = MatchResult(
      level = 1,
      score = 45,
      correctCount = 45,
      wrongCount = 2,
      missedCount = 1,
      totalSpawned = 48,
      highestCombo = 15,
      accuracy = 93.75f,
      stars = 3
    )

    val unlockedNext = saveManager.saveMatchResult(testResult)
    assertTrue("Completing level 1 should unlock level 2", unlockedNext)
    assertTrue(saveManager.getUnlockedLevel() >= 2)
    assertEquals(45, saveManager.getBestScoreForLevel(1))
    assertEquals(3, saveManager.getStarsForLevel(1))
  }
}
