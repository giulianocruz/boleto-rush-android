package br.com.proximaera.boletorush

import android.content.SharedPreferences

/**
 * Boleto Rush's local, offline progression. Score and wallet balance are separate.
 * The game contains no in-app purchases or real-money currency.
 */
internal enum class Upgrade(val key: String, val label: String, val baseCost: Int) {
    MAGNET("upgrade_magnet", "ÍMÃ MAIS FORTE", 12),
    START_SHIELD("upgrade_start_shield", "PROTEÇÃO INICIAL", 18),
    COIN_BONUS("upgrade_coin_bonus", "RENDA EXTRA", 24);

    val maxLevel: Int get() = 3
}

internal object GameEconomy {
    private const val WalletKey = "wallet_coins"

    fun wallet(prefs: SharedPreferences): Int = prefs.getInt(WalletKey, 0).coerceAtLeast(0)

    fun level(prefs: SharedPreferences, upgrade: Upgrade): Int =
        prefs.getInt(upgrade.key, 0).coerceIn(0, upgrade.maxLevel)

    fun price(upgrade: Upgrade, currentLevel: Int): Int? =
        if (currentLevel >= upgrade.maxLevel) null
        else upgrade.baseCost * (currentLevel + 1) * (currentLevel + 1)

    fun magnetSeconds(level: Int): Float = 6f + level.coerceIn(0, 3) * 2f

    fun initialShieldSeconds(level: Int): Float = level.coerceIn(0, 3) * 2f

    fun earnings(coinsCollected: Int, bonusLevel: Int): Int =
        coinsCollected.coerceAtLeast(0) * (1 + bonusLevel.coerceIn(0, 3))

    fun creditRound(prefs: SharedPreferences, coinsCollected: Int, bonusLevel: Int): Int {
        val earned = earnings(coinsCollected, bonusLevel)
        if (earned > 0) prefs.edit().putInt(WalletKey, (wallet(prefs).toLong() + earned)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).apply()
        return earned
    }

    fun buy(prefs: SharedPreferences, upgrade: Upgrade): Boolean {
        val current = level(prefs, upgrade)
        val cost = price(upgrade, current) ?: return false
        val balance = wallet(prefs)
        if (balance < cost) return false
        prefs.edit().putInt(WalletKey, balance - cost)
            .putInt(upgrade.key, current + 1).apply()
        return true
    }
}
