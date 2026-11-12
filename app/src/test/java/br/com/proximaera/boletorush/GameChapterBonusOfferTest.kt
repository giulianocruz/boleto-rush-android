package br.com.proximaera.boletorush

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameChapterBonusOfferTest {
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
