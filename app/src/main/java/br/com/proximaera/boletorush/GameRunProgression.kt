package br.com.proximaera.boletorush

/** Two presentations of the same rules. No real politicians or political parties. */
internal enum class GameTheme { BOLETO_RUSH, BRASIL_EM_APUROS }

/**
 * Score denominations are visible on every falling coin; combo and PIX only
 * multiply the score. Upgrade tokens and cosmetic notes remain separate.
 * XP never decreases when the player loses score to overdraft.
 */
internal object GameRunProgression {
    fun coinValue(roll: Float, chapter: Int): Int {
        val safe = roll.coerceIn(0f, 1f)
        val rareFrom = if (chapter >= 2) .88f else .93f
        return when {
            safe < .54f -> 5
            safe < rareFrom -> 10
            else -> 20
        }
    }

    fun level(xp: Int): Int = when {
        xp < 40 -> 1
        xp < 110 -> 2
        xp < 220 -> 3
        xp < 360 -> 4
        xp < 550 -> 5
        else -> 6
    }

    fun nextLevelXp(xp: Int): Int? =
        listOf(40, 110, 220, 360, 550).firstOrNull { it > xp.coerceAtLeast(0) }

    fun rank(theme: GameTheme, level: Int): String {
        val i = (level - 1).coerceIn(0, 5)
        return if (theme == GameTheme.BRASIL_EM_APUROS) {
            arrayOf("ELEITOR APERTADO", "CIDADÃO ESPERTO", "FISCAL DAS CONTAS",
                "MESTRE DAS EMENDAS", "AUDITOR SUPREMO", "LENDA DO ORÇAMENTO")[i]
        } else {
            arrayOf("ESTAGIÁRIO", "POUPADOR", "CAÇADOR DE BOLETOS",
                "GERENTE DO SALDO", "MESTRE DO PIX", "LENDA DA CARTEIRA")[i]
        }
    }

    fun chapterName(theme: GameTheme, chapter: Int): String =
        if (theme == GameTheme.BRASIL_EM_APUROS) {
            when (chapter) {
                0 -> "PROMESSAS DE CAMPANHA"
                1 -> "CHUVA DE TAXAS"
                2 -> "GUERRA DO ORÇAMENTO"
                else -> "APAGÃO DAS CONTAS"
            }
        } else GamePacing.chapterName(chapter)
}
