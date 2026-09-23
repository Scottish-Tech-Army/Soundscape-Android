package org.scottishtecharmy.soundscape.screens.home.settings

import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import me.zhanghai.compose.preference.MapPreferences
import me.zhanghai.compose.preference.Preferences

/**
 * Deliberately not the library's createDefaultPreferenceFlow(): that one writes the whole map
 * back on every change, clearing the file first, and refuses to write a Long at all unless a
 * global opt-in is set. This file is shared with the rest of the app (see
 * AndroidPreferencesProvider and SoundscapeService's sleep-resume route id, which is a Long), so
 * it writes only the keys that actually changed - see [preferenceEdits].
 */
@Composable
@Suppress("DEPRECATION")
internal actual fun rememberSoundscapePreferenceFlow(): MutableStateFlow<Preferences> {
    val context = LocalContext.current
    val sharedPreferences =
        remember(context) { PreferenceManager.getDefaultSharedPreferences(context) }
    val flow = remember(sharedPreferences) {
        MutableStateFlow(sharedPreferences.asPreferences())
    }

    // Settings changed on this screen, out to the store.
    LaunchedEffect(sharedPreferences, flow) {
        flow.drop(1).collect { preferences -> sharedPreferences.write(preferences.asMap()) }
    }

    // ...and settings changed anywhere else - the headphone button, the audio menu, Siri,
    // Gemini - back in, so an open Settings screen shows them.
    DisposableEffect(sharedPreferences, flow) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { preferences, _ ->
                flow.publishExternalChange(preferences.asPreferences().asMap())
            }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return flow
}

@Suppress("UNCHECKED_CAST")
private fun SharedPreferences.asPreferences(): Preferences =
    MapPreferences(all.filterValues { it != null } as Map<String, Any>)

private fun SharedPreferences.write(desired: Map<String, Any>) {
    val edits = preferenceEdits(asPreferences().asMap(), desired)
    if (edits.removed.isEmpty() && edits.changed.isEmpty()) return
    edit {
        for (key in edits.removed) remove(key)
        for ((key, value) in edits.changed) {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is String -> putString(key, value)
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    putStringSet(key, value as Set<String>)
                }
                // Nothing else can be in a Preferences map, and a value this screen never
                // produced is better left alone than guessed at.
                else -> {}
            }
        }
    }
}
