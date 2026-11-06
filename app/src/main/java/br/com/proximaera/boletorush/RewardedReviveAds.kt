package br.com.proximaera.boletorush

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Optional, user-initiated ad only. No forced interstitials.
 * The publisher test identifiers below NEVER generate monetizable revenue.
 * Replace both IDs with your own AdMob IDs and recheck consent before store launch.
 */
class RewardedReviveAds(
    private val activity: Activity,
    private val consentUiAllowed: () -> Boolean = { true }
) {
    var ready by mutableStateOf(false)
        private set
    var privacyOptionsRequired by mutableStateOf(false)
        private set
    private val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
    private var rewardedAd: RewardedAd? = null
    private var sdkStarted = false
    private var loading = false
    private var consentStarted = false
    private var adRequested = false
    private var consentComplete = false
    private var consentInfoAvailable = false
    private var consentFormRequested = false
    private var pendingConsentForm: ConsentForm? = null

    // Consent info can update in the background, but any required form must
    // wait for the menu instead of appearing over a live round.
    fun resumeSafeMenu() {
        if (!consentUiAllowed()) return
        showConsentIfReady()
        showPendingFormAtMenu()
        initializeIfAllowed()
        if (sdkStarted) load()
    }

    private fun showConsentIfReady() {
        if (!consentInfoAvailable || consentFormRequested ||
            consentComplete || !consentUiAllowed()) return

        if (consentInformation.consentStatus != ConsentInformation.ConsentStatus.REQUIRED ||
            !consentInformation.isConsentFormAvailable) {
            // There is no form to show; canRequestAds still decides whether
            // advertising is permitted, including unusual configuration errors.
            consentComplete = true
            initializeIfAllowed()
            return
        }
        consentFormRequested = true
        // Separating loading from showing prevents a slow network response
        // from opening a privacy form after the user starts a round.
        UserMessagingPlatform.loadConsentForm(
            activity,
            { form ->
                pendingConsentForm = form
                showPendingFormAtMenu()
            },
            { error ->
                Log.w("BoletoRushAds", "Consent form unavailable: ${error.message}")
                consentComplete = true
                initializeIfAllowed()
            }
        )
    }

    private fun showPendingFormAtMenu() {
        if (!consentUiAllowed()) return
        val form = pendingConsentForm ?: return
        pendingConsentForm = null
        form.show(activity) { error ->
            if (error != null) Log.w("BoletoRushAds", "Consent form: ${error.message}")
            consentComplete = true
            initializeIfAllowed()
        }
    }

    fun refreshConsent() {
        if (consentStarted) return
        consentStarted = true
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(activity, params,
            {
                privacyOptionsRequired =
                    consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                consentInfoAvailable = true
                showConsentIfReady()
                // A late network response cannot open the consent form over gameplay.
            },
            { error ->
                Log.w("BoletoRushAds", "Consent update: ${error.message}")
                consentComplete = true
                initializeIfAllowed()
            }
        )
    }

    fun start() {
        val initializedEarlier = sdkStarted
        adRequested = true
        refreshConsent()
        initializeIfAllowed()
        // Retry on the next eligible screen after no-fill/network failure,
        // but never request before the initial SDK callback has run.
        if (initializedEarlier && consentUiAllowed()) load()
    }

    fun showPrivacyOptions() {
        if (privacyOptionsRequired)
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                if (error != null) Log.w("BoletoRushAds", "Privacy options: ${error.message}")
                initializeIfAllowed()
            }
    }

    private fun initializeIfAllowed() {
        if (!adRequested || !consentComplete || !consentUiAllowed() ||
            !consentInformation.canRequestAds() || sdkStarted) return
        sdkStarted = true
        MobileAds.initialize(activity) { load() }
    }

    private fun load() {
        if (loading || rewardedAd != null || !consentInformation.canRequestAds()) return
        loading = true
        RewardedAd.load(activity, activity.getString(R.string.admob_rewarded_unit_id),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loading = false
                    rewardedAd = ad
                    ready = true
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    ready = false
                    rewardedAd = null
                    Log.w("BoletoRushAds", "Rewarded ad unavailable: ${error.message}")
                }
            }
        )
    }

    fun offerRevive(onEarned: () -> Unit, onUnavailable: () -> Unit) =
        offerReward(onEarned, onUnavailable)

    fun offerReward(onEarned: () -> Unit, onUnavailable: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            onUnavailable()
            return
        }
        rewardedAd = null
        ready = false
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (earned) onEarned() else onUnavailable()
                load()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w("BoletoRushAds", "Show failed: ${error.message}")
                if (!earned) onUnavailable()
                load()
            }
        }
        ad.show(activity) { earned = true }
    }
}
