package org.scottishtecharmy.soundscape.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Read-only reactive view of a boolean preference. Observes changes via the
 * provider's listener mechanism so callers re-compose when the value updates
 * elsewhere (e.g. from a settings toggle).
 */
@Composable
fun rememberBooleanPreference(
    provider: PreferencesProvider?,
    key: String,
    default: Boolean,
): State<Boolean> {
    val state = remember(provider, key) {
        mutableStateOf(provider?.getBoolean(key, default) ?: default)
    }
    DisposableEffect(provider, key) {
        val listener = if (provider == null) {
            null
        } else {
            PreferencesListener { changed ->
                if (changed == key) {
                    state.value = provider.getBoolean(key, default)
                }
            }
        }
        if (listener != null) provider!!.addListener(listener)
        onDispose { if (listener != null) provider!!.removeListener(listener) }
    }
    return state
}

/**
 * Mutable reactive view of a boolean preference. Reads observe the provider;
 * writes go through `putBoolean`, which fires listener notifications so
 * other observers update too. Use this to back a Compose toggle so the value
 * is consistent with other readers in the app.
 */
@Composable
fun rememberBooleanPreferenceState(
    provider: PreferencesProvider,
    key: String,
    default: Boolean,
): MutableState<Boolean> {
    val backing = rememberBooleanPreference(provider, key, default)
    return remember(backing, provider, key) {
        object : MutableState<Boolean> {
            override var value: Boolean
                get() = backing.value
                set(v) {
                    provider.putBoolean(key, v)
                }

            override fun component1(): Boolean = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
}
