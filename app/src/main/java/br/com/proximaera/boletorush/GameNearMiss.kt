package br.com.proximaera.boletorush

import kotlin.math.abs

/**
 * A small skill-only score bonus for an intentional close dodge.
 *
 * Awarded at the wallet collision line (once per crossing) and never
 * mints coins, notes, XP, or an advertising reward.
 */
internal object GameNearMiss {
    const val WalletY = .80f
    const val MinimumGap = .125f // Just outside the wallet contact radius (.118).
    const val MaximumGap = .215f
    const val MovementGraceSeconds = .80f

    fun qualified(
        kind: Kind,
        previousY: Float,
        nextY: Float,
        itemX: Float,
        playerX: Float,
        secondsSinceMovement: Float,
        protectionActive: Boolean
    ): Boolean {
        if (!GameDrops.harmful(kind) || protectionActive) return false
        if (!previousY.isFinite() || !nextY.isFinite() ||
            !itemX.isFinite() || !playerX.isFinite() ||
            !secondsSinceMovement.isFinite()) return false
        if (secondsSinceMovement < 0f || secondsSinceMovement > MovementGraceSeconds) return false
        if (previousY >= WalletY || nextY < WalletY) return false
        val gap = abs(itemX - playerX)
        return gap >= MinimumGap && gap <= MaximumGap
    }

    const val FlashDuration = .72f

    /** Stable visual animation curves; invalid times must never leak NaN into Canvas. */
    fun flashAlpha(remaining: Float): Float =
        if (!remaining.isFinite()) 0f else (remaining / FlashDuration).coerceIn(0f, 1f)

    fun flashRadius(remaining: Float, streak: Int): Float =
        1.7f + .8f * (1f - flashAlpha(remaining)) + if (streak >= 3) .22f else 0f

    fun flashLabel(streak: Int, points: Int): String =
        (if (streak >= 3) "SEQUÊNCIA x${streak.coerceIn(3, 5)}" else "RASPÃO!") +
            " +${points.coerceIn(0, 15)}"

    /** A bounded streak raises score slowly, never the permanent wallet. */
    fun points(streak: Int): Int = 5 + 2 * streak.coerceIn(1, 5)
}
