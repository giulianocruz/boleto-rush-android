package br.com.proximaera.boletorush

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameChapterBonusOfferTest {
    @Test fun earnedVideoCanCreditAfterBonusPanelWasDismissed() {
        assertTrue(GameChapterBonusOffer.mayCreditEarned(
            chapter = 1, offeredRoundId = "run-A", currentRoundId = "run-A",
            completedMask = 2, claimedMask = 0, roundSettled = false,
            activeRun = true
        ))
    }

    @Test fun earnedVideoNeverCreditsAReplacedOrSettledRound() {
        assertFalse(GameChapterBonusOffer.mayCreditEarned(
            1, "run-A", "run-B", 2, 0, false, true))
        assertFalse(GameChapterBonusOffer.mayCreditEarned(
            1, "run-A", "run-A", 2, 0, true, true))
        assertFalse(GameChapterBonusOffer.mayCreditEarned(
            1, "run-A", "run-A", 2, 0, false, false))
        assertFalse(GameChapterBonusOffer.mayCreditEarned(
            1, "", "", 2, 0, false, true))
    }

    @Test fun earnedVideoCannotCreditSameChapterTwice() {
        assertTrue(GameChapterBonusOffer.mayCreditEarned(
            0, "run-A", "run-A", 1, 0, false, true))
        assertFalse(GameChapterBonusOffer.mayCreditEarned(
            0, "run-A", "run-A", 1, 1, false, true))
        assertFalse(GameChapterBonusOffer.mayCreditEarned(
            1, "run-A", "run-A", 2, 2, false, true))
    }

    @Test fun earnedVideoRejectsUnfinishedOrInvalidChapter() {
        assertFalse(GameChapterBonusOffer.mayCreditEarned(
            1, "run-A", "run-A", 0, 0, false, true))
        assertFalse(GameChapterBonusOffer.mayCreditEarned(
            3, "run-A", "run-A", 8, 0, false, true))
        assertFalse(GameChapterBonusOffer.mayCreditEarned(
            -1, "run-A", "run-A", 0, 0, false, true))
    }


    @Test fun neverOpensAdsDuringTheMonthlyBossOrItsWarningWindow() {
        for (chapter in 0..2) {
            val completed=1 shl chapter
            assertTrue(GameChapterBonusOffer.shouldOffer(chapter,completed,0,true))
            assertFalse(GameChapterBonusOffer.shouldOffer(
                chapter,completed,0,true,bossStartingSoon=true))
        }
        val ninetySecondStageSwitch=67.5f // 75% of a 90-second run
        assertTrue(GameMonthChallenge.adBreakBlocked(ninetySecondStageSwitch,90f))
        assertFalse(GameChapterBonusOffer.shouldOffer(
            2,4,0,true,GameMonthChallenge.adBreakBlocked(ninetySecondStageSwitch,90f)))
    }

    @Test fun offersOnlyWhenGoalMetAndAdIsActuallyReady() {
        assertFalse(GameChapterBonusOffer.shouldOffer(0, 1, 0, false))
        assertFalse(GameChapterBonusOffer.shouldOffer(0, 0, 0, true))
        assertTrue(GameChapterBonusOffer.shouldOffer(0, 1, 0, true))
        assertTrue(GameChapterBonusOffer.shouldOffer(1, 2, 0, true))
    }

    @Test fun neverInterruptsAgainOrOffersOnFinalChapter() {
        assertFalse(GameChapterBonusOffer.shouldOffer(0, 1, 1, true))
        assertFalse(GameChapterBonusOffer.shouldOffer(2, 4, 4, true))
        assertFalse(GameChapterBonusOffer.shouldOffer(3, 8, 0, true))
        assertFalse(GameChapterBonusOffer.shouldOffer(-1, 1, 0, true))
    }
}
