package br.com.proximaera.boletorush

/** Consent UI is allowed only while the menu is visible and unobstructed.
 * Gameplay, other screens and background state are never safe to prompt.
 */
internal object GameAdScreenPolicy {
    fun canPresentConsent(
        isMenu: Boolean,
        foreground: Boolean,
        blockingDialog: Boolean
    ): Boolean = isMenu && foreground && !blockingDialog
}
