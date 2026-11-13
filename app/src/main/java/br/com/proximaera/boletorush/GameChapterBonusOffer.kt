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

    /**
     * Bind an earned ad to its original round and chapter rather than the
     * screen that happens to be visible when Google's callback arrives.
     * A player may leave the bonus panel while a video is closing.
     */
    fun mayCreditEarned(
        chapter: Int,
        offeredRoundId: String,
        currentRoundId: String,
        completedMask: Int,
        claimedMask: Int,
        roundSettled: Boolean,
        activeRun: Boolean
    ): Boolean =
        chapter in 0..2 &&
            offeredRoundId.isNotBlank() &&
            offeredRoundId == currentRoundId &&
            !roundSettled &&
            activeRun &&
            GameChapterMissions.completed(chapter, completedMask) &&
            claimedMask and (1 shl chapter) == 0
}
