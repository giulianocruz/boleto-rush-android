package br.com.proximaera.boletorush

import android.content.SharedPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** The boss has three predictable waves, with exactly one safe lane per wave. */
internal data class MonthBossWave(
    val number: Int,
    val safeLane: Int,
    val safeX: Float,
    val hazardXs: List<Float>,
    val hazardKind: Kind
)

internal data class MonthStampProgress(
    val lastAwardedMonth: String,
    val earnedStamps: Int
)

internal data class MonthStampUpdate(
    val month: String,
    val awarded: Boolean,
    val notesAward: Int,
    val stampCount: Int
)

/** A calendar-month collectible. No online server, purchase, or advertising required. */
internal object GameMonthChallenge {
    private const val LastMonthKey = "month_boss_last_award_v1"
    private const val StampCountKey = "month_boss_stamp_count_v1"
    private val MonthFormat = DateTimeFormatter.ofPattern("uuuu-MM")

    const val WaveCount = 3
    const val HazardsPerWave = 2
    const val RequiredHazardsResolved = WaveCount * HazardsPerWave
    const val FinalWindowSeconds = 18f
    const val WarningLeadSeconds = 5f
    const val WaveSpacingSeconds = 6f
    const val NotesAward = 25
    const val MaxStamps = 999

    fun monthId(day: LocalDate = LocalDate.now()): String = day.format(MonthFormat)

    /** A single run must finish its full duration to earn the month-end stamp. */
    fun waveIndex(elapsed: Float, duration: Float): Int {
        if (!elapsed.isFinite() || !duration.isFinite() ||
            duration < FinalWindowSeconds || elapsed >= duration) return -1
        val fromStart = elapsed - (duration - FinalWindowSeconds)
        if (fromStart < 0f) return -1
        return (fromStart / WaveSpacingSeconds).toInt().coerceIn(0, WaveCount - 1)
    }

    /**
     * Warn before the boss begins, so a player has time to position.
     * The warning and challenge window must never spawn an optional ad gate.
     */
    fun warningSeconds(elapsed: Float, duration: Float): Int {
        if (!elapsed.isFinite() || !duration.isFinite() ||
            duration <= FinalWindowSeconds + WarningLeadSeconds) return 0
        val timeLeft = duration - FinalWindowSeconds - elapsed
        if (timeLeft <= 0f || timeLeft > WarningLeadSeconds) return 0
        return kotlin.math.ceil(timeLeft.toDouble()).toInt().coerceIn(1, 5)
    }

    fun adBreakBlocked(elapsed: Float, duration: Float): Boolean =
        elapsed.isFinite() && duration.isFinite() &&
            duration > FinalWindowSeconds + WarningLeadSeconds &&
            elapsed >= duration - FinalWindowSeconds - WarningLeadSeconds &&
            elapsed < duration

    fun waveTitle(index: Int): String = "FECHAMENTO | ONDA ${index + 1}/$WaveCount"

    /** A resolved danger may have been dodged or safely removed by a power. */
    fun progressLabel(hazardsResolved: Int, contacts: Int): String {
        val count = hazardsResolved.coerceIn(0, RequiredHazardsResolved)
        return "PERIGOS $count/$RequiredHazardsResolved • " +
            if (contacts > 0) "SELO EM RISCO" else "SEM CONTATOS"
    }

    /** Show each completed wave once, never ahead of waves actually launched. */
    fun completedWaves(hazardsResolved: Int, wavesLaunched: Int): Int =
        (hazardsResolved.coerceIn(0, RequiredHazardsResolved) / HazardsPerWave)
            .coerceAtMost(wavesLaunched.coerceIn(0, WaveCount))

    fun waveCompletionLabel(completed: Int, contacts: Int): String {
        require(completed in 1..WaveCount)
        return if (contacts == 0) "ONDA $completed/$WaveCount SUPERADA! CONTINUE SEM TOCAR NOS PERIGOS"
        else "ONDA $completed/$WaveCount CONCLUÍDA • SELO EXIGE ZERO CONTATOS"
    }

    /** Hazards count once only when they cross the wallet or get cleared before it. */
    fun hazardResolved(previousY: Float, nextY: Float, screenCleared: Boolean): Boolean =
        previousY.isFinite() && nextY.isFinite() &&
            previousY < .80f && (screenCleared || nextY >= .80f)

    fun safeLaneLabel(index: Int): String = when (index) {
        0 -> "ESQUERDA"
        1 -> "CENTRO"
        2 -> "DIREITA"
        else -> "INDEFINIDA"
    }

    fun laneX(index: Int): Float = when (index) {
        0 -> .18f
        1 -> .50f
        2 -> .82f
        else -> throw IllegalArgumentException("Invalid lane")
    }

    /** Vary lane order by month without random, impossible cross-lane patterns. */
    fun wave(index: Int, month: String): MonthBossWave {
        require(index in 0 until WaveCount) { "Invalid boss wave" }
        val monthNumber = month.substringAfter('-', "").toIntOrNull()
            ?.takeIf { it in 1..12 } ?: 1
        val safeLane = (index + monthNumber) % 3
        return MonthBossWave(
            number = index + 1,
            safeLane = safeLane,
            safeX = laneX(safeLane),
            hazardXs = (0..2).filter { it != safeLane }.map(::laneX),
            hazardKind = if (index == 1) Kind.OVERDRAFT else Kind.BILL
        )
    }

    /**
     * A launch alone is not a survived wave: all six hazard lanes must cross
     * the wallet collision line or be safely removed by a screen-clear power.
     */
    fun qualified(
        victory: Boolean, wavesLaunched: Int, bossContacts: Int,
        hazardsResolved: Int
    ): Boolean =
        victory && wavesLaunched == WaveCount && bossContacts == 0 &&
            hazardsResolved == RequiredHazardsResolved

    fun progress(prefs: SharedPreferences): MonthStampProgress =
        MonthStampProgress(
            prefs.getString(LastMonthKey, "").orEmpty(),
            prefs.getInt(StampCountKey, 0).coerceIn(0, MaxStamps)
        )

    /** Plan only; GameSettlement persists the plan in its own atomic editor. */
    fun plan(
        prefs: SharedPreferences,
        victory: Boolean,
        wavesLaunched: Int,
        bossContacts: Int,
        challengeMonth: String,
        today: LocalDate = LocalDate.now(),
        hazardsResolved: Int = 0
    ): MonthStampUpdate {
        val old = progress(prefs)
        val current = monthId(today)
        val eligible = qualified(victory, wavesLaunched, bossContacts, hazardsResolved) &&
            challengeMonth == current &&
            (old.lastAwardedMonth.isBlank() || current > old.lastAwardedMonth) &&
            old.earnedStamps < MaxStamps
        return MonthStampUpdate(
            month = if (eligible) current else old.lastAwardedMonth,
            awarded = eligible,
            notesAward = if (eligible) NotesAward else 0,
            stampCount = old.earnedStamps + if (eligible) 1 else 0
        )
    }

    fun write(
        editor: SharedPreferences.Editor,
        update: MonthStampUpdate
    ): SharedPreferences.Editor =
        editor.putString(LastMonthKey, update.month)
            .putInt(StampCountKey, update.stampCount)
}
