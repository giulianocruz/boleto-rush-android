package br.com.proximaera.boletorush

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestoneRulesTest {
    @Test fun collectorRequiresFifteenCoinsInOneRound() {
        assertFalse(Milestone.COLLECTOR.reached(1000, 14, true))
        assertTrue(Milestone.COLLECTOR.reached(0, 15, false))
    }

    @Test fun scorerRequiresAtLeastThreeHundredFiftyPoints() {
        assertFalse(Milestone.SCORER.reached(349, 100, true))
        assertTrue(Milestone.SCORER.reached(350, 0, false))
    }

    @Test fun survivorOnlyUnlocksAfterVictory() {
        assertFalse(Milestone.SURVIVOR.reached(9999, 100, false))
        assertTrue(Milestone.SURVIVOR.reached(0, 0, true))
    }

    @Test fun milestoneRewardsHaveUniqueKeysAndPositiveAmounts() {
        assertTrue(Milestone.entries.map { it.key }.distinct().size == Milestone.entries.size)
        assertTrue(Milestone.entries.all { it.reward > 0 })
    }
}
