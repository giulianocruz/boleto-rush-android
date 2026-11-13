package br.com.proximaera.boletorush

import android.content.SharedPreferences

/** All relics and rewards are offline game items, never redeemable for real money. */
internal enum class RelicDeal(val cost: Int, val label: String) {
    NOTES(2, "+R$ 18 NOTAS PARA VISUAIS"),
    COINS(3, "+5 MOEDAS DE MELHORIA"),
    START_SHIELD(5, "3s DE SALDO EXTRA NA PRÓXIMA PARTIDA"),
    START_MAGNET(7, "6s DE ÍMÃ NA PRÓXIMA PARTIDA")
}

internal object GameRelics {
    const val WalletKey = "wallet_relic_shards_v1"
    private const val ShieldBoostKey = "relic_start_shield_v1"
    private const val MagnetBoostKey = "relic_start_magnet_v1"
    private const val AdClaimKey = "relic_ad_bonus_round_v1"
    const val MaxWallet = 999
    const val MaxPerRound = 12

    fun isRelic(kind: Kind): Boolean =
        kind == Kind.RELIC_BRONZE || kind == Kind.RELIC_SILVER || kind == Kind.RELIC_GOLD

    fun value(kind: Kind): Int = when (kind) {
        Kind.RELIC_BRONZE -> 1
        Kind.RELIC_SILVER -> 2
        Kind.RELIC_GOLD -> 3
        else -> 0
    }

    fun rarityLabel(kind: Kind): String = when (kind) {
        Kind.RELIC_BRONZE -> "BRONZE"
        Kind.RELIC_SILVER -> "PRATA"
        Kind.RELIC_GOLD -> "OURO"
        else -> ""
    }

    fun bankable(collected: Int): Int = collected.coerceIn(0, MaxPerRound)
    fun balance(prefs: SharedPreferences): Int = prefs.getInt(WalletKey, 0).coerceIn(0, MaxWallet)
    fun pendingShield(prefs: SharedPreferences): Boolean = prefs.getBoolean(ShieldBoostKey, false)
    fun pendingMagnet(prefs: SharedPreferences): Boolean = prefs.getBoolean(MagnetBoostKey, false)

    /**
     * Purchased relic protection is an ADDITION to permanent protection.
     * Using maxOf here used to silently waste the paid-in-relics bonus
     * for players who had already upgraded their starting shield.
     */
    fun totalStartShieldSeconds(permanentSeconds: Float, relicBonusSeconds: Float): Float {
        val permanent = if (permanentSeconds.isFinite()) permanentSeconds.coerceIn(0f, 6f) else 0f
        val bonus = if (relicBonusSeconds.isFinite()) relicBonusSeconds.coerceIn(0f, 3f) else 0f
        return permanent + bonus
    }

    /**
     * Trades are atomic with the relic debit, including debt repayment when
     * buying upgrade tokens. Pending boosts cannot be bought twice.
     */
    /**
     * Never charge shards for a reward that cannot be fully delivered.
     * A coin trade also has value when all five coins pay down card debt.
     */
    fun tradeHasCapacity(prefs: SharedPreferences, deal: RelicDeal): Boolean =
        when (deal) {
            RelicDeal.NOTES ->
                GameEconomy.cash(prefs).toLong() + 18L <= Int.MAX_VALUE.toLong()
            RelicDeal.COINS -> {
                val repayment = minOf(5, GameEconomy.debt(prefs))
                val grantedCoins = 5 - repayment
                GameEconomy.wallet(prefs).toLong() + grantedCoins <= Int.MAX_VALUE.toLong()
            }
            RelicDeal.START_SHIELD, RelicDeal.START_MAGNET -> true
        }

    @Synchronized
    fun exchange(prefs: SharedPreferences, deal: RelicDeal): Boolean {
        val current = balance(prefs)
        if (current < deal.cost || !tradeHasCapacity(prefs, deal)) return false
        val edit = prefs.edit().putInt(WalletKey, current - deal.cost)
        when (deal) {
            RelicDeal.NOTES -> {
                val next = (GameEconomy.cash(prefs).toLong() + 18L)
                    .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                edit.putInt("wallet_cash", next)
            }
            RelicDeal.COINS -> {
                val debtPaid = minOf(5, GameEconomy.debt(prefs))
                val next = (GameEconomy.wallet(prefs).toLong() + 5 - debtPaid)
                    .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                edit.putInt("wallet_coins", next)
                    .putInt(GameEconomy.DebtKey, GameEconomy.debt(prefs) - debtPaid)
            }
            RelicDeal.START_SHIELD -> {
                if (pendingShield(prefs)) return false
                edit.putBoolean(ShieldBoostKey, true)
            }
            RelicDeal.START_MAGNET -> {
                if (pendingMagnet(prefs)) return false
                edit.putBoolean(MagnetBoostKey, true)
            }
        }
        return edit.commit()
    }

    /** Only consumed once at the actual start of the next run. */
    @Synchronized
    fun consumeStartShield(prefs: SharedPreferences): Float {
        if (!pendingShield(prefs)) return 0f
        return if (prefs.edit().putBoolean(ShieldBoostKey, false).commit()) 3f else 0f
    }

    @Synchronized
    fun consumeStartMagnet(prefs: SharedPreferences): Float {
        if (!pendingMagnet(prefs)) return 0f
        return if (prefs.edit().putBoolean(MagnetBoostKey, false).commit()) 6f else 0f
    }

    fun rewardedEligible(prefs: SharedPreferences, roundId: String): Boolean =
        roundId.isNotBlank() &&
            prefs.getString("last_settlement_id", null) == roundId &&
            prefs.getString(AdClaimKey, null) != roundId &&
            (GameSettlement.latestReceipt(prefs)?.relicsBanked ?: 0) > 0 &&
            balance(prefs) < MaxWallet

    /** Call exclusively after Google's rewarded-ad earned callback. */
    @Synchronized
    fun claimAdBonus(prefs: SharedPreferences, roundId: String): Boolean {
        if (!rewardedEligible(prefs, roundId)) return false
        return prefs.edit()
            .putInt(WalletKey, (balance(prefs) + 1).coerceAtMost(MaxWallet))
            .putString(AdClaimKey, roundId)
            .commit()
    }
}
