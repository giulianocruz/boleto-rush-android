package br.com.proximaera.boletorush

import android.content.SharedPreferences

/**
 * Opt-in reward granted only after Google's rewarded-ad earned callback.
 * Uses an atomic commit keyed to the latest settled round to prevent duplicate
 * rewards after rapid taps or activity recreation. It doubles the actual
 * banked coins and notes, never the displayed score or borrowed credit.
 */
internal object RewardedRoundBonus {
    private const val ClaimedKey = "rewarded_double_round_id_v1"

    data class Award(val coins: Int, val notes: Int, val wallet: Int, val cash: Int)

    fun wasClaimed(prefs: SharedPreferences, roundId: String): Boolean =
        roundId.isNotBlank() && prefs.getString(ClaimedKey, null) == roundId

    fun eligible(prefs: SharedPreferences, roundId: String): Boolean {
        if (roundId.isBlank() || wasClaimed(prefs, roundId) ||
            prefs.getString("last_settlement_id", null) != roundId) return false
        val receipt = GameSettlement.latestReceipt(prefs) ?: return false
        return receipt.coinsBanked > 0 || receipt.notesBanked > 0
    }

    @Synchronized
    fun claimDouble(prefs: SharedPreferences, roundId: String): Award? {
        if (!eligible(prefs, roundId)) return null
        val receipt = GameSettlement.latestReceipt(prefs) ?: return null
        val coins = receipt.coinsBanked.coerceAtLeast(0)
        val notes = receipt.notesBanked.coerceAtLeast(0)
        val nextWallet = (GameEconomy.wallet(prefs).toLong() + coins)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val nextNotes = (GameEconomy.cash(prefs).toLong() + notes)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        if (!prefs.edit()
                .putInt("wallet_coins", nextWallet)
                .putInt("wallet_cash", nextNotes)
                .putString(ClaimedKey, roundId)
                .commit()) return null
        return Award(coins, notes, nextWallet, nextNotes)
    }
}
