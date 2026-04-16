package org.scottishtecharmy.soundscape.preferences

/**
 * Platform-neutral key/value preferences lookup. Android backs this with
 * `SharedPreferences`; iOS will back it with `NSUserDefaults`.
 *
 * Pass `null` to callers that don't need any preferences — they'll fall back to
 * their built-in defaults.
 */
interface PreferencesProvider {
    fun getBoolean(key: String, default: Boolean): Boolean
}

object PreferenceKeys {
    const val ALLOW_CALLOUTS = "AllowCallouts"
    const val POSITION_INCLUDES_HEADING_AND_DISTANCE = "PositionTextDescription"
}
