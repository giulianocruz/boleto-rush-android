package br.com.proximaera.boletorush

/** Pure guardrails for optional rewarded advertising and live UMP decisions. */
internal object GameAdConsentGate {
    fun mayLoad(
        requested: Boolean,
        consentResolved: Boolean,
        sdkStarted: Boolean,
        canRequestAds: Boolean,
        privacyOptionsOpen: Boolean
    ): Boolean = requested && consentResolved && sdkStarted &&
        canRequestAds && !privacyOptionsOpen

    fun mayKeepLoadedAd(
        consentResolved: Boolean,
        canRequestAds: Boolean,
        privacyOptionsOpen: Boolean,
        requestEpochMatches: Boolean
    ): Boolean = consentResolved && canRequestAds &&
        !privacyOptionsOpen && requestEpochMatches

    fun mayShowReward(
        adPresent: Boolean,
        ready: Boolean,
        consentResolved: Boolean,
        canRequestAds: Boolean,
        privacyOptionsOpen: Boolean,
        activityAvailable: Boolean
    ): Boolean = adPresent && ready && consentResolved &&
        canRequestAds && !privacyOptionsOpen && activityAvailable
}
