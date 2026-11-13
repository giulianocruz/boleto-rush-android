package br.com.proximaera.boletorush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameAdScreenPolicyTest {
    @Test fun consentCanOnlyAppearAtUnobstructedForegroundMenu() {
        assertTrue(GameAdScreenPolicy.canPresentConsent(true, true, false))
        for (menu in listOf(false, true))
            for (foreground in listOf(false, true))
                for (dialog in listOf(false, true))
                    assertEquals(menu && foreground && !dialog,
                        GameAdScreenPolicy.canPresentConsent(menu, foreground, dialog))
    }

    @Test fun noConsentPromptOverGameplayOrRecoveryAlert() {
        assertFalse(GameAdScreenPolicy.canPresentConsent(false, true, false))
        assertFalse(GameAdScreenPolicy.canPresentConsent(true, true, true))
        assertFalse(GameAdScreenPolicy.canPresentConsent(true, false, false))
    }
}
