package org.scottishtecharmy.soundscape

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.scottishtecharmy.soundscape.components.LocationListActions
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesListener
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.MapApp
import org.scottishtecharmy.soundscape.screens.home.locationDetails.SharedLocationDetailsScreen
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesList
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.MarkersAndRoutesUiState
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme

/**
 * What TalkBack is given for Location Details' "Open in Maps App" button and its chooser. adb
 * can't steer TalkBack (injected touches and keys bypass it), so these read the platform
 * AccessibilityNodeInfo tree - the same objects TalkBack speaks from.
 *
 * - With an app remembered, the long press carries a label, so TalkBack says "double tap and
 *   hold to choose a different app", and there's no duplicate custom action (that is iOS only).
 * - The chooser's apps are a list, so TalkBack says "in list, N items" and each item's position.
 */
class OpenInMapAppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val apps = listOf(
        MapApp("com.google.android.apps.maps", "Maps"),
        MapApp("com.waze", "Waze"),
        MapApp("net.osmand", "OsmAnd"),
    )

    private val marker = LocationDescription(
        name = "Buchanan Galleries",
        location = LngLatAlt(-4.2518, 55.86421),
        databaseId = 7,
    )

    private class Prefs(initial: Map<String, String>) : PreferencesProvider {
        val strings = initial.toMutableMap()
        private val listeners = mutableListOf<PreferencesListener>()
        override fun getBoolean(key: String, default: Boolean) = key != "ShowMap" && default
        override fun getString(key: String, default: String) = strings[key] ?: default
        override fun getFloat(key: String, default: Float) = default
        override fun putBoolean(key: String, value: Boolean) {}
        override fun putString(key: String, value: String) {
            strings[key] = value
            listeners.toList().forEach { it.onPreferenceChanged(key) }
        }
        override fun clearAll() {}
        override fun addListener(listener: PreferencesListener) { listeners += listener }
        override fun removeListener(listener: PreferencesListener) { listeners -= listener }
    }

    private fun show(prefs: Prefs) {
        composeTestRule.setContent {
            SoundscapeTheme(MutableStateFlow(ThemeState())) {
                SharedLocationDetailsScreen(
                    locationDescription = LocationDescription(
                        name = "Buchanan Galleries",
                        location = LngLatAlt(-4.2518, 55.86421),
                    ),
                    userLocation = null,
                    preferencesProvider = prefs,
                    onNavigateUp = {},
                    onStartBeacon = { _, _ -> },
                    mapApps = apps,
                    onOpenInMapApp = { _, _ -> },
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private val standardIds = setOf(
        AccessibilityNodeInfo.ACTION_CLICK,
        AccessibilityNodeInfo.ACTION_LONG_CLICK,
        AccessibilityNodeInfo.ACTION_FOCUS,
        AccessibilityNodeInfo.ACTION_CLEAR_FOCUS,
        AccessibilityNodeInfo.ACTION_SELECT,
        AccessibilityNodeInfo.ACTION_CLEAR_SELECTION,
        AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
        AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS,
        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
        AccessibilityNodeInfo.ACTION_SET_SELECTION,
        android.R.id.accessibilityActionShowOnScreen,
    )

    /** The labels of a node's custom actions, in the order TalkBack lists them. */
    private fun AccessibilityNodeInfo.customActionLabels(): List<String> =
        actionList.filter { it.id !in standardIds }.mapNotNull { it.label?.toString() }

    private fun AccessibilityNodeInfo.performCustomAction(label: String) {
        val action = actionList.first { it.label?.toString() == label }
        assertTrue("$label failed", performAction(action.id))
        composeTestRule.waitForIdle()
    }

    private fun showMarkers(prefs: Prefs, actions: (Prefs) -> LocationListActions) {
        composeTestRule.setContent {
            SoundscapeTheme(MutableStateFlow(ThemeState())) {
                MarkersAndRoutesList(
                    uiState = MarkersAndRoutesUiState(entries = listOf(marker), markers = true),
                    userLocation = null,
                    onSelect = {},
                    itemActions = actions(prefs),
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun roots(): List<AccessibilityNodeInfo> {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        return automation.windows.mapNotNull { it.root }
    }

    private fun AccessibilityNodeInfo.descendants(): Sequence<AccessibilityNodeInfo> = sequence {
        yield(this@descendants)
        for (i in 0 until childCount) getChild(i)?.let { yieldAll(it.descendants()) }
    }

    /**
     * The clickable node TalkBack focuses for [text]. Compose's node provider doesn't implement
     * findAccessibilityNodeInfosByText, so walk every window's tree.
     */
    private fun findByText(text: String): AccessibilityNodeInfo {
        // Compose only builds its accessibility tree once a service is attached, so give it time.
        var node: AccessibilityNodeInfo? = null
        var seen = emptyList<String>()
        repeat(50) {
            if (node != null) return@repeat
            val all = roots().flatMap { it.descendants().toList() }
            seen = all.mapNotNull { it.text?.toString() }
            node = all.firstOrNull {
                // List items read their name first, then address and distance.
                it.text?.toString() == text ||
                    it.contentDescription?.toString()?.startsWith(text) == true
            }
            if (node == null) Thread.sleep(100)
        }
        assertNotNull("No accessibility node with text '$text' in $seen", node)
        var n: AccessibilityNodeInfo? = node
        while (n != null && !n.isClickable) n = n.parent
        return n ?: node!!
    }

    @Test
    fun rememberedApp_longPressIsLabelled_andNoDuplicateCustomAction() {
        show(Prefs(mapOf(PreferenceKeys.PREFERRED_MAP_APP to "com.waze")))

        val semantics = composeTestRule
            .onNodeWithTag("locationDetailsOpenInMapApp").fetchSemanticsNode().config
        assertEquals(
            "Choose a different app",
            semantics.getOrNull(SemanticsActions.OnLongClick)?.label,
        )
        assertEquals(null, semantics.getOrNull(SemanticsActions.CustomActions))

        val node = findByText("Open in Waze")
        // TalkBack reads the click label as "Double tap to view this location in Waze".
        assertEquals(
            "view this location in Waze",
            node.actionList.first { it.id == AccessibilityNodeInfo.ACTION_CLICK }.label,
        )
        val longClick = node.actionList.firstOrNull {
            it.id == AccessibilityNodeInfo.ACTION_LONG_CLICK
        }
        assertNotNull("TalkBack should be offered a long press", longClick)
        assertEquals("Choose a different app", longClick!!.label)
        // Only standard actions: no custom "Choose a different app" duplicating the long press.
        val labelled = node.actionList.filter { it.id !in standardIds }.map { it.label }
        assertFalse("Unexpected custom actions $labelled", labelled.contains("Choose a different app"))
    }

    @Test
    fun noRememberedApp_hasNoLongPress() {
        show(Prefs(emptyMap()))
        val node = findByText("Open in Maps App")
        assertEquals(
            "view this location in another map or navigation app",
            node.actionList.first { it.id == AccessibilityNodeInfo.ACTION_CLICK }.label,
        )
        assertFalse(node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_LONG_CLICK })
    }

    @Test
    fun chooser_isATalkBackList() {
        show(Prefs(emptyMap()))
        composeTestRule.onNodeWithTag("locationDetailsOpenInMapApp").performClick()
        composeTestRule.onNodeWithText("Choose which app to open with").fetchSemanticsNode()

        val listSemantics = composeTestRule
            .onNodeWithTag("mapAppChoice_com.waze").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.CollectionItemInfo)
        assertEquals(1, listSemantics?.rowIndex)

        val first = findByText("Maps")
        var list: AccessibilityNodeInfo? = first.parent
        while (list != null && list.collectionInfo == null) list = list.parent
        assertNotNull("The apps should be inside a node with collection info", list)
        assertEquals(apps.size, list!!.collectionInfo.rowCount)
        assertEquals(1, list.collectionInfo.columnCount)

        apps.forEachIndexed { index, app ->
            val item = findByText(app.name)
            assertNotNull("${app.name} should have list position", item.collectionItemInfo)
            assertEquals(index, item.collectionItemInfo.rowIndex)
        }
    }

    @Test
    fun markersList_offersOpenInRememberedAppAndShare() {
        var opened: Pair<MapApp, LocationDescription>? = null
        var shared: LocationDescription? = null
        showMarkers(Prefs(mapOf(PreferenceKeys.PREFERRED_MAP_APP to "com.waze"))) { prefs ->
            LocationListActions(
                mapApps = apps,
                preferencesProvider = prefs,
                onOpenInMapApp = { app, desc -> opened = app to desc },
                onShare = { shared = it },
            )
        }

        val item = findByText("Buchanan Galleries")
        assertEquals(
            listOf("Start audio beacon at this marker", "Open in Waze", "Share"),
            item.customActionLabels(),
        )

        item.performCustomAction("Open in Waze")
        assertEquals("com.waze", opened?.first?.id)
        assertEquals("Buchanan Galleries", opened?.second?.name)

        item.performCustomAction("Share")
        assertEquals(7L, shared?.databaseId)
    }

    @Test
    fun markersList_openInWithNoRememberedApp_asksWhichApp() {
        var opened: MapApp? = null
        showMarkers(Prefs(emptyMap())) { prefs ->
            LocationListActions(
                mapApps = apps,
                preferencesProvider = prefs,
                onOpenInMapApp = { app, _ -> opened = app },
            )
        }

        val item = findByText("Buchanan Galleries")
        // No share callback, so no Share action.
        assertEquals(
            listOf("Start audio beacon at this marker", "Open in Maps App"),
            item.customActionLabels(),
        )
        item.performCustomAction("Open in Maps App")
        composeTestRule.onNodeWithText("Choose which app to open with").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("mapAppChoice_net.osmand").performClick()
        assertEquals("net.osmand", opened?.id)
    }

    @Test
    fun markersList_withoutActions_keepsJustTheBeacon() {
        showMarkers(Prefs(emptyMap())) { LocationListActions() }
        assertEquals(
            listOf("Start audio beacon at this marker"),
            findByText("Buchanan Galleries").customActionLabels(),
        )
    }
}
