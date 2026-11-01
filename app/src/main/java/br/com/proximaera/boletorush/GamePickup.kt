package br.com.proximaera.boletorush

import kotlin.math.abs

/**
 * The score is strictly tied to a coin that reached the visible wallet lane.
 * Never credit points solely because a magnet is active or an item was spawned.
 */
internal object GamePickup {
    fun coinContact(
        previousY: Float, nextY: Float, itemX: Float, playerX: Float,
        magnetCaughtAtWallet: Boolean
    ): Boolean {
        if (!nextY.isFinite() || !itemX.isFinite() || !playerX.isFinite()) return false
        // Objects are visible in the active playfield by y = .4.
        if (previousY < .40f || nextY < .40f || previousY >= .92f) return false
        val crossing = previousY < .80f && nextY >= .80f &&
            abs(itemX - playerX) < .118f
        return crossing || magnetCaughtAtWallet && GameMagnet.reachedWallet(itemX,nextY,playerX)
    }
}
