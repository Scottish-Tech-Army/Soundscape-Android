package org.scottishtecharmy.soundscape.geoengine.callouts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.audio.AudioType
import org.scottishtecharmy.soundscape.audio.Earcons
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.PositionedString
import org.scottishtecharmy.soundscape.geoengine.TreeId
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.LastStationTracker
import org.scottishtecharmy.soundscape.geoengine.NotableVehicleEventTracker
import org.scottishtecharmy.soundscape.geoengine.describeReverseGeocode
import org.scottishtecharmy.soundscape.geoengine.filters.CalloutHistory
import org.scottishtecharmy.soundscape.geoengine.filters.LocationUpdateFilter
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.formatDistanceAndDirection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.CountryBoundaries
import org.scottishtecharmy.soundscape.geoengine.utils.DrivingSide
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.normalizeHeading
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import kotlin.math.roundToInt

class AutoCallout(
    private val localized: LocalizedStrings?,
    private val preferences: PreferencesProvider?
) {
    private val destinationFilter = LocationUpdateFilter(60000, 10.0)
    private val locationFilter = LocationUpdateFilter(10000, 50.0)
    private val poiFilter = LocationUpdateFilter(5000, 5.0)
    private val intersectionFilter = LocationUpdateFilter(5000, 5.0)
    private val intersectionCalloutHistory = CalloutHistory(30000)
    private val poiCalloutHistory = CalloutHistory()
    private val roadSenseCalloutHistory = CalloutHistory()
    private val vehicleLandmarkFilter = LocationUpdateFilter(10000, 50.0)
    private val vehicleLandmarkCalloutHistory = CalloutHistory()
    private val vehicleTransitStopCalloutHistory = CalloutHistory()
    private var lastTransitStopSweepLocation: LngLatAlt? = null
    private val vehicleCrossingCalloutHistory = CalloutHistory()
    private var lastCrossingSweepLocation: LngLatAlt? = null
    private val lastStationTracker = LastStationTracker()
    private val notableVehicleEventTracker = NotableVehicleEventTracker()
    private var lastTrainTimestampMs: Long? = null
    private var lastVehicleTimestampMs: Long? = null

    // How close a large POI (see TreeId.LANDMARK_POIS) needs to be to be called out as "passed"
    // while travelling by car/bus - bigger than a pedestrian trigger range since landmarks are
    // large and travel is fast.
    private val vehicleLandmarkPassingDistanceMetres = 150.0

    /**
     * How long after last confidently detecting a train (see UserGeometry.probablyOnTrain) we
     * keep suppressing pedestrian-style intersection callouts. Real recorded journeys show
     * station dwell stops of ~12-19 seconds, so this gives generous margin for a real stop
     * without permanently blocking pedestrian callouts once someone has actually got off.
     */
    private val trainStickyWindowMs = 60_000L

    private fun recentlyOnTrain(userGeometry: UserGeometry): Boolean {
        val last = lastTrainTimestampMs ?: return false
        return (userGeometry.timestampMilliseconds - last) < trainStickyWindowMs
    }

    /**
     * How long after last being in any vehicle (car/bus/train - see UserGeometry.inVehicle) we
     * keep suppressing pedestrian-style callouts. UserGeometry.inVehicle() is a raw instantaneous
     * speed check with no hysteresis, so without this a car/bus briefly stopped at a red light or
     * in traffic would immediately expose pedestrian-style intersection/POI callouts, then flip
     * back a moment later as it moves off - this smooths that out, same as the train-specific
     * window above but for any vehicle stop, not just a train dwell stop.
     */
    private val vehicleStickyWindowMs = 60_000L

    private fun recentlyInVehicle(userGeometry: UserGeometry): Boolean {
        val last = lastVehicleTimestampMs ?: return false
        return (userGeometry.timestampMilliseconds - last) < vehicleStickyWindowMs
    }

    private fun buildCalloutForDestination(userGeometry: UserGeometry): TrackedCallout? {

        // Check that we have a destination
        val beacon = userGeometry.currentBeacon ?: return null

        // Check that our location/time has changed enough to generate this callout
        if (!destinationFilter.shouldUpdate(userGeometry)) {
            return null
        }

        val distance = userGeometry.ruler.distance(userGeometry.location, beacon)
        val distanceString = formatDistanceAndDirection(distance, null, localized)
        var text = localized?.get(StringKey.CalloutsAudioBeacon) ?: "Distance to the audio beacon"
        text += " $distanceString"

        return TrackedCallout(
            userGeometry = userGeometry,
            trackedText = "",
            location = beacon,
            isPoint = true,
            isGeneric = true,
            filter = false,
            positionedStrings = List(1) {
                PositionedString(
                    text = text,
                    location = beacon,
                    type = AudioType.LOCALIZED
                )
            }
        )
    }

    private fun buildCalloutForRoadSense(
        userGeometry: UserGeometry,
        gridState: GridState,
        settlementState: GridState
    ): TrackedCallout? {

        // Recorded on every call (ahead of the throttled checks below) so the sticky windows
        // above track actual vehicle presence as closely as the location updates allow, rather
        // than only being refreshed whenever this callout's own throttle happens to fire.
        if (userGeometry.inVehicle()) {
            lastVehicleTimestampMs = userGeometry.timestampMilliseconds
            if (userGeometry.probablyOnTrain()) {
                lastTrainTimestampMs = userGeometry.timestampMilliseconds
            }
        }

        // Check that our location/time has changed enough to generate this callout
        if (!locationFilter.shouldUpdate(userGeometry)) {
            return null
        }

        // Trim history based on location and current time
        roadSenseCalloutHistory.trim(userGeometry)

        // Check that we're in a vehicle
        if (!userGeometry.inVehicle()) {
            return null
        }

        // Update time/location filter for our new position
        locationFilter.update(userGeometry)

        // Reverse geocode the current location (this is the iOS name for the function)
        val result = describeReverseGeocode(
            userGeometry, gridState, settlementState, localized, lastStationTracker,
            notableVehicleEventTracker
        )
        if (result != null) {
            val callout = TrackedCallout(
                userGeometry,
                trackedText = result.text,
                dedupText = result.dedupText,
                location = result.location ?: userGeometry.location,
                positionedStrings = listOf(result),
                isPoint = false,
                isGeneric = false,
                calloutHistory = roadSenseCalloutHistory
            )

            if (roadSenseCalloutHistory.find(callout)) {
                //println("Discard ${callout.trackedText}")
                // Filter out
                return null
            }

            // Check that the geocode has changed before returning a callout describing it
            return callout
        }

        return null
    }

    /**
     * Announces large points of interest (see TreeId.LANDMARK_POIS - stadiums, parks, hospitals,
     * malls etc.) as they're passed while travelling by car/bus. This is layered on top of, not
     * instead of, buildCalloutForRoadSense's periodic road/settlement description - see also the
     * major/minor junction selection in travellingReverseGeocodeName, which shares
     * notableVehicleEventTracker with this so a quiet stretch of neither can fall back to
     * mentioning a minor junction.
     */
    private fun buildCalloutForVehicleLandmark(
        userGeometry: UserGeometry,
        gridState: GridState
    ): TrackedCallout? {
        if (!vehicleLandmarkFilter.shouldUpdate(userGeometry)) {
            return null
        }

        vehicleLandmarkCalloutHistory.trim(userGeometry)

        // Also suppress shortly after losing rail lock (not just while it's held) - real
        // recordings show probablyOnTrain() can flicker false for an instant mid-journey (a brief
        // map-match gap) while still genuinely on the train, which would otherwise cause a
        // trackside POI to get announced as if passed by car/bus.
        if (!userGeometry.inVehicle() || userGeometry.probablyOnTrain() || recentlyOnTrain(userGeometry)) {
            return null
        }

        vehicleLandmarkFilter.update(userGeometry)

        val nearestLandmark = gridState.getFeatureTree(TreeId.LANDMARK_POIS).getNearestFeature(
            userGeometry.location, gridState.ruler, vehicleLandmarkPassingDistanceMetres
        ) as? MvtFeature ?: return null

        val name = getTextForFeature(localized, nearestLandmark)
        if (name.generic || name.text.isEmpty()) {
            // Not worth calling out a large POI with no real name.
            return null
        }

        val nearestPoint = getDistanceToFeature(userGeometry.location, nearestLandmark, userGeometry.ruler)
        val callout = TrackedCallout(
            userGeometry,
            trackedText = name.text,
            location = nearestPoint.point,
            positionedStrings = listOf(
                PositionedString(
                    text = name.text,
                    location = nearestPoint.point,
                    type = AudioType.LOCALIZED
                )
            ),
            isPoint = nearestLandmark.geometry.type == "Point",
            isGeneric = false,
        )

        if (vehicleLandmarkCalloutHistory.find(callout)) {
            return null
        }

        // Added eagerly here (rather than via the callout's calloutHistory field, which
        // updateLocation's generic speak-path would only process if this callout ends up being
        // returned standalone) since this callout may instead be merged into roadSenseCallout's
        // positionedStrings when both fire on the same update - see updateLocation.
        vehicleLandmarkCalloutHistory.add(callout)
        notableVehicleEventTracker.recordEvent(userGeometry.timestampMilliseconds)
        return callout
    }

    /**
     * Announces a bus/tram/train stop as it's passed while travelling by car/bus/train. Checking
     * only the current location against a fixed radius (as buildCalloutForRoadSense/
     * describeReverseGeocode used to) misses most stops at driving speed: this callout only runs
     * every ~10s/50m, but a stop's ~20m detection radius is crossed in a couple of seconds, so the
     * odds of a periodic check landing inside that narrow window are poor. Instead this sweeps the
     * path travelled since the last location update (FeatureTree.getNearbyLine) and checks the
     * whole segment against the stop radius, so a stop can't be skipped over between updates
     * regardless of speed.
     */
    private fun buildCalloutForVehicleTransitStop(
        userGeometry: UserGeometry,
        gridState: GridState
    ): TrackedCallout? {
        // Also covers a brief stop (red light, station dwell) via recentlyInVehicle, so the sweep
        // anchor isn't lost (and a stop right at that moment isn't missed) - only reset once
        // genuinely no longer in a vehicle, e.g. actually got out and started walking.
        if (!userGeometry.inVehicle() && !recentlyInVehicle(userGeometry)) {
            lastTransitStopSweepLocation = null
            return null
        }

        val previousLocation = lastTransitStopSweepLocation
        lastTransitStopSweepLocation = userGeometry.location
        if (previousLocation == null) {
            // Nothing to sweep yet - this is the first update since entering vehicle mode.
            return null
        }

        vehicleTransitStopCalloutHistory.trim(userGeometry)

        val sweep = LineString(previousLocation, userGeometry.location)
        val nearbyStops = gridState.getFeatureTree(TreeId.TRANSIT_STOPS)
            .getNearbyLine(sweep, 20.0, gridState.ruler)

        // A stop on the far side of the road serves the opposite direction of travel and isn't
        // relevant to us - both directions' stops are easily within the 20m sweep radius of an
        // ordinary two-way street, so distance alone can't tell them apart. The near-side kerb
        // (stops serving our direction) is to the left of the direction of travel in a left-hand
        // traffic country, or the right in a right-hand traffic one - see CountryBoundaries. If
        // the country can't be determined (e.g. no bundled boundary covers this location), don't
        // filter at all rather than guess.
        val drivingSide = CountryBoundaries.drivingSide(userGeometry.location)
        val travelBearing = gridState.ruler.bearing(previousLocation, userGeometry.location)
        val candidate = nearbyStops.features
            .mapNotNull { feature ->
                val mvtFeature = feature as? MvtFeature ?: return@mapNotNull null
                val text = getTextForFeature(localized, mvtFeature)
                if (text.generic) return@mapNotNull null
                if (drivingSide != null) {
                    val stopLocation = (mvtFeature.geometry as? Point)?.coordinates
                        ?: return@mapNotNull null
                    val stopBearing = gridState.ruler.bearing(userGeometry.location, stopLocation)
                    val relativeAngle = normalizeHeading((stopBearing - travelBearing).roundToInt())
                    // relativeAngle in 1..179 is to the right of travel, 180..359 to the left.
                    val isFarSide = if (drivingSide == DrivingSide.LEFT) {
                        relativeAngle in 1..179
                    } else {
                        relativeAngle in 180..359
                    }
                    if (isFarSide) return@mapNotNull null
                }
                Pair(mvtFeature, text)
            }
            .minByOrNull {
                getDistanceToFeature(userGeometry.location, it.first, userGeometry.ruler).distance
            } ?: return null

        val (stopFeature, stopText) = candidate
        val nearestPoint = getDistanceToFeature(userGeometry.location, stopFeature, userGeometry.ruler)
        val callout = TrackedCallout(
            userGeometry,
            trackedText = stopText.text,
            location = nearestPoint.point,
            positionedStrings = listOf(
                PositionedString(
                    text = localized?.get(StringKey.DirectionsNearName, stopText.text)
                        ?: "Near ${stopText.text}",
                    location = nearestPoint.point,
                    type = AudioType.LOCALIZED
                )
            ),
            isPoint = stopFeature.geometry.type == "Point",
            isGeneric = false,
        )

        if (vehicleTransitStopCalloutHistory.find(callout)) {
            return null
        }

        // Added eagerly - see the equivalent comment in buildCalloutForVehicleLandmark.
        vehicleTransitStopCalloutHistory.add(callout)
        notableVehicleEventTracker.recordEvent(userGeometry.timestampMilliseconds)
        return callout
    }

    /**
     * Announces a river/canal or railway crossing (see TreeId.WATER_AND_RAIL_CROSSINGS) as it's
     * passed while travelling by car/bus - these are major navigation points ("Crossing Allander
     * Water", "Crossing the railway") worth calling out on their own, not just as part of a "via a
     * bridge" road name. Sweeps the path travelled since the last location update rather than
     * checking only the current point - see buildCalloutForVehicleTransitStop for why that matters
     * at driving speed. Unlike a transit stop, a crossing has no "far side" to filter out - the
     * road crosses the water/railway regardless of direction of travel.
     */
    private fun buildCalloutForVehicleCrossing(
        userGeometry: UserGeometry,
        gridState: GridState
    ): TrackedCallout? {
        if (!userGeometry.inVehicle() && !recentlyInVehicle(userGeometry)) {
            lastCrossingSweepLocation = null
            return null
        }

        val previousLocation = lastCrossingSweepLocation
        lastCrossingSweepLocation = userGeometry.location
        if (previousLocation == null) {
            // Nothing to sweep yet - this is the first update since entering vehicle mode.
            return null
        }

        vehicleCrossingCalloutHistory.trim(userGeometry)

        val sweep = LineString(previousLocation, userGeometry.location)
        val nearbyCrossings = gridState.getFeatureTree(TreeId.WATER_AND_RAIL_CROSSINGS)
            .getNearbyLine(sweep, 20.0, gridState.ruler)

        // An unnamed waterway crossing isn't worth announcing - there's nothing useful to say
        // beyond "Crossing" nothing - but an unnamed railway still is, since "Crossing the
        // railway" is meaningful on its own even without a line name.
        val crossingFeature = nearbyCrossings.features
            .filterIsInstance<MvtFeature>()
            .filter { !it.name.isNullOrEmpty() || it.featureType == "railway" }
            .minByOrNull {
                getDistanceToFeature(userGeometry.location, it, userGeometry.ruler).distance
            } ?: return null

        val name = crossingFeature.name
        // For a railway (unlike a waterway), the road's own brunnel tells us whether we're going
        // over it (bridge) or under it (tunnel) - worth distinguishing, since "going under" reads
        // oddly for a bridge and vice versa.
        val goingUnder = (crossingFeature.featureType == "railway") &&
            (crossingFeature.properties?.get("brunnel") == "tunnel")
        val text = if (name != null) {
            if (goingUnder) {
                localized?.get(StringKey.DirectionsGoingUnderRailway, name) ?: "Going under $name"
            } else {
                localized?.get(StringKey.DirectionsCrossingWaterway, name) ?: "Crossing $name"
            }
        } else if (goingUnder) {
            localized?.get(StringKey.DirectionsGoingUnderRailwayGeneric) ?: "Going under the railway"
        } else {
            localized?.get(StringKey.DirectionsCrossingRailwayGeneric) ?: "Crossing the railway"
        }
        val nearestPoint = getDistanceToFeature(userGeometry.location, crossingFeature, userGeometry.ruler)
        val callout = TrackedCallout(
            userGeometry,
            trackedText = name ?: "railway",
            location = nearestPoint.point,
            positionedStrings = listOf(
                PositionedString(
                    text = text,
                    location = nearestPoint.point,
                    type = AudioType.LOCALIZED
                )
            ),
            isPoint = true,
            isGeneric = false,
        )

        if (vehicleCrossingCalloutHistory.find(callout)) {
            return null
        }

        // Added eagerly - see the equivalent comment in buildCalloutForVehicleLandmark.
        vehicleCrossingCalloutHistory.add(callout)
        notableVehicleEventTracker.recordEvent(userGeometry.timestampMilliseconds)
        return callout
    }

    fun buildCalloutForIntersections(
        userGeometry: UserGeometry,
        gridState: GridState
    ): TrackedCallout? {

        // We rely heavily on having map matched our GPS location to a nearby way. If we're not in
        // StreetPreview mode and we don't have that Way, then skip intersection callouts until we
        // do.
        if ((userGeometry.mapMatchedWay == null) && !userGeometry.inStreetPreview) {
            return null
        }

        // Check that our location/time has changed enough to generate this callout
        if (!intersectionFilter.shouldUpdate(userGeometry)) {
            return null
        }

        // Check that we're not in a vehicle - and not recently in one, so a brief stop (a red
        // light, traffic, a station dwell stop) doesn't fall through to pedestrian-style
        // intersection callouts, which read oddly for someone still sitting in/on a car, bus or
        // train rather than out walking around.
        if (userGeometry.inVehicle() || recentlyInVehicle(userGeometry)) {
            return null
        }

        // Trim callout history based on our location and current time
        intersectionCalloutHistory.trim(userGeometry)

        val roadsDescription = getRoadsDescriptionFromFov(
            gridState,
            userGeometry,
            localized
        )

        // Don't describe the road we're on if there's an intersection
        return addIntersectionCalloutFromDescription(
            roadsDescription,
            localized,
            intersectionCalloutHistory,
            gridState
        )
    }

    private fun buildCalloutForNearbyPOI(
        userGeometry: UserGeometry,
        gridState: GridState
    ): TrackedCallout? {
        // This FOV/trigger-range based POI search is tuned for walking pace - vehicles get their
        // own equivalent, buildCalloutForVehicleLandmark. Also suppressed shortly after being in
        // a vehicle, for the same reason as buildCalloutForIntersections above.
        if (userGeometry.inVehicle() || recentlyInVehicle(userGeometry)) {
            return null
        }

        if (!poiFilter.shouldUpdateActivity(userGeometry)) {
            return null
        }

        // Trim history based on location and current time
        poiCalloutHistory.trim(userGeometry)

        // Get nearby markers that are ahead of us in our field of view
        val triangle = getFovTriangle(userGeometry)
        val markers = gridState.markerTree?.getNearestCollectionWithinTriangle(
            triangle,
            5,
            userGeometry.ruler
        )

        // Get a list of the 10 nearest POI that are within search range, adding in the markers
        val pois = gridState.getFeatureTree(TreeId.SELECTED_SUPER_CATEGORIES).getNearestCollection(
            userGeometry.location,
            userGeometry.getSearchDistance(),
            10,
            userGeometry.ruler,
            markers
        )

        val uniquelyNamedPOIs = mutableMapOf<String, Feature>()
        pois.features.filter { feature ->

            val name = (feature as MvtFeature).getText(localized)
            val nearestPoint =
                getDistanceToFeature(userGeometry.location, feature, userGeometry.ruler)

            if (name.text.isEmpty())
                return@filter true

            val callout = TrackedCallout(
                userGeometry,
                name.text,
                nearestPoint.point,
                positionedStrings = emptyList(),
                feature.geometry.type == "Point",
                name.generic
            )
            val currentBeacon = userGeometry.currentBeacon
            if (currentBeacon != null) {
                // If the feature is within 1m of the current beacon, don't call it out
                if (getDistanceToFeature(
                        currentBeacon,
                        feature,
                        userGeometry.ruler
                    ).distance < 1.0
                ) {
                    // We do want to add it to the POI history though so that when it's no longer
                    // the currentBeacon it doesn't immediately get called out.
                    if (!poiCalloutHistory.find(callout))
                        poiCalloutHistory.add(callout)

                    return@filter true
                }
            }

            if (feature.superCategory == SuperCategoryId.UNCATEGORIZED) {
                true
            } else {
                if (nearestPoint.distance > userGeometry.getTriggerRange(feature.superCategory)) {
                    // The POI is farther away than the category allows
                    true
                } else {
                    // Check the history and if the POI has been called out recently then we skip it
                    if (poiCalloutHistory.find(callout)) {
                        //println("Discard ${callout.trackedText}")
                        // Filter out
                        true
                    } else {
                        if (!uniquelyNamedPOIs.containsKey(name.text)) {
                            // Don't filter out
                            uniquelyNamedPOIs[name.text] = feature
                            val earcon = when (feature.superCategory) {
                                SuperCategoryId.INFORMATION -> Earcons.INFORMATION_ALERT
                                SuperCategoryId.SAFETY -> Earcons.SENSE_SAFETY
                                SuperCategoryId.MOBILITY -> Earcons.SENSE_MOBILITY
                                else -> Earcons.SENSE_POI
                            }
                            if (nearestPoint.distance == 0.0) {
                                callout.positionedStrings = List(1) {
                                    PositionedString(
                                        text = localized?.get(StringKey.DirectionsAtPoi, name.text)
                                            ?: "At ${name.text}",
                                        earcon = earcon,
                                        type = AudioType.STANDARD
                                    )
                                }
                            } else {
                                callout.positionedStrings = List(1) {
                                    PositionedString(
                                        text = name.text,
                                        location = nearestPoint.point,
                                        earcon = earcon,
                                        type = AudioType.LOCALIZED,
                                        addDistanceAndHeading = preferences?.getBoolean(
                                            PreferenceKeys.POSITION_INCLUDES_HEADING_AND_DISTANCE,
                                            false
                                        ) ?: false
                                    )
                                }
                            }
                            poiCalloutHistory.add(callout)
                            return callout
                        } else {
                            true
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * updateLocation is called whenever the current location changes. It works through the auto
     * callout logic to determine which (if any) callouts need to be made. This is based on the iOS
     * app logic.
     * @param userGeometry The new state of the user location/speed etc.
     * @param gridState The current state of the tile data
     * @return A list of PositionedString callouts to be spoken
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun updateLocation(
        userGeometry: UserGeometry,
        gridState: GridState,
        settlementGrid: GridState
    ): TrackedCallout? {

        // Run the code within the treeContext to protect it from changes to the trees whilst it's
        // running.
        return runBlocking {
            withContext(gridState.treeContext) {
                var trackedCallout: TrackedCallout? = null

                val destinationCallout = buildCalloutForDestination(userGeometry)
                if (destinationCallout != null) {
                    // Update the destination filter if we're outputting it
                    destinationCallout.locationFilter = destinationFilter
                    trackedCallout = destinationCallout
                } else if (preferences?.getBoolean(PreferenceKeys.ALLOW_CALLOUTS, true) != false) {
                    // buildCalloutForRoadSense builds a callout for travel that's faster than
                    // walking
                    val roadSenseCallout =
                        buildCalloutForRoadSense(userGeometry, gridState, settlementGrid)
                    // Large POIs and transit stops passed while driving/riding are announced
                    // independently of, and potentially alongside, the road/settlement
                    // description above.
                    val vehicleLandmarkCallout =
                        buildCalloutForVehicleLandmark(userGeometry, gridState)
                    val vehicleTransitStopCallout =
                        buildCalloutForVehicleTransitStop(userGeometry, gridState)
                    val vehicleWaterwayCrossingCallout =
                        buildCalloutForVehicleCrossing(userGeometry, gridState)
                    val vehicleCallouts = listOfNotNull(
                        roadSenseCallout, vehicleLandmarkCallout, vehicleTransitStopCallout,
                        vehicleWaterwayCrossingCallout
                    )
                    if (vehicleCallouts.isNotEmpty()) {
                        val primary = vehicleCallouts.first()
                        primary.positionedStrings +=
                            vehicleCallouts.drop(1).flatMap { it.positionedStrings }
                        trackedCallout = primary
                    } else {
                        val intersectionCallout =
                            buildCalloutForIntersections(userGeometry, gridState)
                        if (intersectionCallout != null) {
                            intersectionCallout.locationFilter = intersectionFilter
                            trackedCallout = intersectionCallout
                        }
                        if ((intersectionCallout == null) || userGeometry.inStreetPreview) {
                            // Get normal callouts for nearby POIs, for the destination, and for beacons
                            val poiCallout = buildCalloutForNearbyPOI(userGeometry, gridState)

                            // Update time/location filter for our new position
                            if (poiCallout != null) {
                                poiCallout.locationFilter = poiFilter
                                if (userGeometry.inStreetPreview) {
                                    // In Street Preview we want to add the call outs on to any intersection that
                                    // is being called out.
                                    if (trackedCallout != null) {
                                        trackedCallout.positionedStrings += poiCallout.positionedStrings
                                    } else
                                        trackedCallout = poiCallout
                                } else
                                    trackedCallout = poiCallout
                            }
                        }
                    }
                }
                trackedCallout
            }
        }
    }
}
