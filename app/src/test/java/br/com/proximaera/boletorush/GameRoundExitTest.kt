package br.com.proximaera.boletorush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRoundExitTest {
    @Test fun cannotWithdrawBeforeHalfOfBothGameModes() {
        assertFalse(GameRoundExit.canCashOut(22.49f,45f))
        assertTrue(GameRoundExit.canCashOut(22.5f,45f))
        assertFalse(GameRoundExit.canCashOut(44.99f,90f))
        assertTrue(GameRoundExit.canCashOut(45f,90f))
        assertEquals(23,GameRoundExit.secondsUntilCashOut(0f,45f))
        assertEquals(1,GameRoundExit.secondsUntilCashOut(22f,45f))
        assertEquals(0,GameRoundExit.secondsUntilCashOut(45f,90f))
    }

    @Test fun withdrawalKeepsCollectedTokensAndNeverClaimsToBeVictory() {
        assertEquals(17,GameRoundExit.eligibleCoins(17))
        assertEquals(21,GameRoundExit.eligibleCash(21))
        assertEquals(0,GameRoundExit.eligibleCash(-10))
        assertEquals("SALDO RESGATADO!",GameRoundExit.resultTitle(false,true))
        assertEquals("A FATURA CHEGOU!",GameRoundExit.resultTitle(false,false))
        assertEquals("SOBREVIVEU!",GameRoundExit.resultTitle(true,false))
    }

    @Test fun allActiveEffectsAreVisibleWithoutHidingEachOther() {
        val powers = GamePowerStatus.labels(4.1f,3.2f,2.8f)
        assertEquals(3,powers.size)
        assertEquals("PIX 2x 5s",powers[0])
        assertEquals("ÍMÃ 4s",powers[1])
        assertEquals("SALDO 3s",powers[2])
        assertTrue(GamePowerStatus.labels(0f,0f,0f).isEmpty())
        assertEquals(listOf("ÍMÃ 6s"),GamePowerStatus.labels(0f,5.5f,0f))
    }
}
