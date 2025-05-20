package org.scottishtecharmy.soundscape.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DataStoreManager @Inject constructor(private val context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "preferences"
    )

    /**
     * Set a preference value
     * @param key: The key of the preference
     * @param value: The value of the preference
     */
    suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    /**
     * Get a preference value
     * @param key: The key of the preference
     * @param defaultValue: The default value of the preference
     */
    suspend fun <T> getValue(key: Preferences.Key<T>, defaultValue: T): T {
        val preferences = context.dataStore.data.first()
        return preferences[key] ?: defaultValue
    }
}