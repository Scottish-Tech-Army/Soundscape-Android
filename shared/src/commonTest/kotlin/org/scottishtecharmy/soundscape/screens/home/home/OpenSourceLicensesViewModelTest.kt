package org.scottishtecharmy.soundscape.screens.home.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenSourceLicensesViewModelTest {

    private val validJson = """
        [
          {
            "project": "Zeta",
            "description": "Zeta desc",
            "version": "1.0",
            "developers": ["Dev A", "Dev B"],
            "url": "https://example.com/zeta",
            "licenses": [{"license": "MIT", "license_url": "http://example.com/license/mit"}]
          },
          {
            "project": "alpha",
            "description": "Alpha desc",
            "version": "2.0",
            "developers": [],
            "url": "https://example.com/alpha",
            "licenses": [{"license": "Apache-2.0", "license_url": "https://example.com/license/apache"}]
          }
        ]
    """.trimIndent()

    @Test
    fun validJson_parsesAndSortsLicensesByProjectCaseInsensitive() {
        val vm = OpenSourceLicensesViewModel(validJson)

        val state = vm.uiState.value
        assertNull(state.error)
        assertEquals(2, state.licenses.size)
        // "alpha" sorts before "Zeta" once lower-cased.
        assertEquals(listOf("alpha", "Zeta"), state.licenses.map { it.project })
    }

    @Test
    fun validJson_buildsLicenseTypesMapKeyedByUrl() {
        val vm = OpenSourceLicensesViewModel(validJson)

        val licenseTypes = vm.uiState.value.licenseTypes
        assertEquals(2, licenseTypes.size)
        assertEquals("Apache-2.0", licenseTypes["https://example.com/license/apache"])
    }

    @Test
    fun validJson_forcesHttpsOnLicenseUrls() {
        val vm = OpenSourceLicensesViewModel(validJson)

        val zeta = vm.uiState.value.licenses.first { it.project == "Zeta" }
        assertEquals("MIT" to "https://example.com/license/mit", zeta.licenses.first())
    }

    @Test
    fun validJson_parsesDevelopersList() {
        val vm = OpenSourceLicensesViewModel(validJson)

        val zeta = vm.uiState.value.licenses.first { it.project == "Zeta" }
        assertEquals(listOf("Dev A", "Dev B"), zeta.developers)

        val alpha = vm.uiState.value.licenses.first { it.project == "alpha" }
        assertTrue(alpha.developers.isEmpty())
    }

    @Test
    fun duplicateProjectUrlAndDescription_isDeduplicated() {
        val json = """
            [
              {"project": "Foo", "description": "desc", "url": "https://example.com/foo", "licenses": []},
              {"project": "Foo", "description": "desc", "url": "https://example.com/foo", "licenses": []}
            ]
        """.trimIndent()

        val vm = OpenSourceLicensesViewModel(json)

        assertEquals(1, vm.uiState.value.licenses.size)
    }

    @Test
    fun sameProjectDifferentDescription_isNotDeduplicated() {
        val json = """
            [
              {"project": "Foo", "description": "desc one", "url": "https://example.com/foo", "licenses": []},
              {"project": "Foo", "description": "desc two", "url": "https://example.com/foo", "licenses": []}
            ]
        """.trimIndent()

        val vm = OpenSourceLicensesViewModel(json)

        assertEquals(2, vm.uiState.value.licenses.size)
    }

    @Test
    fun malformedJson_setsErrorAndLeavesLicensesEmpty() {
        val vm = OpenSourceLicensesViewModel("not valid json")

        val state = vm.uiState.value
        assertNotNull(state.error)
        assertTrue(state.licenses.isEmpty())
        assertTrue(state.licenseTypes.isEmpty())
    }

    @Test
    fun emptyArrayJson_parsesToEmptyState() {
        val vm = OpenSourceLicensesViewModel("[]")

        val state = vm.uiState.value
        assertNull(state.error)
        assertTrue(state.licenses.isEmpty())
    }

    @Test
    fun toggleLicense_flipsIsExpandedForMatchingEntry() {
        val vm = OpenSourceLicensesViewModel(validJson)
        val target = vm.uiState.value.licenses.first { it.project == "alpha" }
        assertFalse(target.isExpanded)

        vm.toggleLicense(target)

        val updated = vm.uiState.value.licenses.first { it.project == "alpha" }
        assertTrue(updated.isExpanded)

        // Toggling again collapses it back.
        vm.toggleLicense(updated)
        val collapsed = vm.uiState.value.licenses.first { it.project == "alpha" }
        assertFalse(collapsed.isExpanded)
    }

    @Test
    fun toggleLicense_unknownEntry_leavesStateUnchanged() {
        val vm = OpenSourceLicensesViewModel(validJson)
        val before = vm.uiState.value.licenses

        val notInList = LicenseInfo(project = "Unknown", description = null, version = null, url = null)
        vm.toggleLicense(notInList)

        assertEquals(before, vm.uiState.value.licenses)
    }
}
