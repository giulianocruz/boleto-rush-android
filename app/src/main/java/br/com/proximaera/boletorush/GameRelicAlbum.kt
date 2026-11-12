package br.com.proximaera.boletorush

import android.content.SharedPreferences
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Album progress and weekly missions are game-only and stored locally.
 * A round's additions are prepared here and persisted by GameSettlement
 * in the same editor as the usual wallet balances and round receipt.
 */
internal data class RelicAlbumProgress(
    val bronze: Int,
    val silver: Int,
    val gold: Int,
    val week: String,
    val weeklyFragments: Int,
    val weeklyClaimed: Boolean
)

internal data class RelicAlbumUpdate(
    val progress: RelicAlbumProgress,
    val weeklyNotesAward: Int,
    val newlyCompletedCollection: Boolean
)

internal object GameRelicAlbum {
    private const val BronzeKey = "relic_album_bronze_v1"
    private const val SilverKey = "relic_album_silver_v1"
    private const val GoldKey = "relic_album_gold_v1"
    private const val WeekKey = "relic_week_key_v1"
    private const val WeekProgressKey = "relic_week_progress_v1"
    private const val WeekClaimedKey = "relic_week_claimed_v1"

    const val BronzeGoal = 5
    const val SilverGoal = 3
    const val GoldGoal = 1
    const val WeeklyGoal = 8
    const val WeeklyNotesReward = 16

    fun weekId(day: LocalDate): String {
        val iso = WeekFields.ISO
        val weekYear = day.get(iso.weekBasedYear())
        val week = day.get(iso.weekOfWeekBasedYear())
        return String.format(Locale.US, "%04d-W%02d", weekYear, week)
    }

    /** Never go back to an older week when a device's clock moves backwards. */
    fun progress(prefs: SharedPreferences, day: LocalDate = LocalDate.now()): RelicAlbumProgress {
        val dateWeek = weekId(day)
        val savedWeek = prefs.getString(WeekKey, "").orEmpty()
        val current = maxOf(dateWeek, savedWeek)
        val newWeek = current != savedWeek
        return RelicAlbumProgress(
            prefs.getInt(BronzeKey, 0).coerceAtLeast(0),
            prefs.getInt(SilverKey, 0).coerceAtLeast(0),
            prefs.getInt(GoldKey, 0).coerceAtLeast(0),
            current,
            if (newWeek) 0 else prefs.getInt(WeekProgressKey, 0).coerceIn(0, WeeklyGoal),
            if (newWeek) false else prefs.getBoolean(WeekClaimedKey, false)
        )
    }

    fun complete(p: RelicAlbumProgress): Boolean =
        p.bronze >= BronzeGoal && p.silver >= SilverGoal && p.gold >= GoldGoal

    private fun addBounded(a: Int, b: Int): Int =
        (a.toLong() + b.coerceAtLeast(0).toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    fun planSettlement(
        prefs: SharedPreferences,
        bronze: Int,
        silver: Int,
        gold: Int,
        collectedFragments: Int,
        day: LocalDate = LocalDate.now()
    ): RelicAlbumUpdate {
        val before = progress(prefs, day)
        val items = listOf(bronze, silver, gold)
        require(items.all { it >= 0 && it <= GameRelics.MaxPerRound })
        require(collectedFragments in 0..GameRelics.MaxPerRound)
        val afterBronze = addBounded(before.bronze, bronze)
        val afterSilver = addBounded(before.silver, silver)
        val afterGold = addBounded(before.gold, gold)
        val earnedWeek = before.weeklyFragments + collectedFragments
        val progressWeek = earnedWeek.coerceAtMost(WeeklyGoal)
        val currentWeek = weekId(day)
        val reached = !before.weeklyClaimed && progressWeek >= WeeklyGoal &&
            currentWeek >= before.week // No old-week rewards after clock rollback
        val after = RelicAlbumProgress(
            afterBronze, afterSilver, afterGold, before.week,
            progressWeek, before.weeklyClaimed || reached
        )
        return RelicAlbumUpdate(
            after,
            if (reached) WeeklyNotesReward else 0,
            !complete(before) && complete(after)
        )
    }

    fun write(editor: SharedPreferences.Editor, update: RelicAlbumUpdate): SharedPreferences.Editor {
        val p = update.progress
        return editor.putInt(BronzeKey, p.bronze)
            .putInt(SilverKey, p.silver)
            .putInt(GoldKey, p.gold)
            .putString(WeekKey, p.week)
            .putInt(WeekProgressKey, p.weeklyFragments)
            .putBoolean(WeekClaimedKey, p.weeklyClaimed)
    }
}
