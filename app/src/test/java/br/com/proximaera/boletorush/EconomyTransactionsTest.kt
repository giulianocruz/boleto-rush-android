package br.com.proximaera.boletorush

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class LocalTestPrefs : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()

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
            if (clearFirst) data.clear()
            removed.forEach { data.remove(it) }
            data.putAll(changes)
            return true
        }
    }
}

class EconomyTransactionsTest {
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
}
