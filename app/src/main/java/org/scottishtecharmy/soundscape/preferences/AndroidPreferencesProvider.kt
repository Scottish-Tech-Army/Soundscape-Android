package org.scottishtecharmy.soundscape.preferences

import android.content.SharedPreferences

class AndroidPreferencesProvider(
    private val sharedPreferences: SharedPreferences
) : PreferencesProvider {
    override fun getBoolean(key: String, default: Boolean): Boolean =
        sharedPreferences.getBoolean(key, default)
}
