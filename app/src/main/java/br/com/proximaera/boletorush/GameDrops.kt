package br.com.proximaera.boletorush

/** Pure drop rules, separated from Compose for automated regression tests. */
internal enum class Kind { COIN, BANKNOTE, CARD, OVERDRAFT, BILL, SHIELD, HEART, MAGNET, PIX }

internal object GameDrops {
    fun harmful(kind: Kind): Boolean = kind == Kind.BILL || kind == Kind.OVERDRAFT

    /**
     * No dry spells longer than three non-coins, no successive hazards,
     * and no bills until after the player has learned to move.
     */
    fun choose(
        itemRoll: Float, hazardRoll: Float, elapsed: Float, duration: Float,
        gamesPlayed: Int, previousWasHazard: Boolean, nonCoinStreak: Int
    ): Kind {
        if (nonCoinStreak >= 3) return Kind.COIN
        val stage = GamePacing.chapter(elapsed, duration)
        return when {
            itemRoll < .040f -> Kind.SHIELD
            itemRoll < .065f -> Kind.HEART
            itemRoll < .105f && elapsed >= 6f -> Kind.MAGNET
            itemRoll < (if (stage == 1) .225f else .19f) -> Kind.BANKNOTE
            itemRoll < .240f && stage >= 1 -> Kind.CARD
            itemRoll < .275f && stage >= 2 && !previousWasHazard -> Kind.OVERDRAFT
            itemRoll < .315f && elapsed >= 7f -> Kind.PIX
            !previousWasHazard && elapsed >= 5f &&
                hazardRoll < GamePacing.billOdds(elapsed, duration, gamesPlayed) -> Kind.BILL
            else -> Kind.COIN
        }
    }

    /** Score is earned only when a visible coin is collected. Powers and notes have other rewards. */
    fun coinScore(combo: Int, pixActive: Boolean): Int {
        val base = 10 + (combo.coerceIn(1, 10) - 1) * 2
        return if (pixActive) base * 2 else base
    }
}
