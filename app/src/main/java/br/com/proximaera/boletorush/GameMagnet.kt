package br.com.proximaera.boletorush

import kotlin.math.abs

/**
 * Visible, frame-based magnet collection. No points are credited before the coin
 * visibly reaches the wallet. Pure logic so QA can verify it without Android.
 */
internal object GameMagnet {
    internal data class Position(val x: Float, val y: Float)

    fun pull(
        x: Float, y: Float, walletX: Float, frameSeconds: Float, active: Boolean
    ): Position {
        val p = Position(x.coerceIn(.09f, .91f), y)
        if (!active || !GamePacing.magnetCanCollect(y)) return p
        val dt = frameSeconds.coerceIn(0f, .05f)
        val horizontal = (dt * 4.2f).coerceIn(0f, 1f)
        val vertical = (dt * 3.0f).coerceIn(0f, 1f)
        val targetX = walletX.coerceIn(.09f, .91f)
        val targetY = .82f
        return Position(
            (p.x + (targetX - p.x) * horizontal).coerceIn(.09f, .91f),
            p.y + (targetY - p.y).coerceAtLeast(0f) * vertical
        )
    }

    fun reachedWallet(x: Float, y: Float, walletX: Float): Boolean =
        y >= .755f && y <= .92f && abs(x - walletX) <= .112f
}
