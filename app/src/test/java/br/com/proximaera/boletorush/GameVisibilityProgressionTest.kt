package br.com.proximaera.boletorush

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class GameVisibilityProgressionTest {
    @Test fun coinCannotScoreWithoutCrossingVisibleWalletLane() {
        assertFalse(GamePickup.coinContact(.15f, .81f, .50f, .50f, false))
        assertFalse(GamePickup.coinContact(.55f, .71f, .50f, .50f, false))
        assertFalse(GamePickup.coinContact(.78f, .81f, .12f, .50f, false))
        assertFalse(GamePickup.coinContact(.90f, .96f, .50f, .50f, true))
        assertFalse(GamePickup.coinContact(.78f, .82f, Float.NaN, .50f, false))
        assertTrue(GamePickup.coinContact(.79f, .81f, .50f, .50f, false))
        assertTrue(GamePickup.coinContact(.73f, .77f, .50f, .50f, true))
    }

    @Test fun slowRampIsRelativeToRoundDurationNotRawSeconds() {
        for (duration in listOf(45f, 90f)) {
            val start = GamePacing.dropSpeed(0f,duration,0,.50f)
            val middle = GamePacing.dropSpeed(duration*.50f,duration,0,.50f)
            val end = GamePacing.dropSpeed(duration,duration,0,.50f)
            assertTrue(middle > start)
            assertTrue(end > middle)
            assertTrue(end - start < .05f)
            assertTrue(GamePacing.spawnInterval(0f,duration,0) -
                GamePacing.spawnInterval(duration,duration,0) <= .101f)
            assertTrue(GamePacing.billOdds(duration,duration,20) <= .185f)
        }
        assertEquals(GamePacing.dropSpeed(45f,45f,0,.5f),
            GamePacing.dropSpeed(90f,90f,0,.5f),.0001f)
    }

    @Test fun coinGenerationNeverHasMoreThanTwoNonCoinsBeforeNextCoin() {
        for (duration in listOf(45f,90f)) {
            for (rollStep in 0..100) {
                val roll = rollStep / 100f
                val kind = GameDrops.choose(roll, .99f,
                    duration*.75f, duration, 0, false, 2)
                assertEquals(Kind.COIN,kind)
            }
        }
    }

    @Test fun existingCurrencyRulesStaySeparate() {
        assertEquals(3,GameEconomy.earnings(15,0))
        assertEquals(5,GameEconomy.earnings(15,2))
        assertEquals(10,GameDrops.coinScore(1,false))
        assertEquals(20,GameDrops.coinScore(1,true))
        assertFalse(GameDrops.harmful(Kind.BANKNOTE))
        assertTrue(GameDrops.harmful(Kind.OVERDRAFT))
        assertTrue(GameDrops.harmful(Kind.BILL))
    }
}
