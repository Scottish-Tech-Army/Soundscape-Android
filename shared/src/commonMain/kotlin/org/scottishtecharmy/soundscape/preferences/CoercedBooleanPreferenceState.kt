package org.scottishtecharmy.soundscape.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import me.zhanghai.compose.preference.LocalPreferenceFlow
import me.zhanghai.compose.preference.Preferences

/**
 * A boolean read of [key] that tolerates the value being stored as a number rather than a
 * genuine boolean.
 *
 * `me.zhanghai.compose.preference`'s own `rememberPreferenceState` does `map[key] as T?`, an
 * unchecked cast. On iOS, our NSUserDefaults-backed [me.zhanghai.compose.preference.Preferences]
 * flow (SoundscapePreferenceFlow.ios.kt) classifies a stored value as Boolean or Int purely from
 * the width of its underlying NSNumber (see its `toPreferenceValueOrNull`) - and NSUserDefaults
 * itself doesn't reliably preserve "this was a Bool" across a write/read round trip for every
 * code path that can reach a given key (Swift's `Any?`-boxing of a native `Bool` versus an
 * `NSNumber` built from an `Int`, for instance). When that happens the cast throws
 * ClassCastException: Int cannot be cast to Boolean, straight out of a settings screen's
 * LazyColumn composing the switch - see the two crashes this was added for (MixAudio and
 * HeadTrackingEnabled on iOS's MainViewController, then RecordTravel/AlphaFeatures in the
 * shared Debug section).
 *
 * Common rather than iOS-only because SharedSettingsScreen's switchPreference calls run on both
 * platforms; it's a no-op safety net on Android, which doesn't have this ambiguity.
 *
 * Nonzero numbers read as true, mirroring how most platforms treat a C-style boolean.
 */
private fun Preferences.getBooleanCoerced(key: String, defaultValue: Boolean): Boolean =
    when (val raw = get<Any?>(key)) {
        null -> defaultValue
        is Boolean -> raw
        is Number -> raw.toInt() != 0
        else -> defaultValue
    }

@Composable
fun rememberCoercedBooleanPreferenceState(
    key: String,
    defaultValue: Boolean,
    flow: MutableStateFlow<Preferences> = LocalPreferenceFlow.current,
): MutableState<Boolean> {
    val valueFlow = remember(key, defaultValue, flow) {
        flow.map { it.getBooleanCoerced(key, defaultValue) }
    }
    return valueFlow
        .collectAsStateWithLifecycle(flow.value.getBooleanCoerced(key, defaultValue))
        .asMutable { value ->
            flow.value = flow.value.toMutablePreferences().apply { this[key] = value }
        }
}

private fun State<Boolean>.asMutable(setValue: (Boolean) -> Unit): MutableState<Boolean> =
    AsMutableBooleanState(this, setValue)

@Stable
private class AsMutableBooleanState(
    private val state: State<Boolean>,
    private val setValue: (Boolean) -> Unit,
) : MutableState<Boolean> {
    override var value: Boolean
        get() = state.value
        set(value) = setValue(value)

    override fun component1(): Boolean = state.value

    override fun component2(): (Boolean) -> Unit = setValue
}
