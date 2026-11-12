package br.com.proximaera.boletorush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameNearMissTest {
    private fun qualified(
        kind: Kind = Kind.BILL,
        previousY: Float = .795f,
        nextY: Float = .805f,
        x: Float = .68f,
        player: Float = .50f,
        age: Float = .25f,
        hasProtection: Boolean = false
    ) = GameNearMiss.qualified(
        kind, previousY, nextY, x, player, age, hasProtection
    )

    @Test fun rewardsCloseIntentionalDodgesOfHazards() {
        assertTrue(qualified())
        assertTrue(qualified(kind = Kind.OVERDRAFT))
        assertTrue(qualified(x = .625f)) // exact safe boundary
        assertTrue(qualified(x = .715f)) // outer edge
        assertTrue(qualified(age = .8f))
        assertFalse(qualified(x = .62f)) // inside collision margin
        assertFalse(qualified(x = .73f)) // too far to be a near miss
    }

    @Test fun requiresRealCrossingAndRecentMovement() {
        assertFalse(qualified(previousY = .60f, nextY = .79f))
        assertFalse(qualified(previousY = .8f, nextY = .9f))
        assertFalse(qualified(age = .801f))
        assertFalse(qualified(age = -1f))
    }

    @Test fun neverRewardsFreeOrProtectedDodges() {
        assertFalse(qualified(kind = Kind.COIN))
        assertFalse(qualified(kind = Kind.HEART))
        assertFalse(qualified(hasProtection = true))
        assertFalse(qualified(previousY = Float.NaN))
        assertFalse(qualified(nextY = Float.POSITIVE_INFINITY))
        assertFalse(qualified(x = Float.NaN))
        assertFalse(qualified(player = Float.NaN))
        assertFalse(qualified(age = Float.NaN))
    }

    @Test fun pointsStayBoundedAndDoNotMintCurrency() {
        assertEquals(7, GameNearMiss.points(1))
        assertEquals(9, GameNearMiss.points(2))
        assertEquals(15, GameNearMiss.points(5))
        assertEquals(15, GameNearMiss.points(1000))
        assertEquals(7, GameNearMiss.points(Int.MIN_VALUE))
    }
}
