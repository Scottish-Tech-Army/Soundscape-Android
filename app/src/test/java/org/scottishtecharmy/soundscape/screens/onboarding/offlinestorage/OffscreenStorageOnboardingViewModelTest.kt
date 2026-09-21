package org.scottishtecharmy.soundscape.screens.onboarding.offlinestorage

import android.content.Context
import android.content.SharedPreferences
import android.os.storage.StorageManager
import androidx.preference.PreferenceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * A minimal in-memory implementation of [SharedPreferences] (and its [SharedPreferences.Editor])
 * used to back the static-mocked [PreferenceManager.getDefaultSharedPreferences] calls that these
 * view models make. Using a real implementation instead of a mock avoids having to stub every
 * getter/putter individually and lets edits round-trip correctly.
 */
private class FakeSharedPreferences : SharedPreferences {
    val map = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = map

    override fun getString(key: String?, defValue: String?): String? =
        map[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        map[key] as? MutableSet<String> ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        map[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = map.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
    }

    inner class FakeEditor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearFlag = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putStringSet(
            key: String?,
            values: MutableSet<String>?
        ): SharedPreferences.Editor {
            if (key != null) pending[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) removals.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearFlag = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearFlag) map.clear()
            removals.forEach { map.remove(it) }
            map.putAll(pending)
        }
    }
}

class OffscreenStorageOnboardingViewModelTest {

    /** Builds a Context mock whose offline-storage-related calls are all stubbed to be "empty". */
    private fun mockContext(): Context {
        val context = mock<Context>()
        whenever(context.getExternalFilesDirs(null)).thenReturn(emptyArray())
        whenever(context.getSystemService(Context.STORAGE_SERVICE)).thenReturn(mock<StorageManager>())
        return context
    }

    @Test
    fun construction_noExternalStorageAndNoStoredPreference_defaultsToEmptyStorageState() {
        val context = mockContext()
        val fakePrefs = FakeSharedPreferences()

        Mockito.mockStatic(PreferenceManager::class.java).use { mockedStatic ->
            mockedStatic.`when`<SharedPreferences> {
                PreferenceManager.getDefaultSharedPreferences(context)
            }.thenReturn(fakePrefs)

            val viewModel = OffscreenStorageOnboardingViewModel(context)

            val state = viewModel.uiState.value
            assertTrue(state.storages.isEmpty())
            assertEquals("", state.currentPath)
            assertEquals(-1, state.selectedStorageIndex)
        }
    }

    @Test
    fun selectStorage_updatesCurrentPathAndPersistsPreference() {
        val context = mockContext()
        val fakePrefs = FakeSharedPreferences()

        Mockito.mockStatic(PreferenceManager::class.java).use { mockedStatic ->
            mockedStatic.`when`<SharedPreferences> {
                PreferenceManager.getDefaultSharedPreferences(context)
            }.thenReturn(fakePrefs)

            val viewModel = OffscreenStorageOnboardingViewModel(context)

            viewModel.selectStorage("/some/chosen/path")

            val state = viewModel.uiState.value
            assertEquals("/some/chosen/path", state.currentPath)
            // No known storages were configured, so no matching index is found.
            assertEquals(-1, state.selectedStorageIndex)
            assertEquals("/some/chosen/path", fakePrefs.map["SelectedStorage"])
        }
    }
}
