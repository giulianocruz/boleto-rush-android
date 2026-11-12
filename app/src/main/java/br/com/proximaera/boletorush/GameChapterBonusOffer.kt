package br.com.proximaera.boletorush

/**
 * Optional stage ad gate. A chapter transition must never stop a live run
 * when there is no ad ready, and a goal can only offer one reward per stage.
 */
internal object GameChapterBonusOffer {
    fun shouldOffer(
        chapter: Int, completedMask: Int, offeredMask: Int, adReady: Boolean,
        bossStartingSoon: Boolean = false
    ): Boolean =
        chapter in 0..2 &&
            !bossStartingSoon &&
            adReady &&
            GameChapterMissions.completed(chapter, completedMask) &&
            offeredMask and (1 shl chapter) == 0
}
