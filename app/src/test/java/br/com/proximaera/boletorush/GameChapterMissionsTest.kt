package br.com.proximaera.boletorush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ensures that chapter goals only award cosmetic notes, once per chapter. */
class GameChapterMissionsTest {
    @Test fun allChaptersHaveAchievableGoalsAndDistinctRewards() {
        assertEquals(listOf(2,3,3,4),(0..3).map(GameChapterMissions::goal))
        assertEquals(listOf(3,4,5,6),(0..3).map(GameChapterMissions::reward))
        assertEquals(4,(0..3).map(GameChapterMissions::title).toSet().size)
        assertEquals(18,(0..3).sumOf(GameChapterMissions::reward))
    }

    @Test fun eachMissionPaysOnceEvenAfterExtraCoinsAndRepeatedCallbacks() {
        var mask = 0
        var notes = 0
        for (chapter in 0..3) {
            var coins = 0
            assertFalse(GameChapterMissions.completed(chapter,mask))
            repeat(GameChapterMissions.goal(chapter)-1) {
                val result = GameChapterMissions.recordCoin(chapter, coins, mask)
                coins = result.coins
                mask = result.claimedMask
                notes += result.cosmeticNotes
                assertEquals(0,result.cosmeticNotes)
            }
            val result = GameChapterMissions.recordCoin(chapter,coins,mask)
            coins = result.coins
            mask = result.claimedMask
            notes += result.cosmeticNotes
            assertEquals(GameChapterMissions.reward(chapter),result.cosmeticNotes)
            assertTrue(GameChapterMissions.completed(chapter,mask))
            repeat(20) {
                val duplicate = GameChapterMissions.recordCoin(chapter,coins,mask)
                notes += duplicate.cosmeticNotes
                assertEquals(0,duplicate.cosmeticNotes)
            }
        }
        assertEquals(18,notes)
        assertEquals(4,GameChapterMissions.countCompleted(mask))
    }

    @Test fun pauseDoesNotResetMissionAndChangingChapterResetsCoinCount() {
        val first = GameChapterMissions.recordCoin(1,0,0)
        assertEquals(1, first.coins)
        val afterResume = GameChapterMissions.recordCoin(1,first.coins,first.claimedMask)
        assertEquals(2,afterResume.coins)
        val newChapter = GameChapterMissions.recordCoin(2,0,afterResume.claimedMask)
        assertEquals(1,newChapter.coins)
        assertEquals(0,newChapter.cosmeticNotes)
        assertEquals(0,GameChapterMissions.countCompleted(newChapter.claimedMask))
    }

    @Test fun badChapterNeverPaysAndCannotUnlockAnyGoal() {
        val noPay = GameChapterMissions.recordCoin(7,1000,0)
        assertEquals(0,noPay.cosmeticNotes)
        assertEquals(0,noPay.claimedMask)
        assertFalse(GameChapterMissions.completed(-1,15))
    }

    @Test fun cashbackIsRareVisibleMoneyForCosmeticsNotScoreOrUpgradeCoins() {
        assertEquals(Kind.COIN,GameDrops.choose(.355f,.99f,0f,45f,0,false,0))
        assertEquals(Kind.CASHBACK,GameDrops.choose(.355f,.99f,28f,45f,0,false,0))
        assertEquals(10,GameDrops.coinScore(1,false))
        assertEquals(0,GameEconomy.earnings(0,0))
        assertFalse(GameDrops.harmful(Kind.CASHBACK))
    }
}
