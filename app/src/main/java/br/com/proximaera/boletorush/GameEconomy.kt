package br.com.proximaera.boletorush

import android.content.SharedPreferences

/** All balances are game-only, stored offline, with no real-money transactions. */
internal enum class Upgrade(val key: String, val label: String, val baseCost: Int) {
    MAGNET("upgrade_magnet", "ÍMÃ MAIS FORTE", 18),
    START_SHIELD("upgrade_start_shield", "SALDO EXTRA INICIAL", 25),
    COIN_BONUS("upgrade_coin_bonus", "RENDA EXTRA", 32);
    val maxLevel: Int get() = 3
}

internal enum class Cosmetic(val key: String, val title: String, val cost: Int) {
    CLASSIC("classic", "CARTEIRA CLÁSSICA", 0),
    NEON("neon", "BRILHO DE NEON", 25),
    GOLD("gold", "BANHO DOURADO", 55),
    BURGUNDY("burgundy", "EXECUTIVO BORDÔ", 90)
}

internal object GameEconomy {
    private const val WalletKey = "wallet_coins"
    private const val CashKey = "wallet_cash"
    private const val CreditKey = "store_credit"
    internal const val DebtKey = "credit_debt"
    private const val SkinKey = "selected_cosmetic"

    fun wallet(prefs: SharedPreferences): Int = prefs.getInt(WalletKey, 0).coerceAtLeast(0)
    fun cash(prefs: SharedPreferences): Int = prefs.getInt(CashKey, 0).coerceAtLeast(0)
    fun credit(prefs: SharedPreferences): Int = prefs.getInt(CreditKey, 0).coerceIn(0, 25)
    fun debt(prefs: SharedPreferences): Int = prefs.getInt(DebtKey, 0).coerceAtLeast(0)

    fun level(prefs: SharedPreferences, upgrade: Upgrade): Int =
        prefs.getInt(upgrade.key, 0).coerceIn(0, upgrade.maxLevel)
    fun price(upgrade: Upgrade, currentLevel: Int): Int? =
        if (currentLevel >= upgrade.maxLevel) null
        else upgrade.baseCost * (currentLevel + 1) * (currentLevel + 1)

    fun magnetSeconds(level: Int): Float = 6f + level.coerceIn(0, 3) * 2f
    fun initialShieldSeconds(level: Int): Float = level.coerceIn(0, 3) * 2f

    // Scarce upgrade tokens: collecting coins still scores points, but only some become spendable.
    fun earnings(coinsCollected: Int, bonusLevel: Int): Int =
        if (coinsCollected <= 0) 0
        else (coinsCollected / (5 - bonusLevel.coerceIn(0, 3))).coerceAtLeast(1)

    @Synchronized
    fun creditRound(prefs: SharedPreferences, coinsCollected: Int, bonusLevel: Int): Int {
        val earned = earnings(coinsCollected, bonusLevel)
        val paidDebt = minOf(earned, debt(prefs))
        val net = earned - paidDebt
        if (earned > 0) prefs.edit()
            .putInt(DebtKey, debt(prefs) - paidDebt)
            .putInt(WalletKey, (wallet(prefs).toLong() + net).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            .commit()
        return net
    }

    @Synchronized
    fun creditCash(prefs: SharedPreferences, earned: Int): Int {
        val value = earned.coerceAtLeast(0)
        if (value > 0) prefs.edit().putInt(CashKey,
            (cash(prefs).toLong() + value).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).commit()
        return value
    }

    /**
     * Keep collected cards pending until the round is settled. Leaving the match
     * without saving must never mint permanent shop credit.
     * Uses Long arithmetic to tolerate corrupted or unexpectedly large preferences.
     */
    fun reserveCardCredit(prefs: SharedPreferences, pending: Int, amount: Int = 10): Int {
        val room = (25L - debt(prefs) - credit(prefs) - pending.coerceAtLeast(0))
            .coerceIn(0L, 25L).toInt()
        return amount.coerceIn(0, room)
    }

    /** Cards grant spending room, never wallet coins. New earnings pay down spent credit first. */
    @Synchronized
    fun addCredit(prefs: SharedPreferences, amount: Int): Int {
        val room = (25 - debt(prefs) - credit(prefs)).coerceAtLeast(0)
        val granted = amount.coerceIn(0, room)
        if (granted > 0) prefs.edit().putInt(CreditKey, credit(prefs) + granted).commit()
        return granted
    }

    @Synchronized
    fun buy(prefs: SharedPreferences, upgrade: Upgrade): Boolean {
        val current = level(prefs, upgrade)
        val cost = price(upgrade, current) ?: return false
        val balance = wallet(prefs)
        val borrowed = (cost - balance).coerceAtLeast(0)
        if (borrowed > credit(prefs)) return false
        return prefs.edit()
            .putInt(WalletKey, (balance - cost).coerceAtLeast(0))
            .putInt(CreditKey, credit(prefs) - borrowed)
            .putInt(DebtKey, debt(prefs) + borrowed)
            .putInt(upgrade.key, current + 1).commit()
    }

    fun selectedCosmetic(prefs: SharedPreferences): Cosmetic =
        Cosmetic.entries.firstOrNull { it.key == prefs.getString(SkinKey, Cosmetic.CLASSIC.key) }
            ?: Cosmetic.CLASSIC

    fun ownsCosmetic(prefs: SharedPreferences, cosmetic: Cosmetic): Boolean =
        cosmetic == Cosmetic.CLASSIC || prefs.getBoolean("owned_${cosmetic.key}", false)

    @Synchronized
    fun buyOrEquipCosmetic(prefs: SharedPreferences, cosmetic: Cosmetic): Boolean {
        if (ownsCosmetic(prefs, cosmetic)) {
            return prefs.edit().putString(SkinKey, cosmetic.key).commit()
        }
        if (cash(prefs) < cosmetic.cost) return false
        return prefs.edit().putInt(CashKey, cash(prefs) - cosmetic.cost)
            .putBoolean("owned_${cosmetic.key}", true)
            .putString(SkinKey, cosmetic.key).commit()
    }
}
