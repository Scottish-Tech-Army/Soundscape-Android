package org.scottishtecharmy.soundscape.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.storage.StorageManager
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.scottishtecharmy.soundscape.MainActivity
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection

/**
 * A minimal in-memory implementation of [SharedPreferences] (and its [SharedPreferences.Editor])
 * used to back the static-mocked [PreferenceManager.getDefaultSharedPreferences] calls that
 * [SettingsViewModel] (and the [org.scottishtecharmy.soundscape.utils.getOfflineMapStorage] util
 * it calls) make. A real implementation avoids having to stub every getter/putter of a mock
 * individually, and lets edits made by the view model round-trip correctly.
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

        override fun remove(key: String?): SharedPreferences.Editor = this
        override fun clear(): SharedPreferences.Editor = this

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            map.putAll(pending)
        }
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Context stubbed so that getOfflineMapStorage()/refreshMicrophones() see "nothing available". */
    private fun mockContext(): Context {
        val context = mock<Context>()
        whenever(context.getExternalFilesDirs(null)).thenReturn(emptyArray())
        whenever(context.getSystemService(Context.STORAGE_SERVICE)).thenReturn(mock<StorageManager>())
        val audioManager = mock<AudioManager>()
        whenever(audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS))
            .thenReturn(emptyArray<AudioDeviceInfo>())
        whenever(context.getSystemService(Context.AUDIO_SERVICE)).thenReturn(audioManager)
        return context
    }

    private fun mockConnection(): SoundscapeServiceConnection {
        val connection = mock<SoundscapeServiceConnection>()
        whenever(connection.serviceBoundState).thenReturn(MutableStateFlow(false).asStateFlow())
        return connection
    }

    @Test
    fun construction_beforeInitCoroutineRuns_stateHasDefaultValues() {
        // The service-connection/context mocks are unconfigured stubs here: the init{} coroutine
        // is only *scheduled* by construction (StandardTestDispatcher doesn't auto-run without an
        // explicit advance), so it never actually touches them. Note this deliberately does NOT
        // use runTest {}, since runTest drains all pending coroutines - including this
        // unconfigured one - before the test completes.
        val viewModel = SettingsViewModel(mock(), mock())

        val state = viewModel.state.value
        assertTrue(state.beaconDescriptions.isEmpty())
        assertTrue(state.beaconValues.isEmpty())
        assertTrue(state.storages.isEmpty())
        assertEquals("", state.currentStoragePath)
        assertEquals(-1, state.selectedStorageIndex)
        assertEquals(listOf("Auto"), state.microphoneDescriptions)
        assertEquals(
            listOf(MainActivity.VOICE_COMMAND_MICROPHONE_DEFAULT),
            state.microphoneValues
        )
    }

    @Test
    fun construction_withNoStoragesOrServiceBound_populatesStateFromMocks() = runTest(testDispatcher) {
        val context = mockContext()
        val fakePrefs = FakeSharedPreferences()

        Mockito.mockStatic(PreferenceManager::class.java).use { mockedStatic ->
            mockedStatic.`when`<SharedPreferences> {
                PreferenceManager.getDefaultSharedPreferences(context)
            }.thenReturn(fakePrefs)

            val viewModel = SettingsViewModel(mockConnection(), context)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.storages.isEmpty())
            assertEquals("", state.currentStoragePath)
            assertEquals(0, state.selectedStorageIndex)
            assertEquals(listOf("Auto"), state.microphoneDescriptions)
        }
    }

    @Test
    fun selectStorage_updatesStateAndPersistsChosenPath() = runTest(testDispatcher) {
        val context = mockContext()
        val fakePrefs = FakeSharedPreferences()

        Mockito.mockStatic(PreferenceManager::class.java).use { mockedStatic ->
            mockedStatic.`when`<SharedPreferences> {
                PreferenceManager.getDefaultSharedPreferences(context)
            }.thenReturn(fakePrefs)

            val viewModel = SettingsViewModel(mockConnection(), context)
            advanceUntilIdle()

            viewModel.selectStorage("/chosen/path")

            val state = viewModel.state.value
            assertEquals("/chosen/path", state.currentStoragePath)
            // No storages were registered, so no matching index is found.
            assertEquals(-1, state.selectedStorageIndex)
            assertEquals("/chosen/path", fakePrefs.map[MainActivity.SELECTED_STORAGE_KEY])
        }
    }

    @Test
    fun beaconPreviewMethods_delegateToServiceConnection() {
        val connection = mockConnection()
        val viewModel = SettingsViewModel(connection, mock())

        viewModel.startBeaconPreview("Current")
        viewModel.updateBeaconPreviewType("Classic")
        viewModel.stopBeaconPreview(true, "Classic")

        verify(connection).startBeaconPreview("Current")
        verify(connection).updateBeaconPreviewType("Classic")
        verify(connection).stopBeaconPreview(true, "Classic")
    }

    @Test
    fun refreshMicrophones_noInputDevices_resetsToAutoOnly() {
        val context = mockContext()
        val viewModel = SettingsViewModel(mockConnection(), context)

        viewModel.refreshMicrophones()

        val state = viewModel.state.value
        assertEquals(listOf("Auto"), state.microphoneDescriptions)
        assertEquals(
            listOf(MainActivity.VOICE_COMMAND_MICROPHONE_DEFAULT),
            state.microphoneValues
        )
    }
}
