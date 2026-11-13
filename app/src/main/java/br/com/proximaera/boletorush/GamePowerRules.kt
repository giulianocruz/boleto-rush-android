package br.com.proximaera.boletorush

/** Power-ups are brief, forgiving, and never change permanent currency directly. */
internal object GamePowerRules {
    const val SprintDuration = 5f
    const val SlowDuration = 5f
    const val ArmorDuration = 12f
    const val ArmorHits = 2

    /**
     * A beneficial pickup must never shorten a shield already earned with
     * permanent upgrades or relics (up to nine seconds).
     */
    fun refreshShieldSeconds(remaining: Float, pickupSeconds: Float = 7f): Float {
        val current = if (remaining.isFinite()) remaining.coerceIn(0f, 9f) else 0f
        val pickup = if (pickupSeconds.isFinite()) pickupSeconds.coerceIn(0f, 9f) else 0f
        return maxOf(current, pickup)
    }

    fun playerResponse(turboSeconds: Float): Float =
        if (turboSeconds > 0f) 26f else 17f

    fun fallingSpeedFactor(slowSeconds: Float): Float =
        if (slowSeconds > 0f) .62f else 1f

    fun armorRemaining(hits: Int, armorSeconds: Float): Int =
        if (armorSeconds > 0f) hits.coerceIn(0, ArmorHits) else 0

    fun screenClear(kind: Kind): Boolean = GameDrops.harmful(kind)

    /** Sort the drop list only when a defensive pickup crosses the wallet lane.
     *  A shield still falling elsewhere must not trigger allocations every frame.
     */
    fun defensivePickupWillCollide(
        kind: Kind, previousY: Float, nextY: Float, itemX: Float, playerX: Float
    ): Boolean = collisionPriority(kind) < 2 &&
        previousY < .80f && nextY >= .80f &&
        nextY.isFinite() && itemX.isFinite() && playerX.isFinite() &&
        kotlin.math.abs(itemX - playerX) < .118f

    /** Handle helpful pickups before hazards in the same frame.
     *  Prevents an armor or screen-clear collected together with a bill
     *  from unfairly consuming a life because of insertion order.
     */
    fun collisionPriority(kind: Kind): Int = when (kind) {
        Kind.SCREEN_CLEAR -> 0
        Kind.ARMOR, Kind.SHIELD, Kind.HEART -> 1
        Kind.BILL, Kind.OVERDRAFT -> 3
        else -> 2
    }
}
