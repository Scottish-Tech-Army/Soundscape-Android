package org.scottishtecharmy.soundscape.services.mediacontrol

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.scottishtecharmy.soundscape.resources.*
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.EARCON_MODE_ENTER
import org.scottishtecharmy.soundscape.audio.EARCON_MODE_EXIT
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt

/**
 * AudioMenu provides a hierarchical, navigable audio menu controlled by media buttons.
 *
 * - NEXT    : advance to next item at the current level (wraps at end)
 * - PREVIOUS: go back one item (wraps at end)
 * - SELECT  : enter a sub-menu, or execute a leaf action
 *
 * There is no inactivity timeout — the current position is remembered until changed.
 */
class AudioMenu(
    private val service: MediaControllableService,
    private val routeDao: RouteDao,
) {

    // ── Menu item types ───────────────────────────────────────────────────────

    sealed class MenuItem {
        abstract val label: String

        /** A leaf node that executes an action when selected. */
        data class Action(
            override val label: String,
            val action: () -> Unit
        ) : MenuItem()

        /** A sub-menu with a fixed list of children. */
        data class Submenu(
            override val label: String,
            val children: List<MenuItem>
        ) : MenuItem()

        /**
         * A sub-menu whose children are loaded lazily when the user enters it.
         * Useful for content that may change at runtime (e.g. saved routes).
         */
        data class DynamicSubmenu(
            override val label: String,
            val childrenProvider: suspend () -> List<MenuItem>
        ) : MenuItem()
    }

    // ── Navigation state ──────────────────────────────────────────────────────
    private data class MenuLevel(val items: List<MenuItem>, var currentIndex: Int)

    /** Stack of levels; the root is at index 0, deeper sub-menus higher up. */
    private val menuStack = ArrayDeque<MenuLevel>()

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        menuStack.addLast(MenuLevel(buildRootMenu(), 0))
    }

    // ── Public navigation API ─────────────────────────────────────────────────

    fun next() {
        service.callbackHoldOff()
        val level = menuStack.last()
        level.currentIndex = (level.currentIndex + 1) % level.items.size
        service.speak2dText(level.items[level.currentIndex].label, true)
    }

    fun previous() {
        service.callbackHoldOff()
        val level = menuStack.last()
        level.currentIndex =
            if(level.currentIndex == 0)
                level.items.size - 1
            else
                level.currentIndex - 1
        service.speak2dText(level.items[level.currentIndex].label, true)
    }

    fun select() {
        service.callbackHoldOff()
        val item = menuStack.last().let { it.items[it.currentIndex] }
        when (item) {
            is MenuItem.Action -> {
                item.action()
            }
            is MenuItem.Submenu -> {
                menuStack.addLast(MenuLevel(item.children, 0))
                service.speak2dText(item.children[0].label, true, EARCON_MODE_ENTER)
            }
            is MenuItem.DynamicSubmenu -> loadAndEnter(item)
        }
    }

    // ── Main-menu escape ──────────────────────────────────────────────────────

    /**
     * Returns a "Main Menu" Action item that, when selected, pops the entire
     * stack back to the root and announces the first root-level item.
     * Appended as the last child of every sub-menu.
     */
    private fun mainMenuAction(): MenuItem.Action =
        MenuItem.Action(kotlinx.coroutines.runBlocking { getString(Res.string.menu_main_menu) }) {
            resetToRoot()
        }

    private fun resetToRoot() {
        while (menuStack.size > 1) menuStack.removeLast()
        menuStack.last().currentIndex = 0
        val firstRootLabel = menuStack.last().items[0].label
        if (!service.requestAudioFocus()) {
            return
        }
        service.speak2dText(firstRootLabel, true, EARCON_MODE_EXIT)
    }

    // ── Audio helpers ─────────────────────────────────────────────────────────
    private fun loadAndEnter(item: MenuItem.DynamicSubmenu) {
        scope.launch {
            val children = item.childrenProvider()
            if (children.size <= 1) {
                service.speakText(getString(Res.string.menu_no_routes), AudioType.STANDARD)
            } else {
                menuStack.addLast(MenuLevel(children, 0))
                service.speak2dText(children[0].label, true, EARCON_MODE_ENTER)
            }
        }
    }

    // ── Menu definition ───────────────────────────────────────────────────────

    private fun buildRootMenu(): List<MenuItem> = listOf(

        MenuItem.Submenu(
            label = kotlinx.coroutines.runBlocking { getString(Res.string.callouts_panel_title) },
            children = listOf(
                MenuItem.Action(kotlinx.coroutines.runBlocking { getString(Res.string.directions_my_location) }) {
                    service.myLocation()
                },
                MenuItem.Action(kotlinx.coroutines.runBlocking { getString(Res.string.help_orient_page_title) }) {
                    service.whatsAroundMe()
                },
                MenuItem.Action(kotlinx.coroutines.runBlocking { getString(Res.string.help_explore_page_title) }) {
                    service.aheadOfMe()
                },
                MenuItem.Action(kotlinx.coroutines.runBlocking { getString(Res.string.callouts_nearby_markers) }) {
                    service.nearbyMarkers()
                },
                mainMenuAction(),
            )
        ),

        MenuItem.Submenu(
            label = kotlinx.coroutines.runBlocking { getString(Res.string.menu_route) },
            children = listOf(
                MenuItem.Action(kotlinx.coroutines.runBlocking { getString(Res.string.menu_route_next_waypoint) }) {
                    service.routeSkipNext()
                },
                MenuItem.Action(kotlinx.coroutines.runBlocking { getString(Res.string.menu_route_previous_waypoint) }) {
                    service.routeSkipPrevious()
                },
                MenuItem.Action(kotlinx.coroutines.runBlocking { getString(Res.string.beacon_action_mute_beacon) }) {
                    service.routeMute()
                },
                MenuItem.Action(kotlinx.coroutines.runBlocking { getString(Res.string.route_detail_action_stop_route) }) {
                    service.routeStop()
                },
                mainMenuAction(),
            )
        ),

        MenuItem.DynamicSubmenu(
            label = kotlinx.coroutines.runBlocking { getString(Res.string.route_detail_action_start_route) },
            childrenProvider = { loadRouteMenuItems() }
        ),

        MenuItem.DynamicSubmenu(
            label = kotlinx.coroutines.runBlocking { getString(Res.string.location_detail_action_beacon) },
            childrenProvider = { loadMarkerMenuItems() }
        ),
    )

    // ── Feature implementations ───────────────────────────────────────────────

    private suspend fun loadRouteMenuItems(): List<MenuItem> =
        withContext(Dispatchers.Default) {
            routeDao.getAllRoutes().map { route ->
                MenuItem.Action(route.name) { service.routeStartById(route.routeId) }
            } + mainMenuAction()
        }

    private suspend fun loadMarkerMenuItems(): List<MenuItem> =
        withContext(Dispatchers.Default) {
            routeDao.getAllMarkers().map { marker ->
                MenuItem.Action(marker.name) {
                    val location = LngLatAlt(marker.longitude, marker.latitude)
                    service.startBeacon(location, marker.name)
                }
            } + mainMenuAction()
        }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun destroy() {
        service.menuActive = false
        scope.cancel()
    }
}
