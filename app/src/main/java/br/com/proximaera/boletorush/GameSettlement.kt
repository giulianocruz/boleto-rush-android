package br.com.proximaera.boletorush

import android.content.SharedPreferences

/** A receipt is returned only after the complete local transaction is committed. */
internal data class GameReceipt(
    val gamesPlayed: Int,
    val best: Int,
    val coinsBanked: Int,
    val notesBanked: Int,
    val creditGranted: Int,
    val debtRepaid: Int,
    val wallet: Int,
    val notes: Int,
    val credit: Int,
    val debt: Int,
    val unlocked: List<Milestone>
)

/**
 * Finishes a game in ONE SharedPreferences.Editor.commit() transaction.
 * Neither a second callback nor a process interruption can partially record
 * coins, salary, milestones, cards or best score.
 */
internal object GameSettlement {
    private const val LastIdKey = "last_settlement_id"
    private const val CoinWalletKey = "wallet_coins"
    private const val NoteWalletKey = "wallet_cash"
    private const val CreditKey = "store_credit"

    @Synchronized
    fun settle(
        prefs: SharedPreferences,
        roundId: String,
        modeIsMarathon: Boolean,
        score: Int,
        coinsCollected: Int,
        notesCollected: Int,
        pendingCardCredit: Int,
        incomeLevel: Int,
        victory: Boolean
    ): GameReceipt? {
        require(roundId.isNotBlank()) { "The round must have a unique identifier" }
        if (prefs.getString(LastIdKey, null) == roundId) return null

        val earned = GameEconomy.earnings(coinsCollected, incomeLevel)
        val milestones = Milestone.entries.filter {
            it.reached(score, coinsCollected, victory) && !GameMilestones.claimed(prefs, it)
        }
        val milestoneCoins = milestones.sumOf { it.reward }

        val previousDebt = GameEconomy.debt(prefs)
        val paidByRun = minOf(earned, previousDebt)
        val debtAfterRun = previousDebt - paidByRun
        val paidByMilestones = minOf(milestoneCoins, debtAfterRun)
        val nextDebt = debtAfterRun - paidByMilestones
        val coinsBanked = earned - paidByRun
        val milestoneBanked = milestoneCoins - paidByMilestones
        val nextCoins = (GameEconomy.wallet(prefs).toLong() +
            coinsBanked + milestoneBanked).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

        val notesBanked = notesCollected.coerceAtLeast(0)
        val nextNotes = (GameEconomy.cash(prefs).toLong() + notesBanked)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

        val availableCredit = GameEconomy.credit(prefs)
        val room = (25L - nextDebt - availableCredit).coerceIn(0L, 25L).toInt()
        val creditGranted = pendingCardCredit.coerceIn(0, room)
        val nextCredit = availableCredit + creditGranted

        val nextGames = (prefs.getInt("games", 0).toLong() + 1)
            .coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        val bestKey = if (modeIsMarathon) "best_marathon" else "best"
        val nextBest = maxOf(score.coerceAtLeast(0), prefs.getInt(bestKey, 0))

        val editor = prefs.edit()
            .putString(LastIdKey, roundId)
            .putInt(CoinWalletKey, nextCoins)
            .putInt(NoteWalletKey, nextNotes)
            .putInt(CreditKey, nextCredit)
            .putInt(GameEconomy.DebtKey, nextDebt)
            .putInt("games", nextGames)
            .putInt(bestKey, nextBest)
        milestones.forEach { editor.putBoolean(it.key, true) }
        if (!editor.commit()) return null
        return GameReceipt(nextGames, nextBest, coinsBanked, notesBanked,
            creditGranted, previousDebt - nextDebt, nextCoins,
            nextNotes, nextCredit, nextDebt, milestones)
    }
}
