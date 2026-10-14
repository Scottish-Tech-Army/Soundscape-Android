package org.scottishtecharmy.soundscape.screens.home.settings

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import me.zhanghai.compose.preference.MapPreferences
import me.zhanghai.compose.preference.Preferences

/**
 * Preferences flow for [me.zhanghai.compose.preference.ProvidePreferenceLocals].
 *
 * On Android the default `createDefaultPreferenceFlow()` is fine — it uses
 * an app-scoped SharedPreferences file that other SDKs do not touch.
 *
 * On iOS the library's default assumes exclusive ownership of the app's
 * NSUserDefaults persistent domain. Firebase Crashlytics writes its cached
 * remote settings (a nested NSDictionary) into that same domain, which
 * makes the library's read path throw `IllegalArgumentException` on the
 * next Settings composition and its write path silently wipe Firebase's
 * cache on every user preference change. The iOS actual replaces both
 * sides with a version that skips foreign value types on read and merges
 * them back on write.
 *
 * Both actuals read the store once when the screen opens and write back what the user changes,
 * so a setting changed from anywhere else - the headphone button, the audio menu, Siri, Gemini -
 * would otherwise leave the open Settings screen showing the old value. Each platform watches
 * its store for changes and feeds them in through [publishExternalChange].
 */
@Composable
internal expect fun rememberSoundscapePreferenceFlow(): MutableStateFlow<Preferences>

/**
 * Publishes a change made outside the Settings screen, ignoring one that only says back what the
 * flow already holds.
 *
 * That guard is what stops a loop rather than an optimisation: the flow writes every change to
 * the store, the store then reports it, and MapPreferences has no equals of its own, so an
 * unguarded update would be a new instance every time and the two would chase each other round
 * for as long as the screen was open.
 *
 * @return true if the flow was updated
 */
internal fun MutableStateFlow<Preferences>.publishExternalChange(
    snapshot: Map<String, Any>
): Boolean {
    if (snapshot == value.asMap()) return false
    value = MapPreferences(snapshot)
    return true
}

/**
 * What has to be written to a preference store to take it from [known] to [desired]: the keys to
 * remove, and the keys to write.
 *
 * Android writes the difference rather than the whole map because the store holds more than this
 * screen's settings - sleep-resume keeps a route id there as a Long, which the preference
 * library's own write path refuses outright, and rewriting everything on each change would throw
 * on it. Writing only what changed also leaves keys of types the library can't represent alone,
 * the same way the iOS path merges foreign values back.
 *
 * [known] is what the flow itself last held, not what is in the store: only a key the screen has
 * seen and then dropped is a key the screen means to remove. A key written by something else -
 * the service saving a sleep-resume route, a migration running as the engine starts - can appear
 * between the flow being read and its listener being registered, and removing those because the
 * flow had never heard of them would delete another component's state on the next tap.
 */
internal data class PreferenceEdits(
    val removed: Set<String>,
    val changed: Map<String, Any>,
)

internal fun preferenceEdits(
    known: Map<String, Any>,
    desired: Map<String, Any>,
): PreferenceEdits = PreferenceEdits(
    removed = known.keys - desired.keys,
    changed = desired.filter { (key, value) -> known[key] != value },
)
