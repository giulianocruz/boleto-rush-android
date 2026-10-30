package br.com.proximaera.boletorush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameDropsTest {
    private fun choose(
        roll: Float, hazardRoll: Float = 0f, elapsed: Float = 35f,
        previousHazard: Boolean = false, nonCoins: Int = 0
    ) = GameDrops.choose(roll, hazardRoll, elapsed, 45f, 5, previousHazard, nonCoins)

    @Test fun openingSecondsAreSafeFromBillsAndOverdrafts() {
        assertEquals(Kind.COIN, choose(.9f, 0f, 0f))
        assertFalse(GameDrops.harmful(choose(.26f, 0f, 0f)))
    }

    @Test fun neverSpawnsBackToBackHazards() {
        assertEquals(Kind.BILL, choose(.90f,0f,35f))
        assertFalse(GameDrops.harmful(choose(.90f,0f,35f,true)))
        assertFalse(GameDrops.harmful(choose(.26f,0f,35f,true)))
    }

    @Test fun threeNonCoinsGuaranteeNextCoin() {
        assertEquals(Kind.COIN, choose(.03f,0f,35f,false,3))
        assertEquals(Kind.COIN, choose(.26f,0f,35f,false,3))
    }

    @Test fun financialItemsBecomeAvailableByChapter() {
        assertEquals(Kind.BANKNOTE,choose(.16f, .99f,0f))
        assertEquals(Kind.CARD,choose(.23f,.99f,14f))
        assertEquals(Kind.OVERDRAFT,choose(.26f,.99f,30f))
        assertEquals(Kind.PIX,choose(.30f,.99f,12f))
    }

    @Test fun pixDoublesOnlyCoinScoreNotWalletTokens() {
        assertEquals(10,GameDrops.coinScore(1,false))
        assertEquals(20,GameDrops.coinScore(1,true))
        assertEquals(28,GameDrops.coinScore(10,false))
        assertEquals(56,GameDrops.coinScore(10,true))
        assertEquals(3,GameEconomy.earnings(15,0))
        assertTrue(GameDrops.coinScore(12,true) > GameDrops.coinScore(12,false))
    }

    @Test fun everyGeneratedKindIsRecognized() {
        val drops = (0..999).map { i ->
            choose(i / 1000f, .9f,35f)
        }.toSet()
        assertTrue(drops.containsAll(listOf(Kind.COIN,Kind.BANKNOTE,Kind.PIX,
            Kind.CARD,Kind.OVERDRAFT,Kind.SHIELD,Kind.HEART,Kind.MAGNET)))
    }
}
