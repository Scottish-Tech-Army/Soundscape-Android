package org.scottishtecharmy.soundscape.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

data class LicenseInfo(
    val project: String?,
    val description: String?,
    val version: String?,
    val developers: List<String> = emptyList(),
    val url: String?,
    val licenses: List<Pair<String,String>> = emptyList(),
    val isExpanded: Boolean = false
)

// 2. Update the UI state to hold a list of these objects.
data class OpenSourceLicensesUiState(
    val licenses: List<LicenseInfo> = emptyList(),
    val licenseTypes: Map<String, String> = emptyMap(),
    val error: String? = null
)

@HiltViewModel
class OpenSourceLicensesViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpenSourceLicensesUiState())
    // Expose the immutable StateFlow to the UI
    val uiState: StateFlow<OpenSourceLicensesUiState> = _uiState.asStateFlow()

    fun toggleLicense(licenseToToggle: LicenseInfo) {
        val currentLicenses = _uiState.value.licenses.toMutableList()
        val index = currentLicenses.indexOf(licenseToToggle)
        if (index != -1) {
            val updatedLicense = licenseToToggle.copy(isExpanded = !licenseToToggle.isExpanded)
            currentLicenses[index] = updatedLicense
            _uiState.value = _uiState.value.copy(licenses = currentLicenses)
        }
    }

    init {
        loadAndParseLicenses()
    }

    private fun loadAndParseLicenses() {
        viewModelScope.launch {
            try {
                val licensesJsonString = appContext.assets.open("open_source_licenses.json")
                    .bufferedReader()
                    .use { it.readText() }

                val parsedLicenses = parseLicenses(licensesJsonString)

                // Create a map of license types mapping URL to name. The URL is far more reliable
                // as a unique identifier as the names vary e.g. "Apache-2.0" or "Apache License 2.0" etc.
                val licenseTypes = mutableMapOf<String, String>()
                for(license in parsedLicenses) {
                    if(license.licenses.isNotEmpty()) {
                        if(license.url != null) {
                            if (!licenseTypes.containsKey(license.licenses[0].second))
                                licenseTypes[license.licenses[0].second] = license.licenses[0].first
                        }
                    }
                }

                _uiState.value = OpenSourceLicensesUiState(
                    licenses = parsedLicenses,
                    licenseTypes = licenseTypes
                )
            } catch (e: Exception) {
                _uiState.value = OpenSourceLicensesUiState(error = "Error loading licenses.")
            }
        }
    }

    private fun parseLicenses(jsonString: String): List<LicenseInfo> {
        val licenseList = mutableListOf<LicenseInfo>()
        val jsonArray = JSONArray(jsonString)

        val licenseSet = mutableMapOf<String, LicenseInfo>()

        for (i in 0 until jsonArray.length()) {
            val licenseObject = jsonArray.getJSONObject(i)

            fun parseDeveloperArray(key: String): List<String> {
                val list = mutableListOf<String>()
                val array = licenseObject.optJSONArray(key) ?: return emptyList()
                for (j in 0 until array.length()) {
                    list.add(array.getString(j))
                }
                return list
            }

            fun parseLicenseArray(key: String): List<Pair<String,String>> {
                val list = mutableListOf<Pair<String,String>>()
                val array = licenseObject.optJSONArray(key) ?: return emptyList()
                for (j in 0 until array.length()) {
                    val license = array[j] as JSONObject
                    // Ensure that the license url is https and not http
                    val licenseUrl = license.getString("license_url")
                    val noProtocolUrl = licenseUrl.substringAfter("//")
                    val secureUrl = "https://$noProtocolUrl"
                    list.add(Pair(license.getString("license"), secureUrl))
                }
                return list
            }

            fun JSONObject.optStringOrNull(key: String): String? {
                // If the key is not present or the value is null, return null. Otherwise, return the string.
                return if (has(key) && !isNull(key)) getString(key) else null
            }

            val li = LicenseInfo(
                project = licenseObject.optStringOrNull("project"),
                description = licenseObject.optStringOrNull("description"),
                version = licenseObject.optStringOrNull("version"),
                url = licenseObject.optStringOrNull("url"),
                developers = parseDeveloperArray("developers"),
                licenses = parseLicenseArray("licenses")
            )

            // De-duplicate where the project, URL and description all match
            if(li.project != null) {
                if (licenseSet.containsKey(li.project)) {
                    val existing = licenseSet[li.project]
                    if (existing?.url == li.url && existing?.description == li.description)
                        continue
                }
                licenseList.add(li)
                licenseSet[li.project] = li
            }
        }

        // Sort the list alphabetically by library name for a cleaner presentation.
        return licenseList.sortedBy { it.project?.lowercase() }
    }
}
