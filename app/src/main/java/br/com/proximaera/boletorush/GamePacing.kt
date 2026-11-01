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

    // Gentle progression: a round speeds up by at most ~13%, even for experienced players.
    fun spawnInterval(elapsed: Float, duration: Float, gamesPlayed: Int): Float =
        (.98f - .10f * progress(elapsed, duration) -
            .0015f * gamesPlayed.coerceIn(0, 20)).coerceAtLeast(.84f)

    fun billOdds(elapsed: Float, duration: Float, gamesPlayed: Int): Float =
        (.065f + .10f * progress(elapsed, duration) +
            .001f * gamesPlayed.coerceIn(0, 20)).coerceAtMost(.185f)

    /** Objects no longer accelerate twice as much in 90s mode as in 45s. */
    fun dropSpeed(elapsed: Float, duration: Float, gamesPlayed: Int, roll: Float): Float =
        .275f + .070f * roll.coerceIn(0f, 1f) +
        .045f * progress(elapsed, duration) +
        .004f * (gamesPlayed.coerceIn(0, 20) / 20f)

    // Canvas gameplay region begins at y=.195. Coins must be seen before the magnet scores.
    fun magnetCanCollect(y: Float): Boolean = y >= .40f
}
