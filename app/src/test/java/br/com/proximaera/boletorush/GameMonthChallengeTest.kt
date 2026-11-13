package br.com.proximaera.boletorush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GameMonthChallengeTest {
    @Test fun bossProgressShowsLiveDangerCountAndSealRisk() {
        assertEquals("PERIGOS 0/6 • SEM CONTATOS",
            GameMonthChallenge.progressLabel(0, 0))
        assertEquals("PERIGOS 3/6 • SEM CONTATOS",
            GameMonthChallenge.progressLabel(3, 0))
        assertEquals("PERIGOS 4/6 • SELO EM RISCO",
            GameMonthChallenge.progressLabel(4, 1))
        assertEquals("PERIGOS 6/6 • SELO EM RISCO",
            GameMonthChallenge.progressLabel(8, 3))
    }

    @Test fun bossWaveCompletionIsBoundToLaunchedWaves() {
        assertEquals(0, GameMonthChallenge.completedWaves(0, 0))
        assertEquals(0, GameMonthChallenge.completedWaves(1, 1))
        assertEquals(1, GameMonthChallenge.completedWaves(2, 1))
        assertEquals(1, GameMonthChallenge.completedWaves(3, 2))
        assertEquals(2, GameMonthChallenge.completedWaves(4, 2))
        assertEquals(3, GameMonthChallenge.completedWaves(6, 3))
        assertEquals(2, GameMonthChallenge.completedWaves(6, 2))
    }

    @Test fun bossProgressNeverGoesNegativeOrBeyondSix() {
        assertEquals("PERIGOS 0/6 • SEM CONTATOS",
            GameMonthChallenge.progressLabel(-50, 0))
        assertEquals("PERIGOS 6/6 • SEM CONTATOS",
            GameMonthChallenge.progressLabel(200, 0))
        assertEquals(0, GameMonthChallenge.completedWaves(-3, 3))
        assertEquals(0, GameMonthChallenge.completedWaves(6, -1))
        assertEquals(3, GameMonthChallenge.completedWaves(100, 30))
    }

    @Test fun bossWaveFeedbackIsEncouragingWithoutGrantingHiddenRewards() {
        assertEquals(
            "ONDA 1/3 SUPERADA! CONTINUE SEM TOCAR NOS PERIGOS",
            GameMonthChallenge.waveCompletionLabel(1, 0)
        )
        assertEquals(
            "ONDA 3/3 CONCLUÍDA • SELO EXIGE ZERO CONTATOS",
            GameMonthChallenge.waveCompletionLabel(3, 1)
        )
        assertFalse(GameMonthChallenge.waveCompletionLabel(2, 0).contains("R$"))
    }

    @Test fun stampCannotBeEarnedBeforeAllSixHazardsAreResolved() {
        assertFalse(GameMonthChallenge.qualified(true,3,0,0))
        assertFalse(GameMonthChallenge.qualified(true,3,0,5))
        assertTrue(GameMonthChallenge.qualified(true,3,0,6))
        assertFalse(GameMonthChallenge.qualified(true,3,0,7))
        assertFalse(GameMonthChallenge.qualified(false,3,0,6))
    }

    @Test fun screenClearCountsOnlyHazardsAboveWalletLine() {
        assertTrue(GameMonthChallenge.hazardResolved(.799f,.801f,false))
        assertFalse(GameMonthChallenge.hazardResolved(.799f,.7995f,false))
        assertFalse(GameMonthChallenge.hazardResolved(.8f,.82f,false))
        assertTrue(GameMonthChallenge.hazardResolved(.12f,.12f,true))
        assertFalse(GameMonthChallenge.hazardResolved(.86f,.86f,true))
        assertFalse(GameMonthChallenge.hazardResolved(Float.NaN,.9f,false))
        assertFalse(GameMonthChallenge.hazardResolved(.1f,Float.POSITIVE_INFINITY,true))
    }

    @Test fun bossWaveTitleIsPrintableOnAndroid() {
        assertEquals("FECHAMENTO | ONDA 1/3",GameMonthChallenge.waveTitle(0))
        assertEquals("FECHAMENTO | ONDA 3/3",GameMonthChallenge.waveTitle(2))
        assertTrue(GameMonthChallenge.waveTitle(1).all { it.code >= 32 })
    }

    @Test fun preBossWarningGivesFiveSecondsToPrepareForBothModes() {
        assertEquals(0,GameMonthChallenge.warningSeconds(21.9f,45f))
        assertEquals(5,GameMonthChallenge.warningSeconds(22f,45f))
        assertEquals(4,GameMonthChallenge.warningSeconds(23.5f,45f))
        assertEquals(1,GameMonthChallenge.warningSeconds(26.9f,45f))
        assertEquals(0,GameMonthChallenge.warningSeconds(27f,45f))
        assertEquals(0,GameMonthChallenge.warningSeconds(44f,45f))
        assertEquals(0,GameMonthChallenge.warningSeconds(66.9f,90f))
        assertEquals(5,GameMonthChallenge.warningSeconds(67f,90f))
        assertEquals(1,GameMonthChallenge.warningSeconds(71.7f,90f))
        assertEquals(0,GameMonthChallenge.warningSeconds(72f,90f))
        assertEquals(0,GameMonthChallenge.warningSeconds(Float.NaN,45f))
        assertEquals(0,GameMonthChallenge.warningSeconds(1f,Float.POSITIVE_INFINITY))
    }

    @Test fun bossPreparationAndWavesAreNeverInterruptedByOptionalAdGates() {
        for (duration in listOf(45f,90f)) {
            val first = duration-GameMonthChallenge.FinalWindowSeconds
            val warning = first-GameMonthChallenge.WarningLeadSeconds
            assertFalse(GameMonthChallenge.adBreakBlocked(warning-.01f,duration))
            assertTrue(GameMonthChallenge.adBreakBlocked(warning,duration))
            assertTrue(GameMonthChallenge.adBreakBlocked(first,duration))
            assertTrue(GameMonthChallenge.adBreakBlocked(first+9f,duration))
            assertTrue(GameMonthChallenge.adBreakBlocked(duration-.05f,duration))
            assertFalse(GameMonthChallenge.adBreakBlocked(duration,duration))
        }
        assertFalse(GameMonthChallenge.adBreakBlocked(Float.NaN,45f))
        assertFalse(GameMonthChallenge.adBreakBlocked(2f,Float.NEGATIVE_INFINITY))
        assertFalse(GameMonthChallenge.adBreakBlocked(1f,20f))
    }

    @Test fun bossStartsWithExactlyThreeWavesNearEndOfBothRoundLengths() {
        assertEquals(-1,GameMonthChallenge.waveIndex(26.99f,45f))
        assertEquals(0,GameMonthChallenge.waveIndex(27f,45f))
        assertEquals(0,GameMonthChallenge.waveIndex(32.99f,45f))
        assertEquals(1,GameMonthChallenge.waveIndex(33f,45f))
        assertEquals(2,GameMonthChallenge.waveIndex(39f,45f))
        assertEquals(-1,GameMonthChallenge.waveIndex(45f,45f))
        assertEquals(-1,GameMonthChallenge.waveIndex(71.99f,90f))
        assertEquals(0,GameMonthChallenge.waveIndex(72f,90f))
        assertEquals(1,GameMonthChallenge.waveIndex(78f,90f))
        assertEquals(2,GameMonthChallenge.waveIndex(84f,90f))
        assertEquals(-1,GameMonthChallenge.waveIndex(90f,90f))
        assertEquals(-1,GameMonthChallenge.waveIndex(Float.NaN,45f))
        assertEquals(-1,GameMonthChallenge.waveIndex(1f,Float.POSITIVE_INFINITY))
    }

    @Test fun eachWaveHasTwoDangerLanesAndAReachableSafeCorridor() {
        for (m in 1..12) {
            val month="2026-"+m.toString().padStart(2,'0')
            val safeLanes = (0 until GameMonthChallenge.WaveCount).map {
                GameMonthChallenge.wave(it,month).safeLane
            }
            assertEquals(setOf(0,1,2),safeLanes.toSet())
            for (wave in 0..2) {
                val w = GameMonthChallenge.wave(wave,month)
                assertEquals(wave+1,w.number)
                assertEquals(2,w.hazardXs.size)
                assertTrue(w.safeX !in w.hazardXs)
                assertTrue(w.hazardXs.all { kotlin.math.abs(it-w.safeX)>=.31f })
                assertTrue(w.hazardXs.all { it in .09f..91f })
                assertEquals(if(wave==1) Kind.OVERDRAFT else Kind.BILL,w.hazardKind)
                assertTrue(GameMonthChallenge.safeLaneLabel(w.safeLane)!="INDEFINIDA")
            }
        }
    }

    @Test fun monthStampNeedsAllWavesVictoryAndZeroContacts() {
        assertTrue(GameMonthChallenge.qualified(true,3,0,6))
        assertFalse(GameMonthChallenge.qualified(true,2,0,4))
        assertFalse(GameMonthChallenge.qualified(false,3,0,6))
        assertFalse(GameMonthChallenge.qualified(true,3,1,6))
        assertFalse(GameMonthChallenge.qualified(true,4,0,8))
        assertEquals("2026-11", GameMonthChallenge.monthId(LocalDate.of(2026,11,12)))
        assertEquals("2027-01", GameMonthChallenge.monthId(LocalDate.of(2027,1,1)))
    }
}
