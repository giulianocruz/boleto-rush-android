package br.com.proximaera.boletorush

import java.util.Random
import kotlin.math.abs
import kotlin.math.sin

/**
 * CLI QA harness: no emulator, internet, Android SDK or GitHub Actions required.
 *
 * Build with the exact production GamePacing.kt + GameDrops.kt sources:
 * kotlinc GamePacing.kt GameDrops.kt tools/qa/DropSimulation.kt -include-runtime -d drops.jar
 * java -jar drops.jar
 */
object DropSimulation {
    @JvmStatic
    fun main(args: Array<String>) {
        for (duration in listOf(45f, 90f)) {
            val totals = Kind.entries.associateWith { 0 }.toMutableMap()
            var totalDrops = 0
            var minimumCoins = Int.MAX_VALUE
            for (round in 0 until 100) {
                val rng = Random(round.toLong() + duration.toLong() * 100)
                var elapsed = 0f
                var streak = 0
                var lastWasHazard = false
                var coins = 0
                val chapters = mutableSetOf<Int>()
                while (elapsed < duration) {
                    val player = (.5f + .30f * sin(elapsed.toDouble()).toFloat())
                        .coerceIn(.09f, .91f)
                    val kind = GameDrops.choose(
                        rng.nextFloat(), rng.nextFloat(), elapsed, duration,
                        8, lastWasHazard, streak
                    )
                    chapters.add(GamePacing.chapter(elapsed, duration))
                    val x = GameDrops.spawnX(rng.nextFloat(), kind, player)
                    check(x in .09f..91f) { "Off-screen pickup: $kind x=$x" }
                    if (GameDrops.harmful(kind)) {
                        check(!lastWasHazard) { "Back-to-back hazards" }
                        check(elapsed >= 5f) { "Hazard in onboarding" }
                        check(abs(x - player) >= .179f) { "Hazard spawned over player" }
                    }
                    if (kind == Kind.COIN) {
                        coins++
                        streak = 0
                    } else {
                        streak++
                        check(streak <= 3) { "Too many non-coin drops" }
                    }
                    totals[kind] = totals.getValue(kind) + 1
                    totalDrops++
                    lastWasHazard = GameDrops.harmful(kind)
                    elapsed += GamePacing.spawnInterval(elapsed, duration, 8)
                }
                check(chapters.size == 4) { "Chapter transition missing" }
                check(coins >= (duration / 4.5f).toInt()) { "Coins too rare" }
                minimumCoins = minOf(minimumCoins, coins)
            }
            check(totals.getValue(Kind.PAYDAY) > 0) { "Salary bonus never spawns" }
            check(totals.getValue(Kind.PIX) > 0) { "PIX bonus never spawns" }
            println("Boleto Rush / ${duration.toInt()}s: 100 simulated rounds, " +
                "$totalDrops drops, minimum coins/round=$minimumCoins")
            println(Kind.entries.joinToString(" | ") { "${it.name}=${totals.getValue(it)}" })
        }
        println("PASS: spawn fairness and pickup variety (200 complete rounds)")
    }
}
