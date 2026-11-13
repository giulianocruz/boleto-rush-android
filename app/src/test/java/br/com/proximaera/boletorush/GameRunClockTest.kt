package br.com.proximaera.boletorush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRunClockTest {
    @Test fun backgroundPlayTransitionIsAutomaticallyPausedEvenAfterAdCallback() {
        assertFalse(GameRunClock.shouldAutoPause(foreground = true, playing = true))
        assertTrue(GameRunClock.shouldAutoPause(foreground = false, playing = true))
        assertFalse(GameRunClock.shouldAutoPause(foreground = false, playing = false))
        assertFalse(GameRunClock.shouldAutoPause(foreground = true, playing = false))
    }


    @Test fun clockOnlyRunsDuringVisibleGameplay() {
        assertTrue(GameRunClock.shouldAdvance(foreground = true, playing = true))
        assertFalse(GameRunClock.shouldAdvance(foreground = false, playing = true))
        assertFalse(GameRunClock.shouldAdvance(foreground = true, playing = false))
        assertFalse(GameRunClock.shouldAdvance(foreground = false, playing = false))
    }

    @Test fun pauseAndRewardedAdDoNotAdvanceHiddenClock() {
        // The earned callback can restore PLAY before full-screen AdMob closes.
        assertFalse(GameRunClock.shouldAdvance(foreground = false, playing = true))
        assertEquals(0.016f, GameRunClock.frameSeconds(1_000_000_000L, 1_016_000_000L), 0.00001f)
        assertEquals(0.05f, GameRunClock.frameSeconds(1_000_000_000L, 4_000_000_000L), 0.00001f)
        assertTrue(GameRunClock.shouldAdvance(foreground = true, playing = true))
    }

    @Test fun frameClockIgnoresNonMonotonicOrDuplicateTimestamps() {
        assertEquals(0f, GameRunClock.frameSeconds(100L, 100L), 0f)
        assertEquals(0f, GameRunClock.frameSeconds(100L, 90L), 0f)
        assertEquals(0f, GameRunClock.frameSeconds(200L, 0L), 0f)
        assertEquals(0.02f, GameRunClock.frameSeconds(1_000_000_000L, 1_020_000_000L), 0.00001f)
    }
}
