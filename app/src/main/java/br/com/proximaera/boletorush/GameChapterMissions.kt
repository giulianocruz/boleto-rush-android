package br.com.proximaera.boletorush

/**
 * Small optional goals for the four themed chapters.
 * Completing a goal only grants virtual banknotes for cosmetics.
 * It never grants score or rare upgrade coins.
 */
internal object GameChapterMissions {
    internal data class Progress(
        val coins: Int,
        val claimedMask: Int,
        val cosmeticNotes: Int
    )

    private val goals = intArrayOf(2, 3, 3, 4)
    private val rewards = intArrayOf(3, 4, 5, 6)
    private val names = arrayOf(
        "FAZER A RESERVA", "GUARDAR O SALÁRIO",
        "FECHAR AS CONTAS", "VENCER A FATURA"
    )

    fun goal(chapter: Int): Int = goals[chapter.coerceIn(0, 3)]

    /**
     * Classic's last chapter starts at 75% (33.75s), after the monthly
     * boss has replaced ordinary spawns (27s onward). Only the safe
     * corridor coins from the second and third waves can arrive in time.
     * Requiring four coins made the fourth Classic mission impossible.
     * Marathon retains its original goal of four coins.
     */
    fun goal(chapter: Int, duration: Float): Int =
        if (chapter == 3 && duration <= 45f) 2 else goal(chapter)
    fun reward(chapter: Int): Int = rewards[chapter.coerceIn(0, 3)]
    fun title(chapter: Int): String = names[chapter.coerceIn(0, 3)]
    fun title(chapter: Int, theme: GameTheme): String =
        if (theme == GameTheme.BRASIL_EM_APUROS)
            arrayOf("FUGIR DAS TAXAS", "VIGIAR A VERBA",
                "SALVAR O ORÇAMENTO", "QUITAR A DÍVIDA")[chapter.coerceIn(0, 3)]
        else title(chapter)

    fun completed(chapter: Int, mask: Int): Boolean =
        chapter in 0..3 && mask and (1 shl chapter) != 0

    fun countCompleted(mask: Int): Int =
        Integer.bitCount(mask and 15)

    fun recordCoin(
        chapter: Int, currentCoins: Int, claimedMask: Int, duration: Float = 90f
    ): Progress {
        if (chapter !in 0..3 || completed(chapter, claimedMask)) {
            return Progress(currentCoins.coerceAtLeast(0), claimedMask, 0)
        }
        val target = goal(chapter, duration)
        val count = (currentCoins.coerceAtLeast(0) + 1)
            .coerceAtMost(target)
        val fulfilled = count >= target
        val nextMask = if (fulfilled) claimedMask or (1 shl chapter) else claimedMask
        return Progress(count, nextMask, if (fulfilled) reward(chapter) else 0)
    }
}
