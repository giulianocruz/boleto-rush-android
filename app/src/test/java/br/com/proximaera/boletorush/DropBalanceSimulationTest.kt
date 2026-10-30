package br.com.proximaera.boletorush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.sin

/** Deterministic simulation of complete Classic and Marathon drop schedules. */
class DropBalanceSimulationTest {
    @Test fun severalFullRoundsRemainFairAndContainEnoughCoins() {
        for (duration in listOf(45f, 90f)) {
            for (seed in 0L..99L) {
                val rng = Random(seed + duration.toLong() * 100)
                var elapsed = 0f
                var nonCoinStreak = 0
                var previousHazard = false
                var coinCount = 0
                var totalDrops = 0
                val chapters = mutableSetOf<Int>()
                while (elapsed < duration) {
                    val player = (.5f + .30f * sin(elapsed.toDouble()).toFloat())
                        .coerceIn(.09f, .91f)
                    val kind = GameDrops.choose(
                        rng.nextFloat(), rng.nextFloat(), elapsed, duration,
                        8, previousHazard, nonCoinStreak
                    )
                    chapters += GamePacing.chapter(elapsed,duration)
                    val x = GameDrops.spawnX(rng.nextFloat(),kind,player)
                    assertTrue("Spawn out of bounds", x in .09f..91f)
                    if (GameDrops.harmful(kind)) {
                        assertFalse("Consecutive hazards", previousHazard)
                        assertTrue("Unfair hazard spawn", kotlin.math.abs(x-player) >= .179f)
                        assertTrue("Hazard during onboarding", elapsed >= 5f)
                    }
                    if (kind == Kind.COIN) {
                        coinCount++
                        nonCoinStreak = 0
                    } else {
                        nonCoinStreak++
                        assertTrue("Too many drops without coins", nonCoinStreak <= 3)
                    }
                    previousHazard = GameDrops.harmful(kind)
                    totalDrops++
                    elapsed += GamePacing.spawnInterval(elapsed,duration,8)
                }
                assertEquals("All four stages should be visited",4,chapters.size)
                assertTrue("Coins were too sparse",coinCount >= (duration / 4.5f).toInt())
                assertTrue("Round too sparse",totalDrops >= (duration * .9f).toInt())
            }
        }
    }
}
