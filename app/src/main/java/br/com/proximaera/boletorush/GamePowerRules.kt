package br.com.proximaera.boletorush

/** Power-ups are brief, forgiving, and never change permanent currency directly. */
internal object GamePowerRules {
    const val SprintDuration = 5f
    const val SlowDuration = 5f
    const val ArmorDuration = 12f
    const val ArmorHits = 2

    fun playerResponse(turboSeconds: Float): Float =
        if (turboSeconds > 0f) 26f else 17f

    fun fallingSpeedFactor(slowSeconds: Float): Float =
        if (slowSeconds > 0f) .62f else 1f

    fun armorRemaining(hits: Int, armorSeconds: Float): Int =
        if (armorSeconds > 0f) hits.coerceIn(0, ArmorHits) else 0

    fun screenClear(kind: Kind): Boolean = GameDrops.harmful(kind)
}
