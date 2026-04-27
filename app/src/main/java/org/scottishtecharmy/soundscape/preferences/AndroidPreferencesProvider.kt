package org.scottishtecharmy.soundscape.preferences

import android.content.SharedPreferences
import androidx.core.content.edit

class AndroidPreferencesProvider(
    private val sharedPreferences: SharedPreferences
) : PreferencesProvider {
    private val androidListeners = mutableMapOf<PreferencesListener, SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getBoolean(key: String, default: Boolean): Boolean =
        sharedPreferences.getBoolean(key, default)

    override fun getString(key: String, default: String): String =
        sharedPreferences.getString(key, default) ?: default

    override fun getFloat(key: String, default: Float): Float =
        sharedPreferences.getFloat(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }

    override fun putString(key: String, value: String) {
        sharedPreferences.edit { putString(key, value) }
    }

    override fun addListener(listener: PreferencesListener) {
        val androidListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key != null) listener.onPreferenceChanged(key)
        }
        androidListeners[listener] = androidListener
        sharedPreferences.registerOnSharedPreferenceChangeListener(androidListener)
    }

    override fun removeListener(listener: PreferencesListener) {
        androidListeners.remove(listener)?.let {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(it)
        }
    }
}
