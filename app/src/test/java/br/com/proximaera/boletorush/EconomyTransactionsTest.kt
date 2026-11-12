package br.com.proximaera.boletorush

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class LocalTestPrefs : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()
    var allowCommit = true

    override fun getAll(): MutableMap<String, *> = data
    override fun contains(key: String?): Boolean = data.containsKey(key)
    override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        data[key] as? MutableSet<String> ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun edit(): SharedPreferences.Editor = Edit()

    private inner class Edit : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()
        private val removed = mutableSetOf<String?>()
        private var clearFirst = false
        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { changes[key.orEmpty()] = value }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply { changes[key.orEmpty()] = values }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { changes[key.orEmpty()] = value }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { changes[key.orEmpty()] = value }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { changes[key.orEmpty()] = value }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { changes[key.orEmpty()] = value }
        override fun remove(key: String?): SharedPreferences.Editor = apply { removed.add(key) }
        override fun clear(): SharedPreferences.Editor = apply { clearFirst = true }
        override fun apply() { commit() }
        override fun commit(): Boolean {
            if (!allowCommit) return false
            if (clearFirst) data.clear()
            removed.forEach { data.remove(it) }
            data.putAll(changes)
            return true
        }
    }
}

class EconomyTransactionsTest {
    @Test fun settlementReceiptDoesNotPromiseDepositsWhenBothWalletsAreFull() {
        val prefs = LocalTestPrefs()
        prefs.edit().putInt("wallet_coins", Int.MAX_VALUE)
            .putInt("wallet_cash", Int.MAX_VALUE).commit()
        val receipt = GameSettlement.settle(
            prefs, "full-wallet-receipt", false, 0, 12, 20, 0, 0, false
        )!!
        assertEquals(0, receipt.coinsBanked)
        assertEquals(0, receipt.notesBanked)
        assertEquals(Int.MAX_VALUE, receipt.wallet)
        assertEquals(Int.MAX_VALUE, receipt.notes)
        assertEquals(null, RewardedRoundBonus.preview(prefs, "full-wallet-receipt"))
        assertEquals(receipt, GameSettlement.latestReceipt(prefs))
    }

    @Test fun settlementReceiptUsesActuallyCreditedPartialRunEarnings() {
        val prefs = LocalTestPrefs()
        prefs.edit().putInt("wallet_coins", Int.MAX_VALUE - 2)
            .putInt("wallet_cash", Int.MAX_VALUE - 3).commit()
        val receipt = GameSettlement.settle(
            prefs, "partial-wallet-receipt", false, 0, 25, 18, 0, 0, false
        )!!
        assertEquals(2, receipt.coinsBanked)
        assertEquals(3, receipt.notesBanked)
        assertEquals(Int.MAX_VALUE, GameEconomy.wallet(prefs))
        assertEquals(Int.MAX_VALUE, GameEconomy.cash(prefs))
        assertTrue(receipt.unlocked.contains(Milestone.COLLECTOR))
        assertEquals(receipt, GameSettlement.settle(
            prefs, "partial-wallet-receipt", false, 0, 999, 999, 0, 0, false
        ))
        assertEquals(1, prefs.getInt("games", 0))
        assertEquals(null, RewardedRoundBonus.preview(prefs, "partial-wallet-receipt"))
    }

    @Test fun weeklyNotesReceiptReflectsRemainingWalletRoom() {
        val prefs = LocalTestPrefs()
        prefs.edit().putInt("wallet_cash", Int.MAX_VALUE - 10).commit()
        val receipt = GameSettlement.settle(
            prefs, "weekly-cap-receipt", false, 0, 0, 4, 0, 0, false,
            relicsCollected = 8, relicBronze = 8
        )!!
        assertEquals(4, receipt.notesBanked)
        assertEquals(6, receipt.weeklyNotesAward)
        assertEquals(Int.MAX_VALUE, receipt.notes)
        assertTrue(GameRelicAlbum.progress(prefs).weeklyClaimed)
        assertEquals(receipt, GameSettlement.latestReceipt(prefs))
        assertEquals(receipt, GameSettlement.settle(
            prefs, "weekly-cap-receipt", false, 0, 0, 40, 0, 0, false
        ))
    }

    @Test fun monthlyStampSurvivesWhenOnlyPartOfNotesPrizeFits() {
        val prefs = LocalTestPrefs()
        prefs.edit().putInt("wallet_cash", Int.MAX_VALUE - 4).commit()
        val receipt = GameSettlement.settle(
            prefs, "monthly-cap-receipt", false, 100, 0, 0, 0, 0, true,
            monthlyBossWaves = 3, monthlyBossContacts = 0,
            monthlyBossResolved = 6,
            challengeMonth = GameMonthChallenge.monthId()
        )!!
        assertTrue(receipt.monthStampAwarded)
        assertEquals(1, receipt.monthStampCount)
        assertEquals(4, receipt.monthNotesAward)
        assertEquals(0, receipt.notesBanked)
        assertEquals(Int.MAX_VALUE, GameEconomy.cash(prefs))
        assertEquals(receipt, GameSettlement.latestReceipt(prefs))
    }

    @Test fun rewardedVideoIsNotOfferedWhenBothWalletsAreCompletelyFull() {
        val prefs = LocalTestPrefs()
        GameSettlement.settle(prefs,"reward-at-cap",false,25,10,20,0,0,false)!!
        prefs.edit().putInt("wallet_coins", Int.MAX_VALUE)
            .putInt("wallet_cash", Int.MAX_VALUE).commit()
        assertEquals(null, RewardedRoundBonus.preview(prefs,"reward-at-cap"))
        assertFalse(RewardedRoundBonus.eligible(prefs,"reward-at-cap"))
        assertEquals(null, RewardedRoundBonus.claimDouble(prefs,"reward-at-cap"))
        assertFalse(RewardedRoundBonus.wasClaimed(prefs,"reward-at-cap"))
        assertEquals(Int.MAX_VALUE, GameEconomy.wallet(prefs))
        assertEquals(Int.MAX_VALUE, GameEconomy.cash(prefs))
    }

    @Test fun rewardedVideoDisplaysAndCreditsOnlyAmountsThatFit() {
        val prefs = LocalTestPrefs()
        val receipt = GameSettlement.settle(
            prefs,"reward-partial",false,15,10,20,0,0,false
        )!!
        assertTrue(receipt.coinsBanked > 1 && receipt.notesBanked > 7)
        prefs.edit().putInt("wallet_coins", Int.MAX_VALUE - 1)
            .putInt("wallet_cash", Int.MAX_VALUE - 7).commit()
        val preview = RewardedRoundBonus.preview(prefs,"reward-partial")!!
        assertEquals(1, preview.coins)
        assertEquals(7, preview.notes)
        assertEquals(Int.MAX_VALUE, preview.wallet)
        assertEquals(Int.MAX_VALUE, preview.cash)
        assertTrue(RewardedRoundBonus.eligible(prefs,"reward-partial"))
        val granted = RewardedRoundBonus.claimDouble(prefs,"reward-partial")!!
        assertEquals(preview, granted)
        assertEquals(Int.MAX_VALUE, GameEconomy.wallet(prefs))
        assertEquals(Int.MAX_VALUE, GameEconomy.cash(prefs))
        assertEquals(null,RewardedRoundBonus.claimDouble(prefs,"reward-partial"))
        assertTrue(RewardedRoundBonus.wasClaimed(prefs,"reward-partial"))
    }

    @Test fun rewardedVideoCanOfferNotesOnlyWhenCoinWalletIsFull() {
        val prefs = LocalTestPrefs()
        GameSettlement.settle(prefs,"reward-only-notes",false,10,10,20,0,0,false)!!
        prefs.edit().putInt("wallet_coins", Int.MAX_VALUE)
            .putInt("wallet_cash", Int.MAX_VALUE - 2).commit()
        val preview = RewardedRoundBonus.preview(prefs,"reward-only-notes")!!
        assertEquals(0, preview.coins)
        assertEquals(2, preview.notes)
        val granted = RewardedRoundBonus.claimDouble(prefs,"reward-only-notes")!!
        assertEquals(preview, granted)
        assertEquals(Int.MAX_VALUE, GameEconomy.wallet(prefs))
        assertEquals(Int.MAX_VALUE, GameEconomy.cash(prefs))
    }

    @Test fun rewardedVideoRechecksWalletLimitsAtEarnedCallback() {
        val prefs = LocalTestPrefs()
        GameSettlement.settle(prefs,"reward-cap-changed",false,10,10,20,0,0,false)!!
        val original = RewardedRoundBonus.preview(prefs,"reward-cap-changed")!!
        assertTrue(original.coins > 0 && original.notes > 0)
        prefs.edit().putInt("wallet_coins", Int.MAX_VALUE - 1)
            .putInt("wallet_cash", Int.MAX_VALUE - 1).commit()
        val granted = RewardedRoundBonus.claimDouble(prefs,"reward-cap-changed")!!
        assertEquals(1, granted.coins)
        assertEquals(1, granted.notes)
        assertEquals(Int.MAX_VALUE, GameEconomy.wallet(prefs))
        assertEquals(Int.MAX_VALUE, GameEconomy.cash(prefs))
        assertFalse(RewardedRoundBonus.eligible(prefs,"reward-cap-changed"))
    }

    @Test fun notesTradeNeverConsumesRelicsIfFullRewardCannotFit() {
        val prefs = LocalTestPrefs()
        prefs.edit()
            .putInt(GameRelics.WalletKey, 20)
            .putInt("wallet_cash", Int.MAX_VALUE - 17)
            .commit()
        assertFalse(GameRelics.tradeHasCapacity(prefs, RelicDeal.NOTES))
        assertFalse(GameRelics.exchange(prefs, RelicDeal.NOTES))
        assertEquals(20, GameRelics.balance(prefs))
        assertEquals(Int.MAX_VALUE - 17, GameEconomy.cash(prefs))
        prefs.edit().putInt("wallet_cash", Int.MAX_VALUE - 18).commit()
        assertTrue(GameRelics.tradeHasCapacity(prefs, RelicDeal.NOTES))
        assertTrue(GameRelics.exchange(prefs, RelicDeal.NOTES))
        assertEquals(18, GameRelics.balance(prefs))
        assertEquals(Int.MAX_VALUE, GameEconomy.cash(prefs))
        assertFalse(GameRelics.exchange(prefs, RelicDeal.NOTES))
        assertEquals(18, GameRelics.balance(prefs))
    }

    @Test fun coinsTradeNeverConsumesRelicsIfFullFiveCoinRewardCannotFit() {
        val prefs = LocalTestPrefs()
        prefs.edit()
            .putInt(GameRelics.WalletKey, 20)
            .putInt("wallet_coins", Int.MAX_VALUE - 3)
            .commit()
        assertFalse(GameRelics.tradeHasCapacity(prefs, RelicDeal.COINS))
        assertFalse(GameRelics.exchange(prefs, RelicDeal.COINS))
        assertEquals(20, GameRelics.balance(prefs))
        assertEquals(Int.MAX_VALUE - 3, GameEconomy.wallet(prefs))

        prefs.edit().putInt("wallet_coins", Int.MAX_VALUE - 5).commit()
        assertTrue(GameRelics.exchange(prefs, RelicDeal.COINS))
        assertEquals(17, GameRelics.balance(prefs))
        assertEquals(Int.MAX_VALUE, GameEconomy.wallet(prefs))
    }

    @Test fun coinTradeCanRepayDebtWithoutWalletRoomButNeverLosesPartialReward() {
        val prefs = LocalTestPrefs()
        prefs.edit()
            .putInt(GameRelics.WalletKey, 20)
            .putInt("wallet_coins", Int.MAX_VALUE)
            .putInt(GameEconomy.DebtKey, 5)
            .commit()
        assertTrue(GameRelics.tradeHasCapacity(prefs, RelicDeal.COINS))
        assertTrue(GameRelics.exchange(prefs, RelicDeal.COINS))
        assertEquals(17, GameRelics.balance(prefs))
        assertEquals(0, GameEconomy.debt(prefs))
        assertEquals(Int.MAX_VALUE, GameEconomy.wallet(prefs))

        prefs.edit().putInt(GameEconomy.DebtKey, 3).commit()
        assertFalse(GameRelics.tradeHasCapacity(prefs, RelicDeal.COINS))
        assertFalse(GameRelics.exchange(prefs, RelicDeal.COINS))
        assertEquals(17, GameRelics.balance(prefs))
        assertEquals(3, GameEconomy.debt(prefs))

        prefs.edit().putInt("wallet_coins", Int.MAX_VALUE - 2).commit()
        assertTrue(GameRelics.exchange(prefs, RelicDeal.COINS))
        assertEquals(14, GameRelics.balance(prefs))
        assertEquals(0, GameEconomy.debt(prefs))
        assertEquals(Int.MAX_VALUE, GameEconomy.wallet(prefs))
    }

    @Test fun failedTradeCommitDoesNotDebitRelicsOrMintPartialWalletReward() {
        val prefs = LocalTestPrefs()
        prefs.edit().putInt(GameRelics.WalletKey, 12).commit()
        prefs.allowCommit = false
        assertFalse(GameRelics.exchange(prefs, RelicDeal.NOTES))
        assertEquals(12, GameRelics.balance(prefs))
        assertEquals(0, GameEconomy.cash(prefs))
        prefs.allowCommit = true
        assertTrue(GameRelics.exchange(prefs, RelicDeal.NOTES))
        assertEquals(10, GameRelics.balance(prefs))
        assertEquals(18, GameEconomy.cash(prefs))
    }

    @Test fun relicShieldAlwaysAddsThreeSecondsToEveryPermanentUpgradeLevel() {
        for (level in 0..Upgrade.START_SHIELD.maxLevel) {
            val prefs = LocalTestPrefs()
            prefs.edit().putInt(GameRelics.WalletKey, 5).commit()
            assertTrue(GameRelics.exchange(prefs, RelicDeal.START_SHIELD))
            assertFalse(GameRelics.exchange(prefs, RelicDeal.START_SHIELD))
            assertEquals(0, GameRelics.balance(prefs))
            val base = GameEconomy.initialShieldSeconds(level)
            val bought = GameRelics.consumeStartShield(prefs)
            assertEquals(3f, bought)
            assertEquals(base + 3f,
                GameRelics.totalStartShieldSeconds(base, bought))
            assertEquals(0f, GameRelics.consumeStartShield(prefs))
            assertEquals(base,
                GameRelics.totalStartShieldSeconds(base, 0f))
        }
    }

    @Test fun relicShieldClampsInvalidOrCorruptedInputToFairProtectionLimits() {
        assertEquals(9f, GameRelics.totalStartShieldSeconds(25f, 20f))
        assertEquals(0f, GameRelics.totalStartShieldSeconds(-10f, -3f))
        assertEquals(3f, GameRelics.totalStartShieldSeconds(Float.NaN, 3f))
        assertEquals(6f, GameRelics.totalStartShieldSeconds(6f, Float.POSITIVE_INFINITY))
        assertEquals(5f, GameRelics.totalStartShieldSeconds(2f, 3f))
    }

    @Test fun relicShieldBonusSurvivesFailedConsumeAndCannotBeUsedTwice() {
        val prefs = LocalTestPrefs()
        prefs.edit().putInt(GameRelics.WalletKey, 8).commit()
        assertTrue(GameRelics.exchange(prefs, RelicDeal.START_SHIELD))
        prefs.allowCommit = false
        assertEquals(0f, GameRelics.consumeStartShield(prefs))
        assertTrue(GameRelics.pendingShield(prefs))
        prefs.allowCommit = true
        val bonus = GameRelics.consumeStartShield(prefs)
        assertEquals(3f, bonus)
        assertEquals(9f,
            GameRelics.totalStartShieldSeconds(
                GameEconomy.initialShieldSeconds(3), bonus))
        assertFalse(GameRelics.pendingShield(prefs))
        assertEquals(0f, GameRelics.consumeStartShield(prefs))
        assertEquals(3, GameRelics.balance(prefs))
    }

    @Test fun aFullRelicWalletStillRecordsDiscoveriesAndWeeklyProgress() {
        val prefs = LocalTestPrefs()
        prefs.edit().putInt(GameRelics.WalletKey, GameRelics.MaxWallet - 1).commit()
        val first = GameSettlement.settle(
            prefs, "relic-cap-bronze", false, 15, 0, 0, 0, 0, false,
            relicsCollected = 5, relicBronze = 5
        )!!
        assertEquals(1, first.relicsBanked)
        assertEquals(GameRelics.MaxWallet, first.relicWallet)
        assertEquals(5, GameRelicAlbum.progress(prefs).weeklyFragments)
        assertEquals(5, GameRelicAlbum.progress(prefs).bronze)
        assertEquals(0, first.weeklyNotesAward)

        val second = GameSettlement.settle(
            prefs, "relic-cap-silver", false, 30, 0, 0, 0, 0, false,
            relicsCollected = 3, relicSilver = 3
        )!!
        assertEquals(0, second.relicsBanked)
        assertEquals(GameRelics.MaxWallet, second.relicWallet)
        assertEquals(8, GameRelicAlbum.progress(prefs).weeklyFragments)
        assertEquals(3, GameRelicAlbum.progress(prefs).silver)
        assertEquals(16, second.weeklyNotesAward)
        assertEquals(16, GameEconomy.cash(prefs))
        assertEquals(second, GameSettlement.settle(
            prefs, "relic-cap-silver", false, 999, 99, 99, 0, 0, true,
            relicsCollected = 12, relicGold = 12
        ))
        assertEquals(16, GameEconomy.cash(prefs))

        val final = GameSettlement.settle(
            prefs, "relic-cap-gold", false, 35, 0, 0, 0, 0, false,
            relicsCollected = 1, relicGold = 1
        )!!
        assertEquals(0, final.relicsBanked)
        assertEquals(0, final.weeklyNotesAward)
        assertTrue(final.albumUnlocked)
        assertEquals(1, GameRelicAlbum.progress(prefs).gold)
        assertEquals(GameRelics.MaxWallet, GameRelics.balance(prefs))
        assertTrue(GameEconomy.ownsCosmetic(prefs, Cosmetic.RELIC_HUNTER))
    }

    @Test fun failedSaveAtRelicCapacityDoesNotPartiallyCreditCollection() {
        val prefs = LocalTestPrefs()
        prefs.edit().putInt(GameRelics.WalletKey, GameRelics.MaxWallet).commit()
        prefs.allowCommit = false
        assertEquals(null, GameSettlement.settle(
            prefs, "relic-full-retry", false, 12, 0, 0, 0, 0, false,
            relicsCollected = 8, relicBronze = 8
        ))
        assertEquals(0, GameRelicAlbum.progress(prefs).bronze)
        assertEquals(0, GameRelicAlbum.progress(prefs).weeklyFragments)
        assertEquals(0, GameEconomy.cash(prefs))
        assertEquals(GameRelics.MaxWallet, GameRelics.balance(prefs))

        prefs.allowCommit = true
        val settled = GameSettlement.settle(
            prefs, "relic-full-retry", false, 12, 0, 0, 0, 0, false,
            relicsCollected = 8, relicBronze = 8
        )!!
        assertEquals(0, settled.relicsBanked)
        assertEquals(16, settled.weeklyNotesAward)
        assertEquals(8, GameRelicAlbum.progress(prefs).weeklyFragments)
        assertEquals(8, GameRelicAlbum.progress(prefs).bronze)
        assertEquals(16, GameEconomy.cash(prefs))
        assertFalse(GameRelics.rewardedEligible(prefs, "relic-full-retry"))
        assertEquals(settled, GameSettlement.latestReceipt(prefs))
        assertEquals(settled, GameSettlement.settle(
            prefs, "relic-full-retry", false, 100, 100, 100, 0, 0, true,
            relicsCollected = 12
        ))
        assertEquals(16, GameEconomy.cash(prefs))
    }

    @Test fun bossWithUnresolvedLastHazardNeverAwardsStampOrNotes() {
        val prefs=LocalTestPrefs()
        val month=GameMonthChallenge.monthId()
        val missed=GameSettlement.settle(prefs,"boss-not-resolved",false,
            300,0,0,0,0,true,monthlyBossWaves=3,
            monthlyBossContacts=0,monthlyBossResolved=5,challengeMonth=month)!!
        assertFalse(missed.monthStampAwarded)
        assertEquals(0,missed.monthNotesAward)
        assertEquals(0,GameMonthChallenge.progress(prefs).earnedStamps)
        val completed=GameSettlement.settle(prefs,"boss-resolved",false,
            300,0,0,0,0,true,monthlyBossWaves=3,
            monthlyBossContacts=0,monthlyBossResolved=6,challengeMonth=month)!!
        assertTrue(completed.monthStampAwarded)
        assertEquals(25,completed.monthNotesAward)
        assertEquals(25,GameEconomy.cash(prefs))
        assertEquals(1,GameMonthChallenge.progress(prefs).earnedStamps)
    }

    @Test fun monthlyBossRewardIsAtomicAndCanOnlyBeClaimedOncePerMonth() {
        val prefs = LocalTestPrefs()
        val month = GameMonthChallenge.monthId()
        val first = GameSettlement.settle(prefs,"boss-month-first",false,
            100,0,0,0,0,true,
            monthlyBossWaves=3,monthlyBossContacts=0,monthlyBossResolved=6,challengeMonth=month)!!
        assertTrue(first.monthStampAwarded)
        assertEquals(25,first.monthNotesAward)
        assertEquals(1,first.monthStampCount)
        assertEquals(25,GameEconomy.cash(prefs))
        assertEquals(first,GameSettlement.latestReceipt(prefs))
        assertEquals(first,GameSettlement.settle(prefs,"boss-month-first",false,
            999,999,999,0,0,true,
            monthlyBossWaves=3,monthlyBossContacts=0,monthlyBossResolved=6,challengeMonth=month))
        assertEquals(25,GameEconomy.cash(prefs))
        val replay = GameSettlement.settle(prefs,"boss-month-second",false,
            20,0,0,0,0,true,
            monthlyBossWaves=3,monthlyBossContacts=0,monthlyBossResolved=6,challengeMonth=month)!!
        assertFalse(replay.monthStampAwarded)
        assertEquals(0,replay.monthNotesAward)
        assertEquals(1,replay.monthStampCount)
        assertEquals(25,GameEconomy.cash(prefs))
    }

    @Test fun monthlyStampNeverPaysForHitWithdrawOrIncompleteWaves() {
        val month=GameMonthChallenge.monthId()
        val prefs=LocalTestPrefs()
        val impact=GameSettlement.settle(prefs,"boss-impact",false,
            0,0,0,0,0,true,
            monthlyBossWaves=3,monthlyBossContacts=1,monthlyBossResolved=6,challengeMonth=month)!!
        val early=GameSettlement.settle(prefs,"boss-incomplete",false,
            0,0,0,0,0,true,
            monthlyBossWaves=2,monthlyBossContacts=0,monthlyBossResolved=4,challengeMonth=month)!!
        val defeat=GameSettlement.settle(prefs,"boss-defeat",false,
            0,0,0,0,0,false,
            monthlyBossWaves=3,monthlyBossContacts=0,monthlyBossResolved=6,challengeMonth=month)!!
        for(receipt in listOf(impact,early,defeat)) {
            assertFalse(receipt.monthStampAwarded)
            assertEquals(0,receipt.monthNotesAward)
        }
        assertEquals(0,GameMonthChallenge.progress(prefs).earnedStamps)
    }

    @Test fun monthlyStampFailedCommitDoesNotPartiallyAwardNotesOrTrophy() {
        val prefs=LocalTestPrefs()
        val month=GameMonthChallenge.monthId()
        prefs.allowCommit=false
        assertEquals(null,GameSettlement.settle(prefs,"boss-fail",false,
            0,0,0,0,0,true,
            monthlyBossWaves=3,monthlyBossContacts=0,monthlyBossResolved=6,challengeMonth=month))
        assertEquals(0,GameMonthChallenge.progress(prefs).earnedStamps)
        assertEquals(0,GameEconomy.cash(prefs))
        prefs.allowCommit=true
        val retry=GameSettlement.settle(prefs,"boss-fail",false,
            0,0,0,0,0,true,
            monthlyBossWaves=3,monthlyBossContacts=0,monthlyBossResolved=6,challengeMonth=month)!!
        assertTrue(retry.monthStampAwarded)
        assertEquals(1,GameMonthChallenge.progress(prefs).earnedStamps)
        assertEquals(25,GameEconomy.cash(prefs))
    }

    @Test fun monthlyProgressRejectsOldClocksAndAllowsNewCalendarMonths() {
        val prefs=LocalTestPrefs()
        val nov=GameMonthChallenge.plan(prefs,true,3,0,"2026-11",
            java.time.LocalDate.of(2026,11,12),hazardsResolved=6)
        assertTrue(nov.awarded)
        assertTrue(GameMonthChallenge.write(prefs.edit(),nov).commit())
        assertEquals(1,GameMonthChallenge.progress(prefs).earnedStamps)
        val same=GameMonthChallenge.plan(prefs,true,3,0,"2026-11",
            java.time.LocalDate.of(2026,11,30),hazardsResolved=6)
        assertFalse(same.awarded)
        val dec=GameMonthChallenge.plan(prefs,true,3,0,"2026-12",
            java.time.LocalDate.of(2026,12,1),hazardsResolved=6)
        assertTrue(dec.awarded)
        assertTrue(GameMonthChallenge.write(prefs.edit(),dec).commit())
        val rollback=GameMonthChallenge.plan(prefs,true,3,0,"2026-11",
            java.time.LocalDate.of(2026,11,12),hazardsResolved=6)
        assertFalse(rollback.awarded)
        assertEquals(2,rollback.stampCount)
        val mismatch=GameMonthChallenge.plan(prefs,true,3,0,"2027-01",
            java.time.LocalDate.of(2027,2,1),hazardsResolved=6)
        assertFalse(mismatch.awarded)
    }

    @Test fun oldVersionThreeAlbumReceiptStillDecodesAfterMonthlyMigration() {
        val prefs=LocalTestPrefs()
        prefs.edit().putString("last_settlement_receipt_v2",
            "3|1|25|1|2|0|0|1|2|0|0|-|3|3|16|1").commit()
        val receipt=GameSettlement.latestReceipt(prefs)!!
        assertEquals(3,receipt.relicsBanked)
        assertEquals(16,receipt.weeklyNotesAward)
        assertTrue(receipt.albumUnlocked)
        assertFalse(receipt.monthStampAwarded)
        assertEquals(0,receipt.monthStampCount)
        assertEquals(0,receipt.monthNotesAward)
    }

    @Test fun albumTracksLifetimeRaritiesAndUnlocksExclusiveSkin() {
        val prefs = LocalTestPrefs()
        assertFalse(GameEconomy.ownsCosmetic(prefs, Cosmetic.RELIC_HUNTER))
        assertFalse(GameEconomy.buyOrEquipCosmetic(prefs, Cosmetic.RELIC_HUNTER))
        assertEquals(0, GameEconomy.cash(prefs))
        val bronze = GameSettlement.settle(prefs, "album-bronze", false,
            12, 0, 0, 0, 0, false, relicsCollected = 5, relicBronze = 5)!!
        assertEquals(5, bronze.relicsBanked)
        assertFalse(bronze.albumUnlocked)
        assertFalse(GameRelicAlbum.complete(GameRelicAlbum.progress(prefs)))
        val final = GameSettlement.settle(prefs, "album-complete", false,
            20, 0, 0, 0, 0, false, relicsCollected = 9, relicSilver = 3, relicGold = 1)!!
        assertEquals(9, final.relicsBanked)
        assertTrue(final.albumUnlocked)
        assertEquals(16, final.weeklyNotesAward)
        assertEquals(16, GameEconomy.cash(prefs))
        val album = GameRelicAlbum.progress(prefs)
        assertEquals(5, album.bronze)
        assertEquals(3, album.silver)
        assertEquals(1, album.gold)
        assertTrue(GameRelicAlbum.complete(album))
        assertTrue(GameEconomy.ownsCosmetic(prefs, Cosmetic.RELIC_HUNTER))
        assertTrue(GameEconomy.buyOrEquipCosmetic(prefs, Cosmetic.RELIC_HUNTER))
        assertEquals(Cosmetic.RELIC_HUNTER, GameEconomy.selectedCosmetic(prefs))
        assertEquals(16, GameEconomy.cash(prefs)) // Cosmetic is free only after earning it.
        assertEquals(final, GameSettlement.settle(prefs,"album-complete",false,
            999, 999, 999, 0, 0, true, relicsCollected=12))
        assertEquals(16, GameEconomy.cash(prefs))
    }

    @Test fun weeklyMissionOnlyPaysOnceAndDoesNotGetDuplicatedByAdReward() {
        val prefs = LocalTestPrefs()
        val first = GameSettlement.settle(prefs,"weekly-1",false,
            0,0,0,0,0,false,relicsCollected=8,relicBronze=8)!!
        assertEquals(16,first.weeklyNotesAward)
        assertEquals(0,first.notesBanked)
        assertEquals(16,GameEconomy.cash(prefs))
        assertTrue(GameRelicAlbum.progress(prefs).weeklyClaimed)
        val second = GameSettlement.settle(prefs,"weekly-2",false,
            0,0,0,0,0,false,relicsCollected=8,relicBronze=8)!!
        assertEquals(0,second.weeklyNotesAward)
        assertEquals(16,GameEconomy.cash(prefs))
        assertEquals(8,GameRelicAlbum.progress(prefs).weeklyFragments)
        assertFalse(RewardedRoundBonus.eligible(prefs, "weekly-2"))
    }

    @Test fun albumAndWeeklyProgressRollbackTogetherOnFailedCommit() {
        val prefs = LocalTestPrefs()
        prefs.allowCommit = false
        assertEquals(null,GameSettlement.settle(prefs,"album-storage-fail",false,
            0,0,0,0,0,false,relicsCollected=8,relicBronze=8))
        assertEquals(0,GameRelicAlbum.progress(prefs).bronze)
        assertEquals(0,GameRelicAlbum.progress(prefs).weeklyFragments)
        assertEquals(0,GameEconomy.cash(prefs))
        prefs.allowCommit = true
        val receipt = GameSettlement.settle(prefs,"album-storage-fail",false,
            0,0,0,0,0,false,relicsCollected=8,relicBronze=8)!!
        assertEquals(16,receipt.weeklyNotesAward)
        assertEquals(8,GameRelicAlbum.progress(prefs).bronze)
    }

    @Test fun isoWeeksHandleYearCrossingAndBlockClockRollbackFarming() {
        assertEquals("2020-W53", GameRelicAlbum.weekId(java.time.LocalDate.of(2021,1,1)))
        assertEquals("2021-W01", GameRelicAlbum.weekId(java.time.LocalDate.of(2021,1,4)))
        val prefs = LocalTestPrefs()
        val old = GameRelicAlbum.planSettlement(prefs,1,0,0,8,
            java.time.LocalDate.of(2020,12,31))
        assertEquals(16,old.weeklyNotesAward)
        assertTrue(GameRelicAlbum.write(prefs.edit(),old).commit())
        val sameWeek = GameRelicAlbum.planSettlement(prefs,0,0,0,8,
            java.time.LocalDate.of(2021,1,1))
        assertEquals(0,sameWeek.weeklyNotesAward)
        val future = GameRelicAlbum.planSettlement(prefs,0,0,0,8,
            java.time.LocalDate.of(2021,1,4))
        assertEquals(16,future.weeklyNotesAward)
        assertTrue(GameRelicAlbum.write(prefs.edit(),future).commit())
        val rollback = GameRelicAlbum.planSettlement(prefs,0,0,0,8,
            java.time.LocalDate.of(2020,12,31))
        assertEquals("2021-W01",rollback.progress.week)
        assertEquals(0,rollback.weeklyNotesAward)
    }

    @Test fun albumCountersAreBoundedWithoutCorruptingWallets() {
        val prefs = LocalTestPrefs()
        val before = GameRelicAlbum.planSettlement(prefs,0,0,0,0)
        assertEquals(0,before.weeklyNotesAward)
        assertTrue(GameRelicAlbum.write(prefs.edit(),before).commit())
        assertEquals(0,GameRelics.balance(prefs))
        val updated = GameRelicAlbum.planSettlement(prefs,12,12,12,12)
        assertTrue(GameRelicAlbum.write(prefs.edit(),updated).commit())
        assertEquals(12,GameRelicAlbum.progress(prefs).bronze)
        assertEquals(12,GameRelicAlbum.progress(prefs).silver)
        assertEquals(12,GameRelicAlbum.progress(prefs).gold)
        assertEquals(0,GameRelics.balance(prefs))
        assertTrue(GameRelicAlbum.complete(GameRelicAlbum.progress(prefs)))
    }

    @Test fun versionTwoRelicReceiptStillDecodesWithoutWeeklyFields() {
        val prefs = LocalTestPrefs()
        prefs.edit().putString("last_settlement_receipt_v2",
            "2|1|25|1|2|0|0|1|2|0|0|-|3|3").commit()
        val value = GameSettlement.latestReceipt(prefs)!!
        assertEquals(3,value.relicsBanked)
        assertEquals(3,value.relicWallet)
        assertEquals(0,value.weeklyNotesAward)
        assertFalse(value.albumUnlocked)
    }

    @Test fun finalSettlementIsAtomicAndIdempotent() {
        val prefs = LocalTestPrefs()
        val receipt = GameSettlement.settle(prefs, "round-a", false,
            370, 15, 12, 20, 0, false)!!
        assertEquals(17, receipt.wallet) // 3 earned + 6 collector + 8 scorer
        assertEquals(12, receipt.notes)
        assertEquals(20, receipt.credit)
        assertEquals(1, receipt.gamesPlayed)
        assertEquals(370, receipt.best)
        assertEquals(2, receipt.unlocked.size)
        assertFalse(GameMilestones.claimed(prefs, Milestone.SURVIVOR))
        assertEquals(receipt, GameSettlement.settle(prefs, "round-a", false,
            370, 15, 12, 20, 0, false))
        assertEquals(17, GameEconomy.wallet(prefs))
        assertEquals(1, prefs.getInt("games", 0))
        assertEquals(20, GameEconomy.credit(prefs))
    }

    @Test fun failedStorageTransactionCanBeRetriedWithoutPartialRewards() {
        val prefs = LocalTestPrefs()
        prefs.allowCommit = false
        assertEquals(null, GameSettlement.settle(prefs, "failed-round", true,
            40, 6, 24, 10, 0, false))
        assertEquals(0, GameEconomy.cash(prefs))
        assertEquals(0, GameEconomy.wallet(prefs))
        assertEquals(0, GameEconomy.credit(prefs))
        assertEquals(0, prefs.getInt("games", 0))
        prefs.allowCommit = true
        val receipt = GameSettlement.settle(prefs, "failed-round", true,
            40, 6, 24, 10, 0, false)!!
        assertEquals(1, receipt.coinsBanked)
        assertEquals(24, receipt.notesBanked)
        assertEquals(10, receipt.creditGranted)
        assertEquals(1, receipt.gamesPlayed)
        assertEquals(40, prefs.getInt("best_marathon", 0))
    }

    @Test fun debtAchievementsAndCardsSettleInOneTransaction() {
        val prefs = LocalTestPrefs()
        GameEconomy.addCredit(prefs, 20)
        assertTrue(GameEconomy.buy(prefs, Upgrade.MAGNET))
        assertEquals(18, GameEconomy.debt(prefs))
        val receipt = GameSettlement.settle(prefs, "credit-repay", false,
            350, 15, 9, 5, 0, true)!!
        assertEquals(18, receipt.debtRepaid)
        assertEquals(0, receipt.debt)
        assertEquals(11, receipt.wallet)
        assertEquals(9, receipt.notes)
        assertEquals(7, receipt.credit)
        assertEquals(5, receipt.creditGranted)
        assertEquals(3, receipt.unlocked.size)
        assertEquals(receipt, GameSettlement.settle(prefs, "credit-repay", false,
            350, 15, 9, 5, 0, true))
        assertEquals(11, GameEconomy.wallet(prefs))
    }


    @Test fun lastRoundReceiptCanBeReplayedWithoutPayingTwice() {
        val prefs = LocalTestPrefs()
        val first = GameSettlement.settle(prefs, "receipt-01", false,
            430, 22, 30, 15, 0, true)!!
        val again = GameSettlement.settle(prefs, "receipt-01", false,
            9999, 9999, 9999, 25, 3, true)
        assertEquals(first, again)
        assertEquals(first, GameSettlement.latestReceipt(prefs))
        assertEquals(first.wallet, GameEconomy.wallet(prefs))
        assertEquals(1, prefs.getInt("games", 0))
        assertEquals(30, GameEconomy.cash(prefs))
    }

    @Test fun olderRoundCannotBePaidAfterAnotherRoundFinishes() {
        val prefs = LocalTestPrefs()
        GameSettlement.settle(prefs, "first", false, 10, 8, 3, 0, 0, false)!!
        val second = GameSettlement.settle(prefs, "second", true, 20, 10, 6, 0, 0, false)!!
        val priorWallet = GameEconomy.wallet(prefs)
        val priorCash = GameEconomy.cash(prefs)
        assertEquals(null, GameSettlement.settle(prefs, "first", false,
            99999, 1000, 1000, 25, 3, true))
        assertEquals(2, prefs.getInt("games", 0))
        assertEquals(priorWallet, GameEconomy.wallet(prefs))
        assertEquals(priorCash, GameEconomy.cash(prefs))
        assertEquals(second, GameSettlement.settle(prefs, "second", true,
            20, 10, 6, 0, 0, false))
    }

    @Test fun legacyLastIdIsRetainedOnMigration() {
        val prefs = LocalTestPrefs()
        prefs.edit().putString("last_settlement_id", "legacy-id").commit()
        assertEquals(null, GameSettlement.settle(prefs, "legacy-id", false,
            10, 5, 0, 0, 0, false))
        GameSettlement.settle(prefs, "new-id", false, 10, 5, 0, 0, 0, false)!!
        assertEquals(null, GameSettlement.settle(prefs, "legacy-id", false,
            10, 5, 0, 0, 0, false))
        assertEquals(1, prefs.getInt("games", 0))
    }

    @Test fun historyRemainsBoundedWhileRejectingRecentSettlements() {
        val prefs = LocalTestPrefs()
        for (i in 0..135) {
            GameSettlement.settle(prefs, "round-$i", false, 0, 0, 0, 0, 0, false)!!
        }
        val ledger = prefs.getString("settled_round_ids_v2", "").orEmpty()
        assertEquals(128, ledger.split("\n").size)
        assertEquals(null, GameSettlement.settle(prefs, "round-50", false,
            0, 99, 99, 10, 0, false))
        assertEquals(136, prefs.getInt("games", 0))
    }

    @Test fun collectedCardsStayPendingUntilMatchSettles() {
        val prefs = LocalTestPrefs()
        var pending = 0
        pending += GameEconomy.reserveCardCredit(prefs, pending)
        pending += GameEconomy.reserveCardCredit(prefs, pending)
        pending += GameEconomy.reserveCardCredit(prefs, pending)
        assertEquals(25, pending)
        assertEquals(0, GameEconomy.credit(prefs))
        // Abandoning before cash-out does not invoke addCredit and leaves no persistent credit.
        assertEquals(0, GameEconomy.debt(prefs))
        assertEquals(25, GameEconomy.addCredit(prefs, pending))
        assertEquals(25, GameEconomy.credit(prefs))
        assertEquals(0, GameEconomy.reserveCardCredit(prefs, 0))
        assertEquals(0, GameEconomy.addCredit(prefs, 0))
    }

    @Test fun pendingCardCannotBypassOutstandingCreditDebtCap() {
        val prefs = LocalTestPrefs()
        GameEconomy.addCredit(prefs, 20)
        assertTrue(GameEconomy.buy(prefs, Upgrade.MAGNET))
        assertEquals(18, GameEconomy.debt(prefs))
        assertEquals(2, GameEconomy.credit(prefs))
        var pending = GameEconomy.reserveCardCredit(prefs, 0, 10)
        assertEquals(5, pending)
        assertEquals(0, GameEconomy.reserveCardCredit(prefs, pending, 10))
        assertEquals(2, GameEconomy.credit(prefs)) // all in-round rewards still pending
        assertEquals(5, GameEconomy.addCredit(prefs, pending))
        assertEquals(7, GameEconomy.credit(prefs))
        assertEquals(18, GameEconomy.debt(prefs))
    }

    @Test fun cardAllowsOnePurchaseAndDebtIsPaidFromFutureEarnings() {
        val prefs = LocalTestPrefs()
        assertEquals(10, GameEconomy.addCredit(prefs, 10))
        assertEquals(10, GameEconomy.addCredit(prefs, 10))
        assertTrue(GameEconomy.buy(prefs, Upgrade.MAGNET))
        assertEquals(18, GameEconomy.debt(prefs))
        assertEquals(2, GameEconomy.credit(prefs))
        assertFalse(GameEconomy.buy(prefs, Upgrade.START_SHIELD))
        assertEquals(0, GameEconomy.creditRound(prefs, 20, 0))
        assertEquals(14, GameEconomy.debt(prefs))
        assertEquals(1, GameEconomy.creditRound(prefs, 75, 0))
        assertEquals(0, GameEconomy.debt(prefs))
        assertEquals(1, GameEconomy.wallet(prefs))
    }

    @Test fun cosmeticsArePaidOnlyWithNotes() {
        val prefs = LocalTestPrefs()
        GameEconomy.creditCash(prefs, 24)
        assertFalse(GameEconomy.buyOrEquipCosmetic(prefs, Cosmetic.NEON))
        GameEconomy.creditCash(prefs, 3)
        assertTrue(GameEconomy.buyOrEquipCosmetic(prefs, Cosmetic.NEON))
        assertEquals(2, GameEconomy.cash(prefs))
        assertEquals(0, GameEconomy.wallet(prefs))
        assertTrue(GameEconomy.buyOrEquipCosmetic(prefs, Cosmetic.NEON))
        assertEquals(Cosmetic.NEON, GameEconomy.selectedCosmetic(prefs))
        assertEquals(2, GameEconomy.cash(prefs))
    }

    @Test fun voluntaryCashoutBanksNotesAndCoinsButNotVictoryMilestone() {
        val prefs = LocalTestPrefs()
        // An eligible voluntary exit uses the normal settlement path with victory=false.
        assertTrue(GameRoundExit.canCashOut(23f,45f))
        assertEquals(3,GameEconomy.creditRound(prefs,
            GameRoundExit.eligibleCoins(15),0))
        assertEquals(12,GameEconomy.creditCash(prefs,
            GameRoundExit.eligibleCash(12)))
        val award = GameMilestones.awardRound(prefs,180,15,false)
        assertEquals(listOf(Milestone.COLLECTOR),award.unlocked)
        assertEquals(9,GameEconomy.wallet(prefs))
        assertEquals(12,GameEconomy.cash(prefs))
        assertFalse(GameMilestones.claimed(prefs,Milestone.SURVIVOR))
        // Later winning another round must still unlock the survivor milestone.
        assertEquals(listOf(Milestone.SURVIVOR),
            GameMilestones.awardRound(prefs,180,0,true).unlocked)
        assertEquals(21,GameEconomy.wallet(prefs))
    }

    @Test fun milestonesRepayDebtAndNeverPayTwice() {
        val prefs = LocalTestPrefs()
        GameEconomy.addCredit(prefs, 20)
        assertTrue(GameEconomy.buy(prefs, Upgrade.MAGNET))
        val award = GameMilestones.awardRound(prefs, 350, 15, true)
        assertEquals(26, award.coins)
        assertEquals(0, GameEconomy.debt(prefs))
        assertEquals(8, GameEconomy.wallet(prefs))
        assertTrue(GameMilestones.awardRound(prefs,350,15,true).unlocked.isEmpty())
        assertEquals(8, GameEconomy.wallet(prefs))
    }

    @Test fun rewardedDoubleCreditsExactlyOnceAndPreservesDebtAndRecord() {
        val prefs = LocalTestPrefs()
        val receipt = GameSettlement.settle(prefs,"rewarded-01",false,
            70,10,24,0,0,false)!!
        assertTrue(RewardedRoundBonus.eligible(prefs,"rewarded-01"))
        val bonus = RewardedRoundBonus.claimDouble(prefs,"rewarded-01")!!
        assertEquals(receipt.coinsBanked,bonus.coins)
        assertEquals(24,bonus.notes)
        assertEquals(receipt.wallet + bonus.coins, GameEconomy.wallet(prefs))
        assertEquals(48, GameEconomy.cash(prefs))
        assertTrue(RewardedRoundBonus.wasClaimed(prefs,"rewarded-01"))
        assertEquals(null,RewardedRoundBonus.claimDouble(prefs,"rewarded-01"))
        assertEquals(bonus.wallet,GameEconomy.wallet(prefs))
        assertEquals(70,prefs.getInt("best",0))
    }

    @Test fun failedRewardCommitDoesNotPayAnyPartialBonus() {
        val prefs = LocalTestPrefs()
        GameSettlement.settle(prefs,"reward-fail",false,0,7,9,0,0,false)!!
        val beforeCoins = GameEconomy.wallet(prefs)
        val beforeNotes = GameEconomy.cash(prefs)
        prefs.allowCommit = false
        assertEquals(null,RewardedRoundBonus.claimDouble(prefs,"reward-fail"))
        assertEquals(beforeCoins,GameEconomy.wallet(prefs))
        assertEquals(beforeNotes,GameEconomy.cash(prefs))
        prefs.allowCommit = true
        assertTrue(RewardedRoundBonus.claimDouble(prefs,"reward-fail") != null)
    }

    @Test fun rewardedBonusRejectsUnknownOldAndEmptyRounds() {
        val prefs = LocalTestPrefs()
        assertFalse(RewardedRoundBonus.eligible(prefs,"missing"))
        GameSettlement.settle(prefs,"round-first",false,0,8,6,0,0,false)!!
        GameSettlement.settle(prefs,"round-second",false,0,0,0,0,0,false)!!
        assertFalse(RewardedRoundBonus.eligible(prefs,"round-first"))
        assertFalse(RewardedRoundBonus.eligible(prefs,"round-second"))
        assertEquals(null,RewardedRoundBonus.claimDouble(prefs,"round-first"))
    }
    @Test fun relicsBankOnlyOnAtomicSettlementAndNeverTwice() {
        val prefs = LocalTestPrefs()
        val first = GameSettlement.settle(prefs,"relic-round",false,
            95,8,6,0,0,false,relicsCollected=5)!!
        assertEquals(5,first.relicsBanked)
        assertEquals(5,first.relicWallet)
        assertEquals(5,GameRelics.balance(prefs))
        assertEquals(first,GameSettlement.settle(prefs,"relic-round",false,
            999,999,999,0,0,true,relicsCollected=12))
        assertEquals(5,GameRelics.balance(prefs))
        assertEquals(first,GameSettlement.latestReceipt(prefs))
        val second = GameSettlement.settle(prefs,"next-round",false,
            0,0,0,0,0,false,relicsCollected=99)!!
        assertEquals(12,second.relicsBanked)
        assertEquals(17,GameRelics.balance(prefs))
        assertEquals(null,GameSettlement.settle(prefs,"relic-round",false,
            1000,1000,1000,0,0,true,relicsCollected=12))
    }

    @Test fun failedSettlementDoesNotBankRelicsOrOtherRewards() {
        val prefs = LocalTestPrefs()
        prefs.allowCommit = false
        assertEquals(null,GameSettlement.settle(prefs,"relic-fail",false,
            60,8,8,0,0,false,relicsCollected=4))
        assertEquals(0,GameRelics.balance(prefs))
        assertEquals(0,GameEconomy.wallet(prefs))
        prefs.allowCommit = true
        val receipt = GameSettlement.settle(prefs,"relic-fail",false,
            60,8,8,0,0,false,relicsCollected=4)!!
        assertEquals(4,receipt.relicsBanked)
        assertEquals(4,GameRelics.balance(prefs))
    }

    @Test fun relicShopExchangesUseOneCommitAndRepayCreditDebt() {
        val prefs = LocalTestPrefs()
        prefs.edit().putInt(GameRelics.WalletKey,8)
            .putInt(GameEconomy.DebtKey,3).commit()
        // Three credits are repaid before adding the remaining two to the wallet.
        assertTrue(GameRelics.exchange(prefs,RelicDeal.COINS))
        assertEquals(5,GameRelics.balance(prefs))
        assertEquals(0,GameEconomy.debt(prefs))
        assertEquals(2,GameEconomy.wallet(prefs))
        assertTrue(GameRelics.exchange(prefs,RelicDeal.NOTES))
        assertEquals(3,GameRelics.balance(prefs))
        assertEquals(18,GameEconomy.cash(prefs))
        prefs.allowCommit = false
        assertFalse(GameRelics.exchange(prefs,RelicDeal.NOTES))
        assertEquals(3,GameRelics.balance(prefs))
        assertEquals(18,GameEconomy.cash(prefs))
    }

    @Test fun relicBoostsAreBoughtOnceAndConsumedAtNextRunStart() {
        val prefs = LocalTestPrefs()
        prefs.edit().putInt(GameRelics.WalletKey,20).commit()
        assertTrue(GameRelics.exchange(prefs,RelicDeal.START_SHIELD))
        assertFalse(GameRelics.exchange(prefs,RelicDeal.START_SHIELD))
        assertTrue(GameRelics.pendingShield(prefs))
        assertTrue(GameRelics.exchange(prefs,RelicDeal.START_MAGNET))
        assertTrue(GameRelics.pendingMagnet(prefs))
        assertEquals(8,GameRelics.balance(prefs))
        prefs.allowCommit = false
        assertEquals(0f,GameRelics.consumeStartShield(prefs))
        assertTrue(GameRelics.pendingShield(prefs))
        prefs.allowCommit = true
        assertEquals(3f,GameRelics.consumeStartShield(prefs))
        assertEquals(0f,GameRelics.consumeStartShield(prefs))
        assertEquals(6f,GameRelics.consumeStartMagnet(prefs))
        assertEquals(0f,GameRelics.consumeStartMagnet(prefs))
        assertFalse(GameRelics.pendingMagnet(prefs))
    }

    @Test fun rewardAdRelicRequiresRealSettledDropAndClaimsOnce() {
        val prefs = LocalTestPrefs()
        assertFalse(GameRelics.rewardedEligible(prefs,"missing"))
        GameSettlement.settle(prefs,"relic-ad-round",false,
            0,0,0,0,0,false,relicsCollected=2)!!
        assertTrue(GameRelics.rewardedEligible(prefs,"relic-ad-round"))
        prefs.allowCommit = false
        assertFalse(GameRelics.claimAdBonus(prefs,"relic-ad-round"))
        assertEquals(2,GameRelics.balance(prefs))
        prefs.allowCommit = true
        assertTrue(GameRelics.claimAdBonus(prefs,"relic-ad-round"))
        assertFalse(GameRelics.claimAdBonus(prefs,"relic-ad-round"))
        assertEquals(3,GameRelics.balance(prefs))
        GameSettlement.settle(prefs,"empty-ad-round",false,
            0,0,0,0,0,false,relicsCollected=0)!!
        assertFalse(GameRelics.rewardedEligible(prefs,"relic-ad-round"))
        assertFalse(GameRelics.rewardedEligible(prefs,"empty-ad-round"))
    }

    @Test fun previousReceiptFormatRemainsReadable() {
        val prefs = LocalTestPrefs()
        prefs.edit()
            .putString("last_settlement_id","legacy-relic-receipt")
            .putString("last_settlement_receipt_v2","1|1|10|2|3|0|0|2|3|0|0|-")
            .commit()
        val receipt = GameSettlement.latestReceipt(prefs)!!
        assertEquals(2,receipt.coinsBanked)
        assertEquals(0,receipt.relicsBanked)
        assertEquals(0,receipt.relicWallet)
    }

}
