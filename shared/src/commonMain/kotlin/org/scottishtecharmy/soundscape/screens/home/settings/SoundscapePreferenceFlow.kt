package org.scottishtecharmy.soundscape.screens.home.settings

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
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
 */
@Composable
internal expect fun rememberSoundscapePreferenceFlow(): MutableStateFlow<Preferences>
