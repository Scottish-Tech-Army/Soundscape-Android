package org.scottishtecharmy.soundscape.viewmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class LicenseInfo(
    val project: String?,
    val description: String?,
    val version: String?,
    val developers: List<String> = emptyList(),
    val url: String?,
    val licenses: List<Pair<String, String>> = emptyList(),
    val isExpanded: Boolean = false,
)

data class OpenSourceLicensesUiState(
    val licenses: List<LicenseInfo> = emptyList(),
    val licenseTypes: Map<String, String> = emptyMap(),
    val error: String? = null,
)

class OpenSourceLicensesStateHolder(jsonString: String) {
    private val _uiState = MutableStateFlow(OpenSourceLicensesUiState())
    val uiState: StateFlow<OpenSourceLicensesUiState> = _uiState.asStateFlow()

    init {
        loadAndParse(jsonString)
    }

    fun toggleLicense(licenseToToggle: LicenseInfo) {
        val currentLicenses = _uiState.value.licenses.toMutableList()
        val index = currentLicenses.indexOf(licenseToToggle)
        if (index != -1) {
            val updatedLicense = licenseToToggle.copy(isExpanded = !licenseToToggle.isExpanded)
            currentLicenses[index] = updatedLicense
            _uiState.value = _uiState.value.copy(licenses = currentLicenses)
        }
    }

    private fun loadAndParse(jsonString: String) {
        try {
            val parsedLicenses = parseLicenses(jsonString)

            // Map license URL -> license name. URL is more reliable as a unique
            // identifier as the names vary e.g. "Apache-2.0" vs "Apache License 2.0".
            val licenseTypes = mutableMapOf<String, String>()
            for (license in parsedLicenses) {
                if (license.licenses.isNotEmpty() && license.url != null) {
                    val (name, url) = license.licenses[0]
                    if (!licenseTypes.containsKey(url)) {
                        licenseTypes[url] = name
                    }
                }
            }

            _uiState.value = OpenSourceLicensesUiState(
                licenses = parsedLicenses,
                licenseTypes = licenseTypes,
            )
        } catch (e: Exception) {
            _uiState.value = OpenSourceLicensesUiState(error = "Error loading licenses.")
        }
    }

    private fun parseLicenses(jsonString: String): List<LicenseInfo> {
        val licenseList = mutableListOf<LicenseInfo>()
        val jsonArray = Json.parseToJsonElement(jsonString).jsonArray

        val licenseSet = mutableMapOf<String, LicenseInfo>()

        for (element in jsonArray) {
            val obj = element.jsonObject

            val li = LicenseInfo(
                project = obj.optString("project"),
                description = obj.optString("description"),
                version = obj.optString("version"),
                url = obj.optString("url"),
                developers = obj.parseStringArray("developers"),
                licenses = obj.parseLicenseArray(),
            )

            // De-duplicate where the project, URL and description all match.
            if (li.project != null) {
                val existing = licenseSet[li.project]
                if (existing != null && existing.url == li.url && existing.description == li.description) {
                    continue
                }
                licenseList.add(li)
                licenseSet[li.project] = li
            }
        }

        return licenseList.sortedBy { it.project?.lowercase() }
    }

    private fun JsonObject.optString(key: String): String? {
        val element = this[key] ?: return null
        if (element !is JsonPrimitive) return null
        return element.contentOrNull
    }

    private fun JsonObject.parseStringArray(key: String): List<String> {
        val arr = (this[key] as? JsonArray) ?: return emptyList()
        return arr.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    }

    private fun JsonObject.parseLicenseArray(): List<Pair<String, String>> {
        val arr = (this["licenses"] as? JsonArray) ?: return emptyList()
        return arr.mapNotNull { entry ->
            val licenseObj = entry as? JsonObject ?: return@mapNotNull null
            val name = licenseObj["license"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val rawUrl = licenseObj["license_url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            // Force https:// in license URLs.
            val noProtocolUrl = rawUrl.substringAfter("//")
            val secureUrl = "https://$noProtocolUrl"
            name to secureUrl
        }
    }
}
