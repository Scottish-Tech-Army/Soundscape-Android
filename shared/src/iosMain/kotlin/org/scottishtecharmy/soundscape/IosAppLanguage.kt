package org.scottishtecharmy.soundscape

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleLanguageDirectionRightToLeft
import platform.Foundation.NSUserDefaults
import platform.Foundation.characterDirectionForLanguage
import platform.Foundation.preferredLanguages

private const val APPLE_LANGUAGES_KEY = "AppleLanguages"

/**
 * Bumped each time the user changes the app's language in this session. Its only job is to
 * change, see [ProvideAppLanguage].
 */
private val LocalAppLanguageGeneration = staticCompositionLocalOf { 0 }

/**
 * Records the per-app language override (a BCP 47 tag, or null to follow the phone) where iOS
 * keeps it, so it also applies on every later launch.
 *
 * It takes effect in this process straight away: Compose's Locale.current and Compose Resources'
 * non-composable getString() both read NSLocale.preferredLanguages on each call, and that follows
 * the app's own AppleLanguages. What doesn't happen by itself is a recomposition, so the screens
 * on show keep their old strings until [ProvideAppLanguage] is handed a new generation.
 */
internal fun writeAppLanguageOverride(tag: String?) {
    val defaults = NSUserDefaults.standardUserDefaults
    if (tag != null) {
        defaults.setObject(listOf(tag), forKey = APPLE_LANGUAGES_KEY)
    } else {
        defaults.removeObjectForKey(APPLE_LANGUAGES_KEY)
    }
}

/**
 * Redraws [content] in the app's current language whenever [generation] changes.
 *
 * Changing a static composition local's value recomposes everything below it without skipping,
 * so every stringResource() call runs again, sees the new Locale.current and looks the string up
 * afresh. Unlike wrapping the content in key(), nothing is torn down: the navigation back stack,
 * the screen the user is on and its remembered state all survive, so they stay in Settings (or
 * on the onboarding language screen) looking at the language they just picked.
 *
 * The layout direction is part of it, because UIKit settles that at launch from the language the
 * app started in: switching between English and Arabic has to flip it here. Until the user picks
 * a language in this session the direction UIKit gave Compose is left alone.
 */
@Composable
internal fun ProvideAppLanguage(generation: Int, content: @Composable () -> Unit) {
    val layoutDirection = if (generation == 0) {
        LocalLayoutDirection.current
    } else {
        currentLanguageLayoutDirection()
    }
    CompositionLocalProvider(
        LocalAppLanguageGeneration provides generation,
        LocalLayoutDirection provides layoutDirection,
        content = content,
    )
}

private fun currentLanguageLayoutDirection(): LayoutDirection {
    val language = NSLocale.preferredLanguages.firstOrNull() as? String ?: return LayoutDirection.Ltr
    return if (NSLocale.characterDirectionForLanguage(language) == NSLocaleLanguageDirectionRightToLeft) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
}
