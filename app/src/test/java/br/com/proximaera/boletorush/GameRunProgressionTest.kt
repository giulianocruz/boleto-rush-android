package br.com.proximaera.boletorush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRunProgressionTest {
    @Test fun coinsHaveReadableValuesInAllChapters() {
        assertEquals(5, GameRunProgression.coinValue(.0f, 0))
        assertEquals(5, GameRunProgression.coinValue(.53f, 3))
        assertEquals(10, GameRunProgression.coinValue(.55f, 0))
        assertEquals(10, GameRunProgression.coinValue(.87f, 3))
        assertEquals(20, GameRunProgression.coinValue(.99f, 0))
        assertEquals(20, GameRunProgression.coinValue(.90f, 3))
    }

    @Test fun comboAndPixAmplifyFaceValueButNotUpgradeEconomy() {
        assertEquals(5, GameDrops.coinScore(1, false, 5))
        assertEquals(10, GameDrops.coinScore(1, true, 5))
        assertEquals(20, GameDrops.coinScore(1, false, 20))
        assertEquals(38, GameDrops.coinScore(10, false, 20))
        assertEquals(76, GameDrops.coinScore(10, true, 20))
        assertEquals(2, GameEconomy.earnings(10, 0))
    }

    @Test fun xpLevelsNeverDecreaseAndCapAtSix() {
        assertEquals(1, GameRunProgression.level(0))
        assertEquals(2, GameRunProgression.level(40))
        assertEquals(3, GameRunProgression.level(110))
        assertEquals(4, GameRunProgression.level(220))
        assertEquals(5, GameRunProgression.level(360))
        assertEquals(6, GameRunProgression.level(550))
        assertEquals(40, GameRunProgression.nextLevelXp(0))
        assertNull(GameRunProgression.nextLevelXp(1000))
    }

    @Test fun brazilThemeIsFictionalAndDoesNotChangeCoreEconomy() {
        assertEquals("PROMESSAS DE CAMPANHA",
            GameRunProgression.chapterName(GameTheme.BRASIL_EM_APUROS, 0))
        assertEquals("LENDA DO ORÇAMENTO",
            GameRunProgression.rank(GameTheme.BRASIL_EM_APUROS, 6))
        assertEquals("ESTAGIÁRIO",
            GameRunProgression.rank(GameTheme.BOLETO_RUSH, 1))
        assertEquals("FUGIR DAS TAXAS",
            GameChapterMissions.title(0, GameTheme.BRASIL_EM_APUROS))
        assertEquals("FAZER A RESERVA",
            GameChapterMissions.title(0, GameTheme.BOLETO_RUSH))
        assertTrue(GameRunProgression.chapterName(GameTheme.BOLETO_RUSH, 1).isNotBlank())
    }
}
