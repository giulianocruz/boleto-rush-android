package br.com.proximaera.boletorush

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameAdConsentGateTest {
    @Test fun cannotRequestAdsUntilConsentAndSdkAreReady() {
        assertFalse(GameAdConsentGate.mayLoad(false,true,true,true,false))
        assertFalse(GameAdConsentGate.mayLoad(true,false,true,true,false))
        assertFalse(GameAdConsentGate.mayLoad(true,true,false,true,false))
        assertFalse(GameAdConsentGate.mayLoad(true,true,true,false,false))
        assertFalse(GameAdConsentGate.mayLoad(true,true,true,true,true))
        assertTrue(GameAdConsentGate.mayLoad(true,true,true,true,false))
    }

    @Test fun refreshedPrivacyChoiceInvalidatesPreviouslyLoadedCreative() {
        assertTrue(GameAdConsentGate.mayKeepLoadedAd(true,true,false,true))
        assertFalse(GameAdConsentGate.mayKeepLoadedAd(true,true,true,true))
        assertFalse(GameAdConsentGate.mayKeepLoadedAd(true,true,false,false))
        assertFalse(GameAdConsentGate.mayKeepLoadedAd(true,false,false,true))
        assertFalse(GameAdConsentGate.mayKeepLoadedAd(false,true,false,true))
    }

    @Test fun neverShowsReadyAdIfPermissionIsWithdrawnOrDialogIsOpen() {
        assertTrue(GameAdConsentGate.mayShowReward(true,true,true,true,false,true))
        assertFalse(GameAdConsentGate.mayShowReward(false,true,true,true,false,true))
        assertFalse(GameAdConsentGate.mayShowReward(true,false,true,true,false,true))
        assertFalse(GameAdConsentGate.mayShowReward(true,true,false,true,false,true))
        assertFalse(GameAdConsentGate.mayShowReward(true,true,true,false,false,true))
        assertFalse(GameAdConsentGate.mayShowReward(true,true,true,true,true,true))
        assertFalse(GameAdConsentGate.mayShowReward(true,true,true,true,false,false))
    }
}
