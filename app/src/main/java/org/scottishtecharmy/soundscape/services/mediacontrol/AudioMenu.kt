package org.scottishtecharmy.soundscape.services.mediacontrol

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.NativeAudioEngine
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabase
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.utils.getCurrentLocale

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
    private val service: SoundscapeService,
    private val application: Context) {

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

    val localizedContext: Context
    init {
        val configLocale = getCurrentLocale()
        val configuration = Configuration(application.applicationContext.resources.configuration)
        configuration.setLocale(configLocale)
        localizedContext = application.applicationContext.createConfigurationContext(configuration)
        menuStack.addLast(MenuLevel(buildRootMenu(), 0))
    }

    // ── Public navigation API ─────────────────────────────────────────────────

    fun next() {
        service.callbackHoldOff()
        val label = synchronized(this) {
            val level = menuStack.last()
            level.currentIndex = (level.currentIndex + 1) % level.items.size
            level.items[level.currentIndex].label
        }
        speak(label)
    }

    fun previous() {
        service.callbackHoldOff()
        val label = synchronized(this) {
            val level = menuStack.last()
            level.currentIndex =
                if(level.currentIndex == 0)
                    level.items.size - 1
                else
                    level.currentIndex - 1
            level.items[level.currentIndex].label
        }
        speak(label)
    }

    fun select() {
        service.callbackHoldOff()
        val item = synchronized(this) { menuStack.last().let { it.items[it.currentIndex] } }
        when (item) {
            is MenuItem.Action -> {
                if (!service.requestAudioFocus()) {
                    Log.w(TAG, "select: could not get audio focus")
                    return
                }
                service.audioEngine.clearTextToSpeechQueue()
                item.action()
            }
            is MenuItem.Submenu -> {
                val firstLabel = synchronized(this) {
                    menuStack.addLast(MenuLevel(item.children, 0))
                    item.children[0].label
                }
                speakWithEnterEarcon(firstLabel)
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
        MenuItem.Action(localizedContext.getString(R.string.menu_main_menu)) {
            resetToRoot()
        }

    private fun resetToRoot() {
        val firstRootLabel = synchronized(this) {
            while (menuStack.size > 1) menuStack.removeLast()
            menuStack.last().currentIndex = 0
            menuStack.last().items[0].label
        }
        if (!service.requestAudioFocus()) {
            Log.w(TAG, "resetToRoot: could not get audio focus")
            return
        }
        service.audioEngine.clearTextToSpeechQueue()
        service.audioEngine.createEarcon(NativeAudioEngine.EARCON_MODE_EXIT, AudioType.STANDARD)
        service.audioEngine.createTextToSpeech(firstRootLabel, AudioType.STANDARD)
    }

    // ── Audio helpers ─────────────────────────────────────────────────────────

    /** Announce a label with no earcon — used for plain navigation. */
    private fun speak(label: String) {
        if (!service.requestAudioFocus()) {
            Log.w(TAG, "speak: could not get audio focus")
            return
        }
        service.audioEngine.clearTextToSpeechQueue()
        service.audioEngine.createTextToSpeech(label, AudioType.STANDARD)
    }

    /** Announce a label preceded by the mode-enter earcon — used when descending into a sub-menu. */
    private fun speakWithEnterEarcon(label: String) {
        if (!service.requestAudioFocus()) {
            Log.w(TAG, "speakWithEnterEarcon: could not get audio focus")
            return
        }
        service.audioEngine.clearTextToSpeechQueue()
        service.audioEngine.createEarcon(NativeAudioEngine.EARCON_MODE_ENTER, AudioType.STANDARD)
        service.audioEngine.createTextToSpeech(label, AudioType.STANDARD)
    }

    private fun loadAndEnter(item: MenuItem.DynamicSubmenu) {
        scope.launch {
            val children = item.childrenProvider()
            if (children.size <= 1) {
                service.speakText(localizedContext.getString(R.string.menu_no_routes), AudioType.STANDARD)
            } else {
                val firstLabel = synchronized(this@AudioMenu) {
                    menuStack.addLast(MenuLevel(children, 0))
                    children[0].label
                }
                speakWithEnterEarcon(firstLabel)
            }
        }
    }

    private fun audioProfileAction(@androidx.annotation.StringRes id: Int, profile: String): MenuItem.Action {
        val label = localizedContext.getString(id)
        return MenuItem.Action(label) {
            applyAudioProfile(profile)
            service.speakText(label, AudioType.STANDARD)
        }
    }

    // ── Menu definition ───────────────────────────────────────────────────────

    private fun buildRootMenu(): List<MenuItem> = listOf(

        MenuItem.Submenu(
            label = localizedContext.getString(R.string.callouts_panel_title),
            children = listOf(
                MenuItem.Action(localizedContext.getString(R.string.directions_my_location)) {
                    service.myLocation()
                },
                MenuItem.Action(localizedContext.getString(R.string.help_orient_page_title)) {
                    service.whatsAroundMe()
                },
                MenuItem.Action(localizedContext.getString(R.string.help_explore_page_title)) {
                    service.aheadOfMe()
                },
                MenuItem.Action(localizedContext.getString(R.string.callouts_nearby_markers)) {
                    service.nearbyMarkers()
                },
                mainMenuAction(),
            )
        ),

        MenuItem.Submenu(
            label = localizedContext.getString(R.string.menu_route),
            children = listOf(
                MenuItem.Action(localizedContext.getString(R.string.menu_route_next_waypoint)) {
                    service.routeSkipNext()
                },
                MenuItem.Action(localizedContext.getString(R.string.menu_route_previous_waypoint)) {
                    service.routeSkipPrevious()
                },
                MenuItem.Action(localizedContext.getString(R.string.beacon_action_mute_beacon)) {
                    service.routeMute()
                },
                MenuItem.Action(localizedContext.getString(R.string.route_detail_action_stop_route)) {
                    service.routeStop()
                },
                mainMenuAction(),
            )
        ),

        MenuItem.DynamicSubmenu(
            label = localizedContext.getString(R.string.route_detail_action_start_route),
            childrenProvider = { loadRouteMenuItems() }
        ),

        MenuItem.DynamicSubmenu(
            label = localizedContext.getString(R.string.location_detail_action_beacon),
            childrenProvider = { loadMarkerMenuItems() }
        ),
    )

    // ── Feature implementations ───────────────────────────────────────────────

    private suspend fun loadRouteMenuItems(): List<MenuItem> =
        withContext(Dispatchers.IO) {
            val db = MarkersAndRoutesDatabase.getMarkersInstance(service)
            db.routeDao().getAllRoutes().map { route ->
                MenuItem.Action(route.name) { service.routeStart(route.routeId) }
            } + mainMenuAction()
        }

    private suspend fun loadMarkerMenuItems(): List<MenuItem> =
        withContext(Dispatchers.IO) {
            val db = MarkersAndRoutesDatabase.getMarkersInstance(service)
            db.routeDao().getAllMarkers().map { marker ->
                MenuItem.Action(marker.name) {
                    val location = LngLatAlt(marker.longitude, marker.latitude)
                    service.startBeacon(location, marker.name)
                }
            } + mainMenuAction()
        }

    /**
     */
    private fun applyAudioProfile(profileName: String) {
        when (profileName) {
            "eating" -> {}
            "shopping" -> {}
            "navigating" -> {}
            "roads_only" -> {}
            "all" -> {}
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun destroy() {
        service.menuActive = false
        scope.cancel()
    }

    companion object {
        private const val TAG = "AudioMenu"
    }
}
