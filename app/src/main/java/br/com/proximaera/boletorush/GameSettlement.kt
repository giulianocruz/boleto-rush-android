package br.com.proximaera.boletorush

import android.content.SharedPreferences

/** The actual amounts committed to disk in one local game settlement. */
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
    val unlocked: List<Milestone>,
    val relicsBanked: Int = 0,
    val relicWallet: Int = 0,
    val weeklyNotesAward: Int = 0,
    val albumUnlocked: Boolean = false
)

/**
 * One transaction for coins, notes, cards, milestones, debt and record.
 * A durable receipt permits retrying the most recent round even if its first
 * commit succeeded but the app was interrupted before displaying the result.
 * A bounded history also rejects old round IDs (128 recent settlements).
 */
internal object GameSettlement {
    private const val LastIdKey = "last_settlement_id"
    private const val ReceiptKey = "last_settlement_receipt_v2"
    private const val HistoryKey = "settled_round_ids_v2"
    private const val HistoryLimit = 128
    private const val CoinWalletKey = "wallet_coins"
    private const val NoteWalletKey = "wallet_cash"
    private const val CreditKey = "store_credit"

    private fun encode(receipt: GameReceipt): String = listOf(
        "3", receipt.gamesPlayed, receipt.best, receipt.coinsBanked,
        receipt.notesBanked, receipt.creditGranted, receipt.debtRepaid,
        receipt.wallet, receipt.notes, receipt.credit, receipt.debt,
        receipt.unlocked.joinToString(",") { it.name }.ifEmpty { "-" },
        receipt.relicsBanked, receipt.relicWallet,
        receipt.weeklyNotesAward, if (receipt.albumUnlocked) 1 else 0
    ).joinToString("|")

    private fun decode(value: String?): GameReceipt? {
        if (value == null) return null
        val parts = value.split('|')
        val legacy = parts.size == 12 && parts[0] == "1"
        val modern = parts.size == 14 && parts[0] == "2"
        val album = parts.size == 16 && parts[0] == "3"
        if (!legacy && !modern && !album) return null
        val numbers = parts.subList(1, 11).map { it.toIntOrNull() ?: return null }
        val names = if (parts[11] == "-") emptyList() else parts[11].split(',')
        val unlocked = Milestone.entries.filter { it.name in names }
        if (unlocked.size != names.size) return null
        val banked = if (modern || album) parts[12].toIntOrNull() ?: return null else 0
        val balance = if (modern || album) parts[13].toIntOrNull() ?: return null else 0
        val weeklyNotes = if (album) parts[14].toIntOrNull() ?: return null else 0
        val unlockFlag = if (album) parts[15].toIntOrNull() ?: return null else 0
        if (banked !in 0..GameRelics.MaxPerRound || balance !in 0..GameRelics.MaxWallet ||
            weeklyNotes !in 0..GameRelicAlbum.WeeklyNotesReward || unlockFlag !in 0..1) return null
        return GameReceipt(
            numbers[0], numbers[1], numbers[2], numbers[3], numbers[4],
            numbers[5], numbers[6], numbers[7], numbers[8], numbers[9],
            unlocked, banked, balance, weeklyNotes, unlockFlag == 1
        )
    }

    /** Available again after reopening the app; no round needs to be replayed. */
    fun latestReceipt(prefs: SharedPreferences): GameReceipt? =
        decode(prefs.getString(ReceiptKey, null))

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
        victory: Boolean,
        relicsCollected: Int = 0,
        relicBronze: Int = 0,
        relicSilver: Int = 0,
        relicGold: Int = 0
    ): GameReceipt? {
        require(roundId.isNotBlank() && '\n' !in roundId && '\r' !in roundId) {
            "The round must have a valid unique identifier"
        }

        val lastId = prefs.getString(LastIdKey, null)
        // A retried save must show the original receipt, not pay twice or falsely fail.
        if (lastId == roundId) return decode(prefs.getString(ReceiptKey, null))

        val history = prefs.getString(HistoryKey, "")
            .orEmpty().split('\n').filter { it.isNotBlank() }
        if (roundId in history) return null // Older settled round: reject any replay.

        // Upgrade path from v1.7.6: preserve its most recent ID in the ledger.
        val priorIds = (history + listOfNotNull(lastId)).distinct().takeLast(HistoryLimit - 1)

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
        val albumUpdate = GameRelicAlbum.planSettlement(prefs,
            relicBronze, relicSilver, relicGold,
            minOf(GameRelics.bankable(relicsCollected),
                GameRelics.MaxWallet - GameRelics.balance(prefs)))
        val nextNotes = (GameEconomy.cash(prefs).toLong() +
            notesBanked + albumUpdate.weeklyNotesAward)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

        val relicBalance = GameRelics.balance(prefs)
        val relicsBanked = minOf(GameRelics.bankable(relicsCollected),
            GameRelics.MaxWallet - relicBalance)
        val nextRelicBalance = relicBalance + relicsBanked

        val availableCredit = GameEconomy.credit(prefs)
        val room = (25L - nextDebt - availableCredit).coerceIn(0L, 25L).toInt()
        val creditGranted = pendingCardCredit.coerceIn(0, room)
        val nextCredit = availableCredit + creditGranted

        val nextGames = (prefs.getInt("games", 0).toLong() + 1)
            .coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        val bestKey = if (modeIsMarathon) "best_marathon" else "best"
        val nextBest = maxOf(score.coerceAtLeast(0), prefs.getInt(bestKey, 0))

        val receipt = GameReceipt(nextGames, nextBest, coinsBanked, notesBanked,
            creditGranted, previousDebt - nextDebt, nextCoins,
            nextNotes, nextCredit, nextDebt, milestones,
            relicsBanked, nextRelicBalance,
            albumUpdate.weeklyNotesAward, albumUpdate.newlyCompletedCollection)

        val editor = prefs.edit()
            .putString(LastIdKey, roundId)
            .putString(ReceiptKey, encode(receipt))
            .putString(HistoryKey, (priorIds + roundId).joinToString("\n"))
            .putInt(CoinWalletKey, nextCoins)
            .putInt(NoteWalletKey, nextNotes)
            .putInt(GameRelics.WalletKey, nextRelicBalance)
            .putInt(CreditKey, nextCredit)
            .putInt(GameEconomy.DebtKey, nextDebt)
            .putInt("games", nextGames)
            .putInt(bestKey, nextBest)
        GameRelicAlbum.write(editor, albumUpdate)
        milestones.forEach { editor.putBoolean(it.key, true) }
        if (!editor.commit()) return null
        return receipt
    }
}
