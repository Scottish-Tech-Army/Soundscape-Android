package org.scottishtecharmy.soundscape.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.scottishtecharmy.soundscape.resources.*
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geoengine.formatDistanceAndDirection
import org.scottishtecharmy.soundscape.geoengine.utils.distance
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.createCheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.ComposeLocalizedStrings
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService

class RoutePlayer(
    val service: MediaControllableService,
    private val routeDao: RouteDao,
) {
    private var currentRouteData: RouteWithMarkers? = null
    private var currentMarker = -1
    private val coroutineScope = CoroutineScope(Job())
    private var locationMonitoringJob: Job? = null

    // Flow to return current route data
    private val _currentRouteFlow = MutableStateFlow(RoutePlayerState())
    var currentRouteFlow: StateFlow<RoutePlayerState> = _currentRouteFlow

    /**
     * startBeacon creates a temporary route with a single waypoint and starts playing it. This
     * means that the same UI code can be used to control the beacon as for a real route.
     * @param beaconLocation The location to place the beacon at
     * @param beaconName The name of the beacon
     */
    fun startBeacon(beaconLocation: LngLatAlt, beaconName: String) {
        currentMarker = 0

        // If the beacon start point is more than 30m away, then we can have it as a destination
        val currentLocation = service.filteredLocationFlow.value
        var beaconOnly = true
        if(currentLocation != null) {
            val distance =
                beaconLocation.createCheapRuler().distance(
                    LngLatAlt(currentLocation.longitude, currentLocation.latitude),
                    beaconLocation
                )
            if(distance > 30.0)
                beaconOnly = false
        }

        val marker = MarkerEntity(
            name = beaconName,
            longitude = beaconLocation.longitude,
            latitude = beaconLocation.latitude,
        )
        val waypoints = listOf(marker)

        currentRouteData = RouteWithMarkers(
            RouteEntity(0, beaconName, ""),
            waypoints
        )
        _currentRouteFlow.update {
            it.copy(
                routeData = currentRouteData,
                currentWaypoint = currentMarker,
                beaconOnly = true
            )
        }
        play()

        if(!beaconOnly) {
            // We want to describe how far we are and a route completion
            startMonitoringLocation()
        }
    }

    /** startRoute starts playback of a route from the database.
     * @param routeId The id of the route to play
     * @param reverse If true, play the route in reverse order (from last waypoint to first)
     */
    fun startRoute(routeId: Long, reverse: Boolean = false) {
        coroutineScope.launch {
            val route = routeDao.getRouteWithMarkers(routeId) ?: return@launch
            currentMarker = 0
            currentRouteData = if (reverse) {
                val reverseName = getString(Res.string.route_reverse_name, route.route.name)
                RouteWithMarkers(
                    RouteEntity(route.route.routeId, reverseName, route.route.description),
                    route.markers.reversed()
                )
            } else {
                route
            }
            _currentRouteFlow.update {
                it.copy(
                    routeData = currentRouteData,
                    currentWaypoint = currentMarker,
                    beaconOnly = false
                )
            }
            play()
        }
        startMonitoringLocation()
    }

    fun stopMonitoringLocation() {
        locationMonitoringJob?.cancel()
    }

    fun startMonitoringLocation() {
        locationMonitoringJob?.cancel()
        locationMonitoringJob = coroutineScope.launch {
            // Observe location updates from the service
            service.filteredLocationFlow.collect { value ->
                if (value != null) {
                    currentRouteData?.let { route ->
                        if(currentMarker < route.markers.size) {
                            val location = route.markers[currentMarker].getLngLatAlt()
                            val distanceToWaypoint = distance(
                                location.latitude,
                                location.longitude,
                                value.latitude,
                                value.longitude
                            )
                            if (distanceToWaypoint < 12.0) {
                                if ((currentMarker + 1) < route.markers.size) {
                                    // We're within 12m of the marker, move on to the next one
                                    moveToNext(false)
                                } else {
                                    // We've reached the end of the route
                                    val endOfRouteText = getString(
                                        Res.string.route_end_completed_accessibility,
                                        route.route.name
                                    )
                                    service.speakText(
                                        endOfRouteText,
                                        AudioType.STANDARD
                                    )

                                    // Stop the beacon
                                    stopRoute()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createBeaconAtWaypoint(index: Int, userInitiated: Boolean) {
        currentRouteData?.let { route ->
            if (index < route.markers.size) {
                val location = route.markers[index].getLngLatAlt()

                val currentLocation = service.filteredLocationFlow.value
                if(currentLocation != null) {
                    val distance =
                        location.createCheapRuler().distance(
                            LngLatAlt(currentLocation.longitude, currentLocation.latitude),
                            location
                        )
                    val beaconSetText =
                        if(route.markers.size > 1) {
                            kotlinx.coroutines.runBlocking { getString(
                                Res.string.behavior_scavenger_hunt_callout_next_flag,
                                route.markers[index].name,
                                formatDistanceAndDirection(distance, null, ComposeLocalizedStrings()),
                                (index + 1).toString(),
                                (route.markers.size).toString()
                            ) }
                        } else {
                            kotlinx.coroutines.runBlocking { getString(
                                Res.string.behavior_scavenger_hunt_callout_next_flag_short_route,
                                route.markers[index].name,
                                formatDistanceAndDirection(distance, null, ComposeLocalizedStrings())) }
                        }
                    if(userInitiated) service.clearTextToSpeechQueue()
                    service.speakText(
                        beaconSetText,
                        AudioType.LOCALIZED, location.latitude, location.longitude, 0.0
                    )
                }

                service.createBeacon(location, false)
            }
        }
    }

    fun stopRoute() {
        service.destroyBeacon()
        _currentRouteFlow.update { it.copy(
            routeData = null,
            currentWaypoint = 0
        )}
        currentRouteData = null
        stopMonitoringLocation()
    }

    fun play() {
        createBeaconAtWaypoint(currentMarker, true)
    }

    fun moveToNext(userInitiated: Boolean) : Boolean {
        currentRouteData?.let { route ->
            if(route.markers.size > 1) {
                if ((currentMarker + 1) < route.markers.size) {
                    currentMarker++
                    _currentRouteFlow.update { it.copy(currentWaypoint = currentMarker) }

                    createBeaconAtWaypoint(currentMarker, userInitiated)
                }
                return true
            }
        }
        return false
    }

    fun moveToPrevious(userInitiated: Boolean) : Boolean{
        currentRouteData?.let { route ->
            if(route.markers.size > 1) {
                if (currentMarker > 0) {
                    currentMarker--
                    _currentRouteFlow.update { it.copy(currentWaypoint = currentMarker) }
                    createBeaconAtWaypoint(currentMarker, userInitiated)
                    return true
                }
            }
        }
        return false
    }
    fun isPlaying(): Boolean {
        return (currentRouteData != null)
    }
    override fun toString(): String {
        currentRouteData?.let { route ->
            var state = ""
            state += "Route : ${route.route.name}\n"
            for((index, waypoint) in route.markers.withIndex()) {
                state += "  ${waypoint.name} at ${waypoint.latitude},${waypoint.longitude}"
                state += if(index == currentMarker) {
                    " <current>\n"
                } else {
                    "\n"
                }
            }

            return state
        }
        return "No current route set."
    }
}
