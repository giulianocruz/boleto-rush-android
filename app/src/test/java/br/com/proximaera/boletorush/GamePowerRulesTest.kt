package br.com.proximaera.boletorush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamePowerRulesTest {
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

    @Test fun screenClearNeverRemovesGoodPickups() {
        assertTrue(GamePowerRules.screenClear(Kind.BILL))
        assertTrue(GamePowerRules.screenClear(Kind.OVERDRAFT))
        for (kind in Kind.entries.filterNot { GameDrops.harmful(it) }) {
            assertFalse(GamePowerRules.screenClear(kind))
        }
    }
}
