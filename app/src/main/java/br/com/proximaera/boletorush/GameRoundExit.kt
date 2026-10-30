package br.com.proximaera.boletorush

import kotlin.math.ceil

/** A voluntary early exit is an earned option, not a way to farm a first pickup. */
internal object GameRoundExit {
    fun canCashOut(elapsed: Float, duration: Float): Boolean =
        elapsed >= duration.coerceAtLeast(1f) * .5f

    fun secondsUntilCashOut(elapsed: Float, duration: Float): Int =
        ceil((duration.coerceAtLeast(1f) * .5f - elapsed).coerceAtLeast(0f)).toInt()

    /** Every coin and note collected before a qualified withdrawal is banked once. */
    fun eligibleCoins(collected: Int): Int = collected.coerceAtLeast(0)
    fun eligibleCash(collected: Int): Int = collected.coerceAtLeast(0)

    fun resultTitle(victory: Boolean, cashedOut: Boolean): String = when {
        victory -> "SOBREVIVEU!"
        cashedOut -> "SALDO RESGATADO!"
        else -> "A FATURA CHEGOU!"
    }
}

/** On-screen labels must show all simultaneous powers, not only the highest-priority one. */
internal object GamePowerStatus {
    fun labels(pix: Float, magnet: Float, shield: Float): List<String> = buildList {
        if (pix > 0f) add("PIX 2x ${seconds(pix)}s")
        if (magnet > 0f) add("ÍMÃ ${seconds(magnet)}s")
        if (shield > 0f) add("SALDO ${seconds(shield)}s")
    }
    private fun seconds(value: Float): Int = ceil(value.coerceAtLeast(0f)).toInt()
}
