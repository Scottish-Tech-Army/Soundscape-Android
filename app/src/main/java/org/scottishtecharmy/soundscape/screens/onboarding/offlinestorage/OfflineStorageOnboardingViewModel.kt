package org.scottishtecharmy.soundscape.screens.onboarding.offlinestorage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.MainActivity
import javax.inject.Inject
import androidx.core.content.edit
import org.scottishtecharmy.soundscape.utils.StorageUtils
import org.scottishtecharmy.soundscape.utils.getOfflineMapStorage


data class OfflineStorageOnboardingUiState(
    // Storage status
    val storages: List<StorageUtils.StorageSpace> = emptyList(),

    val currentPath: String = "",
    val selectedStorageIndex: Int = -1
)

@HiltViewModel
class OffscreenStorageOnboardingViewModel @Inject constructor(@param:ApplicationContext val appContext: Context): ViewModel() {

    private val _uiState = MutableStateFlow(OfflineStorageOnboardingUiState())
    val uiState: StateFlow<OfflineStorageOnboardingUiState> = _uiState

    init {
        val storages = getOfflineMapStorage(appContext)

        // Get the currently selected storage and if uninitialized set it to the first external storage
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        var path = sharedPreferences.getString(MainActivity.SELECTED_STORAGE_KEY, MainActivity.SELECTED_STORAGE_DEFAULT)

        var currentIndex = -1
        for((index, storage) in storages.withIndex()) {
            if (storage.path == path) {
                currentIndex = index
                break
            }
        }
        _uiState.value = _uiState.value.copy(
            currentPath = path!!,
            selectedStorageIndex = currentIndex,
            storages = storages
        )
    }

    fun selectStorage(path: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        sharedPreferences.edit(commit = true) { putString(MainActivity.SELECTED_STORAGE_KEY, path) }

        var currentIndex = -1
        for((index, storage) in _uiState.value.storages.withIndex()) {
            if (storage.path == path) {
                currentIndex = index
                break
            }
        }
        _uiState.value = _uiState.value.copy(
            currentPath = path,
            selectedStorageIndex = currentIndex
        )
    }
}