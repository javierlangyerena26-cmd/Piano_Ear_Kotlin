package com.oido.ui.navigation

/**
 * Simple, explicit navigation of the app. No navigation library is needed for
 * four destinations, which keeps dependencies minimal.
 */
sealed interface Screen {
    data object Home : Screen
    data class Training(val level: Int, val length: Int) : Screen
    data object Results : Screen
    data object Familiar : Screen
}
