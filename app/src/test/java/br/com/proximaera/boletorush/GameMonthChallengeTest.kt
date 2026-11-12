package br.com.proximaera.boletorush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GameMonthChallengeTest {
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
        assertTrue(GameMonthChallenge.qualified(true,3,0))
        assertFalse(GameMonthChallenge.qualified(true,2,0))
        assertFalse(GameMonthChallenge.qualified(false,3,0))
        assertFalse(GameMonthChallenge.qualified(true,3,1))
        assertFalse(GameMonthChallenge.qualified(true,4,0))
        assertEquals("2026-11", GameMonthChallenge.monthId(LocalDate.of(2026,11,12)))
        assertEquals("2027-01", GameMonthChallenge.monthId(LocalDate.of(2027,1,1)))
    }
}
