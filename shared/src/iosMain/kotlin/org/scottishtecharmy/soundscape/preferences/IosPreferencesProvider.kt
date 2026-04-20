package org.scottishtecharmy.soundscape.preferences

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of PreferencesProvider using NSUserDefaults.
 */
class IosPreferencesProvider : PreferencesProvider {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val listeners = mutableListOf<PreferencesListener>()

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return if (defaults.objectForKey(key) != null) {
            defaults.boolForKey(key)
        } else {
            default
        }
    }

    override fun getString(key: String, default: String): String {
        return defaults.stringForKey(key) ?: default
    }

    override fun getFloat(key: String, default: Float): Float {
        return if (defaults.objectForKey(key) != null) {
            defaults.floatForKey(key)
        } else {
            default
        }
    }

    override fun addListener(listener: PreferencesListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: PreferencesListener) {
        listeners.remove(listener)
    }
}
