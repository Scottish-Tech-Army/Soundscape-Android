package org.scottishtecharmy.soundscape.screens.home.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.utils.GLOBAL_MARKERS_FILE_ROOT
import org.scottishtecharmy.soundscape.utils.MarkersAndRoutesIo
import org.scottishtecharmy.soundscape.utils.buildMarkersAndRoutesArchive
import org.scottishtecharmy.soundscape.utils.restoreMarkersAndRoutesArchive

open class AdvancedMarkersAndRoutesSettingsViewModel(
    private val routeDao: RouteDao,
    private val io: MarkersAndRoutesIo,
) : ViewModel() {

    private val _userFeedback = MutableStateFlow("")
    val userFeedback: StateFlow<String> = _userFeedback

    fun deleteAllMarkersAndRoutes(successString: String) {
        viewModelScope.launch {
            routeDao.clearAll()
            _userFeedback.value = successString
        }
    }

    fun exportMarkersAndRoutes(shareTitle: String) {
        viewModelScope.launch {
            // The archive is a zip of GPX rather than an opaque database file so users can
            // inspect and edit their data in other tools. Building it is shared with the iCloud
            // backup, which stores the same archive by a different route.
            io.exportGpxZip(
                files = buildMarkersAndRoutesArchive(routeDao),
                suggestedFilename = "soundscape-routes-export",
                shareTitle = shareTitle,
            )
        }
    }

    fun importMarkersAndRoutes(successString: String, failureString: String) {
        viewModelScope.launch {
            val files = try {
                io.pickGpxZip()
            } catch (e: Exception) {
                println("Failed to pick zip: ${e.message}")
                null
            }
            if (files == null) return@launch
            try {
                val restored = restoreMarkersAndRoutesArchive(files, routeDao)
                _userFeedback.value = if (restored > 0) successString else failureString
            } catch (e: Exception) {
                println("Failed to import zip: ${e.message}")
                _userFeedback.value = failureString
            }
        }
    }

    fun userFeedbackShown() {
        _userFeedback.value = ""
    }

    companion object {
        /**
         * Kept as an alias of [GLOBAL_MARKERS_FILE_ROOT] so callers and tests that already refer
         * to it here don't have to move.
         */
        const val GLOBAL_MARKERS_NAME = GLOBAL_MARKERS_FILE_ROOT
    }
}
