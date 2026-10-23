package org.scottishtecharmy.soundscape.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

/**
 * Pop the current screen in response to an on-screen back button.
 *
 * A bare popBackStack() from a back button can empty the back stack: a quick
 * double tap lands the second tap on the exiting screen and pops the start
 * destination too, leaving the NavHost with nothing to draw (a blank window
 * that relaunching the app doesn't fix). So the pop is ignored while a previous
 * one is still animating (the new current entry isn't RESUMED yet), and it
 * never pops the last entry.
 */
fun NavController.navigateBack(): Boolean {
    val current = currentBackStackEntry ?: return false
    if (!current.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return false
    if (previousBackStackEntry == null) return false
    return popBackStack()
}
