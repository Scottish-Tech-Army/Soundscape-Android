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
    fun getString(key: String, default: String): String
    fun getFloat(key: String, default: Float): Float

    fun addListener(listener: PreferencesListener)
    fun removeListener(listener: PreferencesListener)
}

fun interface PreferencesListener {
    fun onPreferenceChanged(key: String)
}

object PreferenceKeys {
    const val ALLOW_CALLOUTS = "AllowCallouts"
    const val POSITION_INCLUDES_HEADING_AND_DISTANCE = "PositionTextDescription"

    const val RECORD_TRAVEL = "RecordTravel"
    const val MEASUREMENT_UNITS = "MeasurementUnits"
    const val SEARCH_LANGUAGE = "SearchLanguage"
    const val SELECTED_STORAGE = "SelectedStorage"
    const val GEOCODER_MODE = "GeocoderMode"
}

object PreferenceDefaults {
    const val RECORD_TRAVEL = false
    const val MEASUREMENT_UNITS = "Auto"
    const val SEARCH_LANGUAGE = "auto"
    const val SELECTED_STORAGE = ""
    const val GEOCODER_MODE = "Auto"
}
