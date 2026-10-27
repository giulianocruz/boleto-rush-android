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
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Optional, user-initiated ad only. No forced interstitials.
 * The publisher test identifiers below NEVER generate monetizable revenue.
 * Replace both IDs with your own AdMob IDs and recheck consent before store launch.
 */
class RewardedReviveAds(private val activity: Activity) {
    var ready by mutableStateOf(false)
        private set
    var privacyOptionsRequired by mutableStateOf(false)
        private set
    private val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
    private var rewardedAd: RewardedAd? = null
    private var sdkStarted = false
    private var loading = false

    fun start() {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(activity, params,
            {
                privacyOptionsRequired =
                    consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w("BoletoRushAds", "Consent form: ${error.message}")
                    initializeIfAllowed()
                }
                // If valid consent is cached, still avoid a duplicate initialization.
                initializeIfAllowed()
            },
            { error ->
                Log.w("BoletoRushAds", "Consent update: ${error.message}")
                initializeIfAllowed()
            }
        )
    }

    fun showPrivacyOptions() {
        if (privacyOptionsRequired)
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                if (error != null) Log.w("BoletoRushAds", "Privacy options: ${error.message}")
            }
    }

    private fun initializeIfAllowed() {
        if (!consentInformation.canRequestAds() || sdkStarted) return
        sdkStarted = true
        MobileAds.initialize(activity) { load() }
    }

    private fun load() {
        if (loading || rewardedAd != null || !consentInformation.canRequestAds()) return
        loading = true
        RewardedAd.load(activity, "ca-app-pub-3940256099942544/5224354917",
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
                    Log.w("BoletoRushAds", "Test ad unavailable: ${error.message}")
                }
            }
        )
    }

    fun offerRevive(onEarned: () -> Unit, onUnavailable: () -> Unit) {
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
                if (earned) onEarned()
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
