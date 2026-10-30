package br.com.proximaera.boletorush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PacingAndEconomyTest {
    @Test fun pressureStartsLowAndRampsGradually() {
        val early = GamePacing.billOdds(0f,45f,0)
        val middle = GamePacing.billOdds(22.5f,45f,0)
        val late = GamePacing.billOdds(45f,45f,0)
        assertTrue(early < middle && middle < late)
        assertTrue(early < .10f && late < .26f)
        assertTrue(late - middle < .09f)
        assertTrue(GamePacing.spawnInterval(45f,45f,10) > .75f)
    }

    @Test fun progressionHasFourDifferentChapters() {
        assertEquals(0, GamePacing.chapter(0f,45f))
        assertEquals(1, GamePacing.chapter(12f,45f))
        assertEquals(2, GamePacing.chapter(25f,45f))
        assertEquals(3, GamePacing.chapter(40f,45f))
        assertEquals(3, GamePacing.chapter(45f,45f))
    }

    @Test fun magnetDoesNotScoreBeforeCoinsAreVisible() {
        assertFalse(GamePacing.magnetCanCollect(.13f))
        assertFalse(GamePacing.magnetCanCollect(.33f))
        assertTrue(GamePacing.magnetCanCollect(.41f))
    }

    @Test fun upgradeCurrencyRemainsScarceAndUpgradesHelp() {
        assertEquals(0, GameEconomy.earnings(0,0))
        assertEquals(3, GameEconomy.earnings(15,0))
        assertEquals(7, GameEconomy.earnings(15,3))
        assertTrue(GameEconomy.earnings(15,0) < 15)
    }

    @Test fun allCosmeticsCostOnlyCashExceptDefault() {
        assertEquals(0,Cosmetic.CLASSIC.cost)
        assertTrue(Cosmetic.entries.drop(1).all { it.cost > 0 })
        assertTrue(Cosmetic.entries.map { it.key }.distinct().size == Cosmetic.entries.size)
    }
}
