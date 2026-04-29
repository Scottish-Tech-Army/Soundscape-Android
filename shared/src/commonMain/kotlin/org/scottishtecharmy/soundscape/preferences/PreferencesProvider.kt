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

    fun putBoolean(key: String, value: Boolean)
    fun putString(key: String, value: String)

    fun addListener(listener: PreferencesListener)
    fun removeListener(listener: PreferencesListener)
}

fun interface PreferencesListener {
    fun onPreferenceChanged(key: String)
}

object PreferenceKeys {
    const val ALLOW_CALLOUTS = "AllowCallouts"
    const val PLACES_AND_LANDMARKS = org.scottishtecharmy.soundscape.geoengine.PLACES_AND_LANDMARKS_KEY
    const val MOBILITY = org.scottishtecharmy.soundscape.geoengine.MOBILITY_KEY
    const val DISTANCE_TO_BEACON = "DistanceToBeacon"
    const val POSITION_INCLUDES_HEADING_AND_DISTANCE = "PositionTextDescription"
    const val RELATIVE_DIRECTION = "RelativeDirectionMode"

    const val BEACON_TYPE = "BeaconType"
    const val SPEECH_RATE = "SpeechRate"

    const val RECORD_TRAVEL = "RecordTravel"
    const val MEASUREMENT_UNITS = "MeasurementUnits"
    const val SEARCH_LANGUAGE = "SearchLanguage"
    const val SELECTED_STORAGE = "SelectedStorage"
    const val GEOCODER_MODE = "GeocoderMode"
    const val MIX_AUDIO = "MixAudio"
    const val MEDIA_CONTROLS_MODE = "MediaControlsMode"
    const val FIRST_LAUNCH = "FirstLaunch"
    const val SHOW_MAP = "ShowMap"
    const val LANGUAGE_SUPPORTED_PROMPTED = "LanguageSupported"
    const val LAST_NEW_RELEASE = "LastNewRelease"
    const val MARKERS_SORT_BY_NAME = "MarkersSortByName"
    const val MARKERS_SORT_ASCENDING = "MarkersSortAscending"
}

object PreferenceDefaults {
    const val ALLOW_CALLOUTS = true
    const val PLACES_AND_LANDMARKS = true
    const val MOBILITY = true
    const val DISTANCE_TO_BEACON = true
    const val POSITION_INCLUDES_HEADING_AND_DISTANCE = false
    const val RELATIVE_DIRECTION = "ClockFace"

    const val BEACON_TYPE = "Classic"
    const val SPEECH_RATE = 1.0f

    const val RECORD_TRAVEL = false
    const val MEASUREMENT_UNITS = "Auto"
    const val SEARCH_LANGUAGE = "auto"
    const val SELECTED_STORAGE = ""
    const val GEOCODER_MODE = "Auto"
    const val MIX_AUDIO = false
    const val MEDIA_CONTROLS_MODE = "Original"
    const val FIRST_LAUNCH = true
    const val SHOW_MAP = true
    const val LANGUAGE_SUPPORTED_PROMPTED = false
    const val LAST_NEW_RELEASE = ""
    const val MARKERS_SORT_BY_NAME = false  // distance by default
    const val MARKERS_SORT_ASCENDING = true
}
