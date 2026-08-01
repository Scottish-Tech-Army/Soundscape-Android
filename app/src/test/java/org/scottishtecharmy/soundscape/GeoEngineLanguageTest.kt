package org.scottishtecharmy.soundscape

import org.junit.Test
import org.scottishtecharmy.soundscape.geoengine.getPhotonLanguage
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesListener
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class FakePreferencesProvider(private val values: Map<String, String>) : PreferencesProvider {
    override fun getBoolean(key: String, default: Boolean) = default
    override fun getString(key: String, default: String) = values[key] ?: default
    override fun getFloat(key: String, default: Float) = default
    override fun putBoolean(key: String, value: Boolean) = throw NotImplementedError()
    override fun putString(key: String, value: String) = throw NotImplementedError()
    override fun clearAll() = throw NotImplementedError()
    override fun addListener(listener: PreferencesListener) = throw NotImplementedError()
    override fun removeListener(listener: PreferencesListener) = throw NotImplementedError()
}

/**
 * The Photon search server (https://photon.soundscape.scottishtecharmy.org) is only built with
 * English, French, German and each location's local language. Passing any other "lang" query
 * param makes Photon return an error, so getPhotonLanguage() must fall back to not passing a
 * language at all for unsupported languages (e.g. Arabic) - Photon then returns results matched
 * and named in the local language, which is exactly what's wanted.
 */
class GeoEngineLanguageTest {
    private fun withDefaultLocale(language: String, block: () -> Unit) {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.Builder().setLanguage(language).build())
            block()
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun unsupportedDeviceLanguageFallsBackToNull() {
        withDefaultLocale("ar") {
            assertNull(getPhotonLanguage(null))
            assertNull(getPhotonLanguage(FakePreferencesProvider(mapOf(PreferenceKeys.SEARCH_LANGUAGE to "auto"))))
        }
    }

    @Test
    fun explicitSupportedLanguageOverridesDeviceLocale() {
        withDefaultLocale("ar") {
            assertEquals("en", getPhotonLanguage(FakePreferencesProvider(mapOf(PreferenceKeys.SEARCH_LANGUAGE to "en"))))
        }
    }

    @Test
    fun supportedDeviceLanguageIsPassedThrough() {
        withDefaultLocale("de") {
            assertEquals("de", getPhotonLanguage(null))
        }
        withDefaultLocale("fr") {
            assertEquals("fr", getPhotonLanguage(FakePreferencesProvider(mapOf(PreferenceKeys.SEARCH_LANGUAGE to "auto"))))
        }
    }
}
