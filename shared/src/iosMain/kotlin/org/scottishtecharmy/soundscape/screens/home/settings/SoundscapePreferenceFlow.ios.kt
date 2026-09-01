package org.scottishtecharmy.soundscape.screens.home.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.MapPreferences
import me.zhanghai.compose.preference.Preferences
import platform.Foundation.NSArray
import platform.Foundation.NSBundle
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun rememberSoundscapePreferenceFlow(): MutableStateFlow<Preferences> =
    remember { createSoundscapePreferenceFlow(NSUserDefaults.standardUserDefaults) }

private fun createSoundscapePreferenceFlow(
    userDefaults: NSUserDefaults,
): MutableStateFlow<Preferences> {
    val flow = MutableStateFlow(userDefaults.readSoundscapePreferences())
    @OptIn(DelicateCoroutinesApi::class)
    GlobalScope.launch(Dispatchers.Main.immediate) {
        flow.drop(1).collect { userDefaults.writeSoundscapePreferences(it) }
    }
    return flow
}

private fun NSUserDefaults.readSoundscapePreferences(): Preferences {
    val bundleId = NSBundle.mainBundle.bundleIdentifier ?: return MapPreferences(emptyMap())
    @Suppress("UNCHECKED_CAST")
    val dictionary =
        (persistentDomainForName(bundleId) as? Map<String, Any>)
            ?: return MapPreferences(emptyMap())
    return MapPreferences(
        buildMap {
            for ((key, value) in dictionary) {
                if (key.isLegacyKey()) continue
                val converted = value.toPreferenceValueOrNull() ?: continue
                put(key, converted)
            }
        },
    )
}

private fun NSUserDefaults.writeSoundscapePreferences(preferences: Preferences) {
    val bundleId = NSBundle.mainBundle.bundleIdentifier ?: return
    // setPersistentDomain replaces the entire domain, so re-read it now and
    // keep any foreign keys (Firebase Crashlytics's cached remote settings
    // dictionary, etc.) that we cannot represent in a Preferences map, plus
    // the legacy app's keys, which have to survive verbatim.
    @Suppress("UNCHECKED_CAST")
    val foreign =
        ((persistentDomainForName(bundleId) as? Map<String, Any>).orEmpty())
            .filter { (key, value) -> key.isLegacyKey() || value.toPreferenceValueOrNull() == null }
    val converted =
        preferences.asMap().mapValues { (_, mapValue) ->
            @Suppress("CAST_NEVER_SUCCEEDS")
            when (mapValue) {
                is Boolean -> mapValue as NSNumber
                is Int -> mapValue as NSNumber
                is Float -> mapValue as NSNumber
                is String -> mapValue as NSString
                is Set<*> ->
                    @Suppress("UNCHECKED_CAST")
                    (mapValue as Set<String>).map { it as NSString } as NSArray
                else -> throw IllegalArgumentException("Unsupported type for value $mapValue")
            }
        }
    setPersistentDomain(converted + foreign, bundleId)
}

// The legacy iOS app's settings, which LegacyMigrator translates once on
// first launch and then leaves in place untouched. Excluding them from the
// Preferences map keeps them out of the app's settings UI and, because the
// write path then carries them across verbatim, stops a round-trip through
// the preference types from rewriting them (a legacy double re-read as a
// Float, say).
private fun String.isLegacyKey(): Boolean = startsWith("GDA")

// Values stored in NSUserDefaults by third-party SDKs (nested NSDictionary
// from Firebase Crashlytics's settings cache, NSNumber with an objCType
// the compose-preference API does not model, arrays of non-string
// elements, etc.) are skipped by returning null. The read path drops
// them from the exposed Preferences; the write path preserves them so
// the SDK's cache is not lost.
private fun Any.toPreferenceValueOrNull(): Any? {
    @Suppress("CAST_NEVER_SUCCEEDS")
    return when (this) {
        is NSNumber ->
            @OptIn(ExperimentalForeignApi::class)
            when (objCType?.toKString()) {
                "c", "C", "B" -> boolValue
                "i", "I", "s", "S", "l", "L", "q", "Q" -> intValue
                "f", "d" -> floatValue
                else -> null
            }
        is NSString -> this as String
        is NSArray -> {
            @Suppress("UNCHECKED_CAST")
            val list = this as List<Any?>
            if (list.all { it is NSString }) {
                @Suppress("UNCHECKED_CAST")
                (list as List<NSString>).mapTo(mutableSetOf()) { it as String }
            } else {
                null
            }
        }
        else -> null
    }
}
