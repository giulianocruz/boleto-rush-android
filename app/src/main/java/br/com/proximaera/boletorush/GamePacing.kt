package br.com.proximaera.boletorush

/** Deliberately gentle ramp. Chapters change the background, not the game rules. */
internal object GamePacing {
    private fun progress(elapsed: Float, duration: Float): Float =
        (elapsed / duration.coerceAtLeast(1f)).coerceIn(0f, 1f)

    fun chapter(elapsed: Float, duration: Float): Int =
        (progress(elapsed, duration) * 4).toInt().coerceIn(0, 3)

    fun chapterName(index: Int): String = when (index) {
        0 -> "COMEÇO DO MÊS"
        1 -> "DIA DO PAGAMENTO"
        2 -> "FIM DO MÊS"
        else -> "FECHAMENTO DA FATURA"
    }

    fun spawnInterval(elapsed: Float, duration: Float, gamesPlayed: Int): Float =
        (.96f - .16f * progress(elapsed, duration) -
            .002f * gamesPlayed.coerceIn(0, 20)).coerceAtLeast(.75f)

    fun billOdds(elapsed: Float, duration: Float, gamesPlayed: Int): Float =
        (.065f + .16f * progress(elapsed, duration) +
            .0015f * gamesPlayed.coerceIn(0, 20)).coerceAtMost(.255f)

    // Canvas gameplay region begins at y=.195. Coins must be seen before the magnet scores.
    fun magnetCanCollect(y: Float): Boolean = y >= .40f
}
