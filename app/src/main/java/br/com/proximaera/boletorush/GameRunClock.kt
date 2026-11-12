package br.com.proximaera.boletorush

/**
 * Do not advance gameplay behind rewarded ads or when another app is in front.
 * Each return to foreground starts with a fresh frame baseline.
 */
internal object GameRunClock {
    const val MaxFrameSeconds = 0.05f

    fun shouldAdvance(foreground: Boolean, playing: Boolean): Boolean =
        foreground && playing

    fun frameSeconds(previousFrameNanos: Long, nowFrameNanos: Long): Float {
        if (nowFrameNanos <= previousFrameNanos) return 0f
        return ((nowFrameNanos - previousFrameNanos) / 1_000_000_000f)
            .coerceIn(0f, MaxFrameSeconds)
    }
}
