package org.scottishtecharmy.soundscape.utils

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

/**
 * Return the application locale if there is one, otherwise return the default system one
 */
fun getCurrentLocale() : Locale {
    val appLocale = AppCompatDelegate.getApplicationLocales()[0]
    return appLocale ?: Locale.getDefault()
}
