package br.com.proximaera.boletorush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamePowerRulesTest {
    @Test fun collectingShieldNeverShortensPaidRelicProtection() {
        val activeWithRelic = GameRelics.totalStartShieldSeconds(
            GameEconomy.initialShieldSeconds(3), 3f
        )
        assertEquals(9f, activeWithRelic)
        assertEquals(9f, GamePowerRules.refreshShieldSeconds(activeWithRelic))
        assertEquals(8.5f, GamePowerRules.refreshShieldSeconds(8.5f))
        assertEquals(7f, GamePowerRules.refreshShieldSeconds(6.5f))
        assertEquals(7f, GamePowerRules.refreshShieldSeconds(0f))
        assertEquals(7f, GamePowerRules.refreshShieldSeconds(-4f))
    }

    @Test fun refreshingShieldSanitizesInvalidTimerValues() {
        assertEquals(7f, GamePowerRules.refreshShieldSeconds(Float.NaN))
        assertEquals(7f, GamePowerRules.refreshShieldSeconds(Float.POSITIVE_INFINITY))
        assertEquals(7f, GamePowerRules.refreshShieldSeconds(Float.NEGATIVE_INFINITY))
        assertEquals(0f, GamePowerRules.refreshShieldSeconds(-2f, Float.NaN))
        assertEquals(9f, GamePowerRules.refreshShieldSeconds(20f, 7f))
        assertEquals(9f, GamePowerRules.refreshShieldSeconds(9f, 100f))
        assertEquals(4f, GamePowerRules.refreshShieldSeconds(4f, -5f))
    }

    private fun choose(roll: Float, elapsed: Float = 35f) =
        GameDrops.choose(roll, .99f, elapsed, 45f, 0, false, 0)

    @Test fun extraPowersAreRareAndUnlockAfterOpeningSeconds() {
        assertEquals(Kind.SPRINT,choose(.38f))
        assertEquals(Kind.SLOW,choose(.40f))
        assertEquals(Kind.ARMOR,choose(.43f))
        assertEquals(Kind.SCREEN_CLEAR,choose(.45f))
        assertEquals(Kind.COIN,choose(.38f,0f))
        assertEquals(Kind.COIN,GameDrops.choose(.45f,.99f,35f,45f,0,false,2))
    }

    @Test fun turboChangesOnlyControlResponseAndSlowChangesOnlyGravity() {
        assertEquals(17f,GamePowerRules.playerResponse(0f))
        assertEquals(26f,GamePowerRules.playerResponse(.2f))
        assertEquals(1f,GamePowerRules.fallingSpeedFactor(0f))
        assertEquals(.62f,GamePowerRules.fallingSpeedFactor(.2f))
    }

    @Test fun armorBlocksTwoHitsAndExpiresWhenTimeRunsOut() {
        assertEquals(2,GamePowerRules.armorRemaining(2,12f))
        assertEquals(1,GamePowerRules.armorRemaining(1,2f))
        assertEquals(0,GamePowerRules.armorRemaining(2,0f))
        assertEquals(0,GamePowerRules.armorRemaining(-1,4f))
    }

    @Test fun defensivePowerUpsResolveBeforeDamageOnSameFrame() {
        val kinds = listOf(Kind.BILL, Kind.OVERDRAFT, Kind.COIN,
            Kind.ARMOR, Kind.SCREEN_CLEAR, Kind.SHIELD, Kind.HEART)
        val ordered = kinds.sortedBy(GamePowerRules::collisionPriority)
        assertEquals(Kind.SCREEN_CLEAR, ordered.first())
        assertTrue(ordered.indexOf(Kind.ARMOR) < ordered.indexOf(Kind.BILL))
        assertTrue(ordered.indexOf(Kind.SHIELD) < ordered.indexOf(Kind.OVERDRAFT))
        assertTrue(ordered.indexOf(Kind.HEART) < ordered.indexOf(Kind.BILL))
        assertTrue(ordered.indexOf(Kind.COIN) < ordered.indexOf(Kind.OVERDRAFT))
        assertEquals(Kind.OVERDRAFT, ordered.last())
    }

    @Test fun defenseSortingOnlyNeededForActualSameFrameWalletContact() {
        val hits = GamePowerRules::defensivePickupWillCollide
        assertTrue(hits(Kind.ARMOR, .79f, .81f, .50f, .50f))
        assertTrue(hits(Kind.SHIELD, .79f, .81f, .50f, .50f))
        assertTrue(hits(Kind.HEART, .79f, .81f, .50f, .50f))
        assertTrue(hits(Kind.SCREEN_CLEAR, .79f, .81f, .50f, .50f))
        assertFalse(hits(Kind.BILL, .79f, .81f, .50f, .50f))
        assertFalse(hits(Kind.SHIELD, .50f, .53f, .50f, .50f))
        assertFalse(hits(Kind.SHIELD, .79f, .81f, .20f, .50f))
        assertFalse(hits(Kind.ARMOR, .80f, .82f, .50f, .50f))
        assertFalse(hits(Kind.ARMOR, .79f, Float.NaN, .50f, .50f))
    }

    @Test fun screenClearNeverRemovesGoodPickups() {
        assertTrue(GamePowerRules.screenClear(Kind.BILL))
        assertTrue(GamePowerRules.screenClear(Kind.OVERDRAFT))
        for (kind in Kind.entries.filterNot { GameDrops.harmful(it) }) {
            assertFalse(GamePowerRules.screenClear(kind))
        }
    }
}
