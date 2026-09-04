package org.scottishtecharmy.soundscape.preferences

import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDefaultsDidChangeNotification

/**
 * iOS implementation of PreferencesProvider using NSUserDefaults.
 */
class IosPreferencesProvider : PreferencesProvider {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val listeners = mutableListOf<PreferencesListener>()

    // Track previous values so we can detect which keys actually changed
    private var previousSnapshot: Map<String, Any?> = captureSnapshot()

    private val observer = NSNotificationCenter.defaultCenter.addObserverForName(
        name = NSUserDefaultsDidChangeNotification,
        `object` = defaults,
        queue = NSOperationQueue.mainQueue,
    ) { _: NSNotification? ->
        notifyChangedKeys()
    }

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

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun clearAll() {
        // Drop every key in the app domain. NSUserDefaults posts a single
        // change notification, so any registered listeners will be notified
        // for each key that actually changed via notifyChangedKeys().
        //
        // Except the legacy app's own settings. We share a bundle identifier
        // with it, so its GDA* keys sit in the same domain as ours - but they
        // are not ours to reset. LegacyMigrator copies them rather than moving
        // them precisely so that a user who rolls back to the legacy build
        // still has their settings, and so the migration can be re-run or
        // corrected later; deleting them here would quietly break both.
        @Suppress("UNCHECKED_CAST")
        val keys = (defaults.dictionaryRepresentation() as? Map<String, Any?>)?.keys
            ?: emptySet()
        for (key in keys) {
            if (key.startsWith(LEGACY_KEY_PREFIX)) continue
            defaults.removeObjectForKey(key)
        }
    }

    override fun addListener(listener: PreferencesListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: PreferencesListener) {
        listeners.remove(listener)
    }

    @Suppress("UNCHECKED_CAST")
    private fun captureSnapshot(): Map<String, Any?> {
        return (defaults.dictionaryRepresentation() as? Map<String, Any?>) ?: emptyMap()
    }

    private fun notifyChangedKeys() {
        val current = captureSnapshot()
        val allKeys = previousSnapshot.keys + current.keys
        for (key in allKeys) {
            val oldValue = previousSnapshot[key]
            val newValue = current[key]
            if (oldValue != newValue) {
                for (listener in listeners) {
                    listener.onPreferenceChanged(key)
                }
            }
        }
        previousSnapshot = current
    }

    private companion object {
        /**
         * Prefix on every setting belonging to the legacy Microsoft Soundscape iOS app, which
         * shares our bundle identifier and therefore our UserDefaults domain. See LegacyMigrator
         * in the iOS app target.
         */
        const val LEGACY_KEY_PREFIX = "GDA"
    }
}
