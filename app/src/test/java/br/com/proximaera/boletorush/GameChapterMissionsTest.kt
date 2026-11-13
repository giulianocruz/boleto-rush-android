package br.com.proximaera.boletorush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ensures that chapter goals only award cosmetic notes, once per chapter. */
class GameChapterMissionsTest {
    @Test fun classicFinalChapterGoalMatchesTwoReachableBossCoins() {
        assertEquals(2, GameChapterMissions.goal(3, 45f))
        assertEquals(4, GameChapterMissions.goal(3, 90f))
        for (chapter in 0..2) {
            assertEquals(GameChapterMissions.goal(chapter),
                GameChapterMissions.goal(chapter, 45f))
            assertEquals(GameChapterMissions.goal(chapter),
                GameChapterMissions.goal(chapter, 90f))
        }
    }

    @Test fun classicFinalChapterPaysAfterTwoCoinsAndNeverPaysTwice() {
        val first = GameChapterMissions.recordCoin(3, 0, 0, 45f)
        assertEquals(1, first.coins)
        assertEquals(0, first.cosmeticNotes)
        val second = GameChapterMissions.recordCoin(3, first.coins, first.claimedMask, 45f)
        assertEquals(2, second.coins)
        assertEquals(GameChapterMissions.reward(3), second.cosmeticNotes)
        assertTrue(GameChapterMissions.completed(3, second.claimedMask))
        val third = GameChapterMissions.recordCoin(3, second.coins, second.claimedMask, 45f)
        assertEquals(0, third.cosmeticNotes)
        assertEquals(second.claimedMask, third.claimedMask)
    }

    @Test fun marathonFinalChapterStillNeedsFourCoins() {
        var coins = 0
        var mask = 0
        repeat(3) {
            val step = GameChapterMissions.recordCoin(3, coins, mask, 90f)
            coins = step.coins
            mask = step.claimedMask
            assertEquals(0, step.cosmeticNotes)
        }
        val fourth = GameChapterMissions.recordCoin(3, coins, mask, 90f)
        assertEquals(4, fourth.coins)
        assertEquals(6, fourth.cosmeticNotes)
        assertTrue(GameChapterMissions.completed(3, fourth.claimedMask))
    }

    @Test fun bossSafeCoinsCanReachClassicFinalChapterBeforeTimerEndsEvenWhenSlowed() {
        val duration = 45f
        val chapterFourStart = duration * .75f
        for (slow in listOf(0f, GamePowerRules.SlowDuration)) {
            val factor = GamePowerRules.fallingSpeedFactor(slow)
            val fallTime = (GameNearMiss.WalletY - GameMonthChallenge.WaveDropStartY) /
                (GameMonthChallenge.WaveDropSpeed * factor)
            val arrivalTimes = (0 until GameMonthChallenge.WaveCount).map { index ->
                val start = duration - GameMonthChallenge.FinalWindowSeconds +
                    index * GameMonthChallenge.WaveSpacingSeconds
                assertEquals(index, GameMonthChallenge.waveIndex(start, duration))
                start + fallTime
            }
            assertEquals(2, arrivalTimes.count {
                it >= chapterFourStart && it < duration
            })
            assertTrue(arrivalTimes.last() < duration)
            assertEquals(GameChapterMissions.goal(3, duration),
                arrivalTimes.count { it >= chapterFourStart && it < duration })
        }
    }

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
