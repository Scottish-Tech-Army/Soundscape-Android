package org.scottishtecharmy.soundscape.services

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabase
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geoengine.formatDistanceAndDirection
import org.scottishtecharmy.soundscape.geoengine.utils.distance
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.utils.getCurrentLocale

data class RoutePlayerState(
    val routeData: RouteWithMarkers? = null,
    val currentWaypoint: Int = 0,
    val beaconOnly: Boolean = false,
    val reversePlayback: Boolean = false
)

class RoutePlayer(val service: SoundscapeService, context: Context) {
    private var currentRouteData: RouteWithMarkers? = null
    private var currentMarker = -1
    private var standaloneBeacon = true
    private var reversePlayback = false
    private val coroutineScope = CoroutineScope(Job())
    private var localizedContext: Context
    private var locationMonitoringJob: Job? = null
    init {
        val configLocale = getCurrentLocale()
        val configuration = Configuration(context.applicationContext.resources.configuration)
        configuration.setLocale(configLocale)
        localizedContext = context.applicationContext.createConfigurationContext(configuration)
    }

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
        Log.e(TAG, "startBeacon")
        currentMarker = 0

        Analytics.getInstance().logEvent("startBeacon", null)

        // If the beacon start point is more than 30m away, then we can have it as a destination
        val currentLocation = service.locationProvider.filteredLocationFlow.value
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
        Log.d(TAG, toString())

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
        val realm = MarkersAndRoutesDatabase.getMarkersInstance(localizedContext)
        val routeDao = realm.routeDao()

        Analytics.getInstance().logEvent(if (reverse) "startRouteReverse" else "startRoute", null)

        Log.e(TAG, "startRoute reverse=$reverse")
        coroutineScope.launch {
            val route = routeDao.getRouteWithMarkers(routeId) ?: return@launch
            reversePlayback = reverse
            currentMarker = if (reverse && route.markers.isNotEmpty()) {
                route.markers.size - 1
            } else {
                0
            }
            currentRouteData = route
            _currentRouteFlow.update {
                it.copy(
                    routeData = currentRouteData,
                    currentWaypoint = currentMarker,
                    beaconOnly = false,
                    reversePlayback = reversePlayback
                )
            }
            play()
            Log.d(TAG, toString())
        }
        startMonitoringLocation()
    }

    fun stopMonitoringLocation() {
        locationMonitoringJob?.cancel()
    }

    fun startMonitoringLocation() {
        Log.d(TAG, "startMonitoringLocation")
        locationMonitoringJob?.cancel()
        locationMonitoringJob = coroutineScope.launch {
            // Observe location updates from the service
            service.locationProvider.filteredLocationFlow.collect { value ->
                if (value != null) {
                    currentRouteData?.let { route ->
                        if(!standaloneBeacon) {
                            if(currentMarker in route.markers.indices) {
                                val location = route.markers[currentMarker].getLngLatAlt()
                                val distanceToWaypoint = distance(
                                    location.latitude,
                                    location.longitude,
                                    value.latitude,
                                    value.longitude
                                )
                                if (distanceToWaypoint < 12.0) {
                                    val hasNextWaypoint = if (reversePlayback) {
                                        currentMarker > 0
                                    } else {
                                        (currentMarker + 1) < route.markers.size
                                    }
                                    if (hasNextWaypoint) {
                                        // We're within 12m of the marker, move on to the next one
                                        Log.d(TAG, "Moving to next waypoint ${coroutineContext[Job]}")
                                        advanceInPlaybackDirection()
                                    } else {
                                        // We've reached the end of the route
                                        // Announce the end of the route
                                        Log.d(TAG, "End of route ${coroutineContext[Job]}")
                                        val endOfRouteText = localizedContext.getString(
                                            R.string.route_end_completed_accessibility,
                                            route.route.name
                                        )
                                        service.speakText(
                                            endOfRouteText,
                                            AudioType.STANDARD
                                        )

                                        // Stop the beacon
                                        stopRoute()
                                    }
                                } else {
                                    Log.d(TAG, "Waypoint $distanceToWaypoint away")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createBeaconAtWaypoint(index: Int) {
        currentRouteData?.let { route ->
            if (index < route.markers.size) {
                val location = route.markers[index].getLngLatAlt()
                val positionInPlayback = if (reversePlayback) {
                    route.markers.size - index
                } else {
                    index + 1
                }

                val currentLocation = service.locationProvider.filteredLocationFlow.value
                if(currentLocation != null) {
                    val distance =
                        location.createCheapRuler().distance(
                            LngLatAlt(currentLocation.longitude, currentLocation.latitude),
                            location
                        )
                    val beaconSetText =
                        if(route.markers.size > 1) {
                            localizedContext.getString(
                                R.string.behavior_scavenger_hunt_callout_next_flag,
                                route.markers[index].name,
                                formatDistanceAndDirection(distance, null, localizedContext),
                                positionInPlayback.toString(),
                                (route.markers.size).toString()
                            )
                        } else {
                            localizedContext.getString(
                                R.string.behavior_scavenger_hunt_callout_next_flag_short_route,
                                route.markers[index].name,
                                formatDistanceAndDirection(distance, null, localizedContext))
                        }

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
        reversePlayback = false
        _currentRouteFlow.update { it.copy(
            routeData = null,
            currentWaypoint = 0,
            reversePlayback = false
        )}
        currentRouteData = null
        stopMonitoringLocation()
    }

    fun play() {
        createBeaconAtWaypoint(currentMarker)
        Log.d(TAG, toString())
    }

    /**
     * Advances to the next waypoint in the current playback direction.
     * In forward mode: moves to higher index. In reverse mode: moves to lower index.
     */
    private fun advanceInPlaybackDirection(): Boolean {
        currentRouteData?.let { route ->
            if (reversePlayback) {
                if (currentMarker > 0) {
                    currentMarker--
                    _currentRouteFlow.update {
                        it.copy(currentWaypoint = currentMarker, reversePlayback = reversePlayback)
                    }
                    createBeaconAtWaypoint(currentMarker)
                    return true
                }
            } else {
                if ((currentMarker + 1) < route.markers.size) {
                    currentMarker++
                    _currentRouteFlow.update { it.copy(currentWaypoint = currentMarker) }
                    createBeaconAtWaypoint(currentMarker)
                    return true
                }
            }
        }
        Log.d(TAG, toString())
        return false
    }

    fun moveToNext(): Boolean {
        return if (reversePlayback) {
            moveToPreviousIndex()
        } else {
            moveToNextIndex()
        }
    }

    fun moveToPrevious(): Boolean {
        return if (reversePlayback) {
            moveToNextIndex()
        } else {
            moveToPreviousIndex()
        }
    }

    private fun moveToNextIndex(): Boolean {
        currentRouteData?.let { route ->
            if (route.markers.size > 1 && (currentMarker + 1) < route.markers.size) {
                currentMarker++
                _currentRouteFlow.update { it.copy(currentWaypoint = currentMarker) }
                createBeaconAtWaypoint(currentMarker)
                return true
            }
        }
        Log.d(TAG, toString())
        return false
    }

    private fun moveToPreviousIndex(): Boolean {
        currentRouteData?.let { route ->
            if (route.markers.size > 1 && currentMarker > 0) {
                currentMarker--
                _currentRouteFlow.update { it.copy(currentWaypoint = currentMarker) }
                createBeaconAtWaypoint(currentMarker)
                return true
            }
        }
        Log.d(TAG, toString())
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


    companion object {
        private const val TAG = "RoutePlayer"
    }
}