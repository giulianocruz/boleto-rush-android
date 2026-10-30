package br.com.proximaera.boletorush

import android.content.SharedPreferences

/**
 * Permanent, single-claim achievements that reward offline play.
 * Awards are local game coins only, never real money or paid currency.
 */
internal enum class Milestone(
    val key: String,
    val title: String,
    val goal: String,
    val reward: Int
) {
    COLLECTOR("milestone_15_coins", "PRIMEIRA RESERVA", "Colete 15 moedas em uma partida", 20),
    SCORER("milestone_350_score", "BOLSO CHEIO", "Faça 350 pontos em uma partida", 25),
    SURVIVOR("milestone_first_win", "FATURA VENCIDA", "Sobreviva até o fim de uma partida", 40);

    fun reached(score: Int, collectedCoins: Int, victory: Boolean): Boolean = when (this) {
        COLLECTOR -> collectedCoins >= 15
        SCORER -> score >= 350
        SURVIVOR -> victory
    }
}

internal data class MilestoneAward(val unlocked: List<Milestone> = emptyList()) {
    val coins: Int get() = unlocked.sumOf { it.reward }
}

internal object GameMilestones {
    fun claimed(prefs: SharedPreferences, milestone: Milestone): Boolean =
        prefs.getBoolean(milestone.key, false)

    /**
     * Wallet and claim flags are committed in the same preference transaction.
     * Repeated calls never pay a claimed milestone a second time.
     */
    @Synchronized
    fun awardRound(
        prefs: SharedPreferences, score: Int, collectedCoins: Int, victory: Boolean
    ): MilestoneAward {
        val unlocked = Milestone.entries.filter { milestone ->
            milestone.reached(score, collectedCoins, victory) && !claimed(prefs, milestone)
        }
        if (unlocked.isEmpty()) return MilestoneAward()
        val award = MilestoneAward(unlocked)
        val newWallet = (GameEconomy.wallet(prefs).toLong() + award.coins)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val edit = prefs.edit().putInt("wallet_coins", newWallet)
        unlocked.forEach { edit.putBoolean(it.key, true) }
        return if (edit.commit()) award else MilestoneAward()
    }

    fun claimedCount(prefs: SharedPreferences): Int =
        Milestone.entries.count { claimed(prefs, it) }
}
