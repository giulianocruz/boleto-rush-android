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

    /**
     * What a completed rewarded video can actually credit right now.
     * If the player's wallet is near Int.MAX_VALUE, never promise or charge
     * ad attention for a bonus that cannot be delivered in full.
     */
    fun preview(prefs: SharedPreferences, roundId: String): Award? {
        if (roundId.isBlank() || wasClaimed(prefs, roundId) ||
            prefs.getString("last_settlement_id", null) != roundId) return null
        val receipt = GameSettlement.latestReceipt(prefs) ?: return null
        val coinsWallet = GameEconomy.wallet(prefs)
        val notesWallet = GameEconomy.cash(prefs)
        val coinRoom = Int.MAX_VALUE.toLong() - coinsWallet.toLong()
        val noteRoom = Int.MAX_VALUE.toLong() - notesWallet.toLong()
        val coins = minOf(receipt.coinsBanked.coerceAtLeast(0).toLong(), coinRoom).toInt()
        val notes = minOf(receipt.notesBanked.coerceAtLeast(0).toLong(), noteRoom).toInt()
        if (coins == 0 && notes == 0) return null
        return Award(coins, notes, coinsWallet + coins, notesWallet + notes)
    }

    fun eligible(prefs: SharedPreferences, roundId: String): Boolean =
        preview(prefs, roundId) != null

    @Synchronized
    fun claimDouble(prefs: SharedPreferences, roundId: String): Award? {
        // Recalculate at the earned callback. Never credit more than the
        // remaining capacity or mark an empty reward as claimed.
        val award = preview(prefs, roundId) ?: return null
        if (!prefs.edit()
                .putInt("wallet_coins", award.wallet)
                .putInt("wallet_cash", award.cash)
                .putString(ClaimedKey, roundId)
                .commit()) return null
        return award
    }
}
