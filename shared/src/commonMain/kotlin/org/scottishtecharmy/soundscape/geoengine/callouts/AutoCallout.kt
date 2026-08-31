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
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.CountryBoundaries
import org.scottishtecharmy.soundscape.geoengine.utils.DrivingSide
import org.scottishtecharmy.soundscape.geoengine.utils.PoiRankStrategy
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.normalizeHeading
import org.scottishtecharmy.soundscape.geoengine.utils.orderPoisForSpeech
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LineString
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import kotlin.math.roundToInt

class AutoCallout(
    private val localized: LocalizedStrings?,
    private val preferences: PreferencesProvider?,
    /**
     * Which of the [PoiRankStrategy] prototypes to use when choosing between nearby POIs. Read
     * through a lambda on each callout rather than captured once, so that flipping the debug
     * setting takes effect on the next location update rather than needing a restart.
     */
    private val poiStrategy: () -> PoiRankStrategy = {
        PoiRankStrategy.fromPreference(
            preferences?.getString(
                PreferenceKeys.POI_RANK_STRATEGY,
                PreferenceDefaults.POI_RANK_STRATEGY
            )
        )
    }
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
    // null means "no baseline yet" (just entered this travel mode), not "currently unmatched" -
    // losing map-match briefly (mapMatchedWay == null) deliberately leaves this alone rather than
    // resetting it, so a transient map-match gap right as the user reaches a bridge doesn't
    // suppress the callout. See buildCalloutForVehicleCrossing/buildCalloutForWalkingCrossing.
    private var lastVehicleCrossingWayOsmId: Long? = null
    private var lastWalkingCrossingWayOsmId: Long? = null
    // Crossings already announced via the proximity trigger (see crossingToAnnounce). Shared
    // between the vehicle and walking builders so that flipping travel mode partway across a
    // structure can't announce the same crossing twice. CalloutHistory isn't usable here: its
    // trim() hardcodes a 50m radius, which at motorway speed is inside the trigger radius, so an
    // entry would be dropped and re-armed while still approaching the same crossing.
    private val announcedCrossings = mutableListOf<AnnouncedCrossing>()
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
        val distanceString =
            formatDistanceAndDirection(distance, null, localized, speed = userGeometry.speed)
        val text = localized?.get(StringKey.CalloutsAudioBeaconDistance, distanceString)
            ?: "Distance to beacon $distanceString"
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
                extraDedupText = result.extraDedupText,
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

        val name = nearestLandmark.getText(localized)
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
        gridState: GridState,
        settlementGrid: GridState
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
                val text = mvtFeature.getText(localized)
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
        val calloutText = if (stopFeature.name == null) {
            enrichUnnamedTransitStopText(stopText.text, nearestPoint.point, gridState, settlementGrid)
        } else {
            stopText.text
        }
        val callout = TrackedCallout(
            userGeometry,
            trackedText = calloutText,
            location = nearestPoint.point,
            positionedStrings = listOf(
                PositionedString(
                    text = localized?.get(StringKey.DirectionsNearName, calloutText)
                        ?: "Near $calloutText",
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
     * An unnamed transit stop's text is just its generic class ("Bus Stop", "Tram Stop"...) -
     * indistinguishable from every other unnamed stop along a route while driving past dozens of
     * them at speed. Unlike walking mode, where the stop itself is the destination and needs no
     * further context, this adds whatever's available: a small nearby settlement (hamlet/village
     * only - a town/city is usually already obvious from the surrounding road-sense callouts, so
     * isn't repeated here), or failing that a notable nearby landmark.
     */
    private fun enrichUnnamedTransitStopText(
        genericText: String,
        location: LngLatAlt,
        gridState: GridState,
        settlementGrid: GridState
    ): String {
        val settlement = (
            settlementGrid.getFeatureTree(TreeId.SETTLEMENT_HAMLET)
                .getNearestFeature(location, settlementGrid.ruler, 1000.0) as? MvtFeature
            ) ?: (
            settlementGrid.getFeatureTree(TreeId.SETTLEMENT_VILLAGE)
                .getNearestFeature(location, settlementGrid.ruler, 2000.0) as? MvtFeature
            )
        settlement?.name?.let { settlementName ->
            return localized?.get(StringKey.DirectionsTransitStopNearSettlement, genericText, settlementName)
                ?: "$genericText, $settlementName"
        }

        val landmark = gridState.getFeatureTree(TreeId.LANDMARK_POIS)
            .getNearestFeature(location, gridState.ruler, 300.0) as? MvtFeature
        landmark?.name?.let { landmarkName ->
            return localized?.get(StringKey.DirectionsTransitStopNearPoi, genericText, landmarkName)
                ?: "$genericText near $landmarkName"
        }

        return genericText
    }

    // How far from a bridge's current location to look for a named water polygon (see
    // TreeId.NAMED_WATER_POLYGONS) that isn't literally containing it - digitisation of the
    // bridge deck and the water polygon's coastline are independent, so they don't always overlap
    // exactly, especially where a coastline is heavily simplified at max zoom.
    private val waterCrossingSearchDistanceMetres = 50.0

    /**
     * Announces a river/canal or railway crossing while travelling by car/bus - these are major
     * navigation points ("Crossing Allander Water", "Crossing the railway") worth calling out on
     * their own, not just as part of a "via a bridge" road name. Fires as a simple edge: once when
     * userGeometry.mapMatchedWay's osmId changes to a Way carrying crossing properties (see
     * extractCrossings in MvtToGeoJson.kt, which computes and attaches these directly onto the
     * crossing Way at tile-parse time - no runtime search needed). Since an OSM way can be split
     * into several Way pieces that all share the same osmId (see WayGenerator), moving between
     * pieces of the same bridge/tunnel never re-fires; leaving and later returning to the same
     * crossing does re-fire, which is the desired behaviour.
     */
    private fun buildCalloutForVehicleCrossing(userGeometry: UserGeometry, gridState: GridState): TrackedCallout? {
        if (!userGeometry.inVehicle() && !recentlyInVehicle(userGeometry)) {
            lastVehicleCrossingWayOsmId = null
            return null
        }

        val matchedWay = userGeometry.mapMatchedWay ?: return null

        val previousOsmId = lastVehicleCrossingWayOsmId
        lastVehicleCrossingWayOsmId = matchedWay.osmId

        val crossing = crossingToAnnounce(userGeometry, gridState, previousOsmId) ?: return null
        val text = crossingCalloutText(crossing)
        val callout = TrackedCallout(
            userGeometry,
            trackedText = crossing.name ?: "railway",
            location = userGeometry.location,
            positionedStrings = listOf(
                PositionedString(
                    text = text,
                    location = userGeometry.location,
                    type = AudioType.STANDARD
                )
            ),
            isPoint = true,
            isGeneric = false,
        )

        notableVehicleEventTracker.recordEvent(userGeometry.timestampMilliseconds)
        return callout
    }

    /**
     * Announces the roads a train passes over and under. GridState.attachRailwayCrossings already
     * records, on every road Way that crosses a railway, which railway it crosses and exactly
     * where - so riding a railway, the same data can simply be read the other way round: find the
     * roads whose recorded crossing is with *this* line, and name them as they go by.
     *
     * The over/under sense inverts, since the stored position describes the road user's
     * relationship to the railway. A road recorded as going "over" the line is a bridge the train
     * passes beneath, and vice versa.
     *
     * Only grade-separated crossings appear, because that's all attachRailwayCrossings records - a
     * level crossing has no brunnel on either side and is deliberately left to the explicit
     * railway=level_crossing point. Unnamed roads are skipped: "Crossing" an unnamed track isn't
     * worth saying, the same reasoning as for an unnamed waterway in wayCrossingInfo.
     */
    private fun buildCalloutForTrainCrossing(userGeometry: UserGeometry, gridState: GridState): TrackedCallout? {
        if (!userGeometry.probablyOnTrain()) return null
        val railwayName = userGeometry.mapMatchedRailway?.name ?: return null

        announcedCrossings.removeAll {
            ((userGeometry.timestampMilliseconds - it.timestampMilliseconds) >
                crossingForgetTimeMilliseconds) ||
                (gridState.ruler.distance(userGeometry.location, it.location) >
                    crossingForgetDistanceMetres)
        }

        val radius = (userGeometry.speed * crossingTriggerLeadSeconds)
            .coerceIn(crossingTriggerMinimumRadiusMetres, crossingTriggerMaximumRadiusMetres)

        // TreeId.ROADS rather than ROADS_AND_PATHS: footways, pavements, cycleways and bridleways
        // are excluded from it (see WayGenerator), which is both what a rail passenger wants and a
        // good deal less to sift through. The Way objects are shared between the two collections,
        // so the crossing properties attachRailwayCrossings wrote are on these same instances.
        //
        // Searched at the trigger radius, not wider: the crossing point lies on the road's own
        // geometry, so any road whose crossing is close enough to announce is itself within that
        // radius. Anything found beyond it would only be discarded below.
        val nearbyRoads = gridState.getFeatureTree(TreeId.ROADS).getNearbyCollection(
            userGeometry.location, radius, gridState.ruler
        ).features.filterIsInstance<Way>()

        for (road in nearbyRoads) {
            if (road.properties?.get("crossing_type") != "railway") continue
            if (road.properties?.get("crossing_name") != railwayName) continue
            val latitude = road.properties?.get("crossing_latitude") as? Double ?: continue
            val longitude = road.properties?.get("crossing_longitude") as? Double ?: continue
            val point = LngLatAlt(longitude, latitude)
            if (gridState.ruler.distance(userGeometry.location, point) > radius) continue

            // Only genuinely named roads. Way.getName confects a name for anything unnamed, which
            // from a train reads as noise rather than a landmark - "Crossing Service that joins
            // Lennox Park and Crossveggate" tells a passenger nothing.
            if ((road.name == null) && (road.ref == null)) continue
            val roadName = road.getName(null, gridState, localized, true)
            if (roadName.isEmpty()) continue

            // Keyed on the name rather than the osmId, so a dual carriageway carried on two
            // separate bridge decks is announced once rather than twice. Genuinely crossing the
            // same road again later in the journey still re-announces, once the earlier entry has
            // aged or fallen far enough behind to be pruned above.
            val key = "road|$roadName"
            if (announcedCrossings.any { it.key == key }) continue

            // Inverted: a road recorded as being over the railway is one the train goes under.
            val goingUnder = road.properties?.get("crossing_position") == "over"
            val text = if (goingUnder) {
                localized?.get(StringKey.DirectionsGoingUnderRailway, roadName)
                    ?: "Going under $roadName"
            } else {
                localized?.get(StringKey.DirectionsCrossingWaterway, roadName)
                    ?: "Crossing $roadName"
            }

            announcedCrossings.add(
                AnnouncedCrossing(key, point, userGeometry.timestampMilliseconds)
            )
            return TrackedCallout(
                userGeometry,
                trackedText = roadName,
                location = userGeometry.location,
                positionedStrings = listOf(
                    PositionedString(
                        text = text,
                        location = userGeometry.location,
                        type = AudioType.STANDARD
                    )
                ),
                isPoint = true,
                isGeneric = false,
            )
        }
        return null
    }

    // position is the user's relationship to the structure, "over" or "under" - see CrossingInfo
    // in MvtToGeoJson.kt for why the raw OSM brunnel value isn't good enough. point is where the
    // road and the structure actually cross, absent for the water-polygon fallback below.
    private data class WayCrossingInfo(
        val type: String,
        val name: String?,
        val position: String?,
        val point: LngLatAlt?
    )

    private data class AnnouncedCrossing(
        val key: String,
        val location: LngLatAlt,
        val timestampMilliseconds: Long
    )

    /**
     * Reads the crossing_type/crossing_name/crossing_position/crossing_latitude/crossing_longitude
     * properties extractCrossings (see
     * MvtToGeoJson.kt) attaches directly onto the Way(s) that cross a named river/canal or a
     * railway, if any. An unnamed waterway crossing isn't worth announcing - there's nothing
     * useful to say beyond "Crossing" nothing - but an unnamed railway still is, since "Crossing
     * the railway" is meaningful on its own even without a line name.
     *
     * extractCrossings only covers named river/canal `waterway` lines - a firth/bay/strait is
     * tagged `natural=bay`/`natural=strait` in OSM, not as a waterway, so it never gets a
     * crossing_* property attached at parse time. Rather than trying to compute a specific
     * crossing point for those at parse time (unreliable - a firth is commonly wider than a
     * single MVT tile, so a bridge across one can straddle several tiles), fall back to a live
     * check here instead: if we're on a bridge with no pre-attached crossing info, look for a
     * named water polygon (see TreeId.NAMED_WATER_POLYGONS) at/near the current location.
     */
    private fun wayCrossingInfo(way: Way, gridState: GridState, location: LngLatAlt): WayCrossingInfo? {
        val type = way.properties?.get("crossing_type") as? String
        if (type != null) {
            val name = way.properties?.get("crossing_name") as? String
            if (type == "waterway" && name.isNullOrEmpty()) return null
            val position = way.properties?.get("crossing_position") as? String
            val latitude = way.properties?.get("crossing_latitude") as? Double
            val longitude = way.properties?.get("crossing_longitude") as? Double
            val point = if (latitude != null && longitude != null) {
                LngLatAlt(longitude, latitude)
            } else {
                null
            }
            return WayCrossingInfo(type, name, position, point)
        }

        // A bridge carries the user over the water, a tunnel takes them under it - the Clyde
        // Tunnel under the River Clyde being the obvious example. Neither is reachable through
        // extractCrossings, because a firth/bay/tidal river is a water polygon rather than a
        // waterway line.
        val position = when (way.properties?.get("brunnel")) {
            "bridge" -> "over"
            "tunnel" -> "under"
            else -> return null
        }
        val waterTree = gridState.getFeatureTree(TreeId.NAMED_WATER_POLYGONS)
        val containing = waterTree.getContainingPolygons(location).features.firstOrNull()
        val nearby = containing
            ?: waterTree.getNearestFeature(location, gridState.ruler, waterCrossingSearchDistanceMetres)
        val waterName = (nearby as? MvtFeature)?.name ?: return null
        // No crossing point for this one - a firth is commonly wider than a tile, so there's no
        // precomputed intersection to aim at. The Way is itself the bridge or tunnel though, so
        // the Way change is already an accurate trigger; see crossingToAnnounce.
        return WayCrossingInfo("waterway", waterName, position, null)
    }

    // How much warning to give before reaching a crossing the user passes under, and the bounds
    // the resulting radius is clamped to. Scaling with speed matters: at 30m/s locations arrive
    // roughly 30m apart, so a small fixed radius would be stepped straight over on a motorway.
    private val crossingTriggerLeadSeconds = 3.0
    private val crossingTriggerMinimumRadiusMetres = 25.0
    private val crossingTriggerMaximumRadiusMetres = 150.0
    // An announced crossing is forgotten once well clear of it, so that genuinely returning to the
    // same crossing later announces it again.
    private val crossingForgetDistanceMetres = 300.0
    private val crossingForgetTimeMilliseconds = 300_000L

    /**
     * Decides whether the crossing on the currently matched Way should be announced now.
     *
     * The Way-change edge this used to rely on exclusively is only meaningful when the matched Way
     * *is* the structure. Going over, it is: the road carries brunnel=bridge, so OSM split it there
     * and the Way change coincides with the crossing. Going under, the road below carries no tag at
     * all, so it is never split and the Way change lands wherever that road happens to begin -
     * measured 1705m before the railway viaduct over the M80 at Castlecary, roughly 55 seconds
     * early. So a Way with a crossing it passes under triggers on proximity to the stored crossing
     * point instead.
     */
    private fun crossingToAnnounce(
        userGeometry: UserGeometry,
        gridState: GridState,
        previousOsmId: Long?
    ): WayCrossingInfo? {
        // Crossings hang off the *road* the user is matched to, so they only mean anything if the
        // user is actually on a road. On a train the road matcher still latches onto whatever runs
        // alongside the line, and those roads carry the crossing properties for the very railway
        // being ridden - recordings had "Going under Milngavie Branch" and "Crossing Milngavie
        // Branch" interleaved with "On Milngavie Branch". Announcing a train's own crossings would
        // mean attaching them to railway Ways, which is a separate job from this one.
        //
        // Suppressed shortly after losing rail lock too, for the same reason as the vehicle
        // landmark callouts above: probablyOnTrain() can flicker false for an instant mid-journey.
        if (userGeometry.probablyOnTrain() || recentlyOnTrain(userGeometry)) return null

        val way = userGeometry.mapMatchedWay ?: return null
        val crossing = wayCrossingInfo(way, gridState, userGeometry.location) ?: return null

        // A Way carrying its own brunnel is the structure, so the edge is already accurate. This
        // also covers the water-polygon fallback above, which has no point to aim at.
        val wayIsStructure = way.properties?.get("brunnel") != null
        if (crossing.point == null || wayIsStructure) {
            val edgeFired = previousOsmId != null && previousOsmId != way.osmId
            return if (edgeFired) crossing else null
        }

        announcedCrossings.removeAll {
            ((userGeometry.timestampMilliseconds - it.timestampMilliseconds) >
                crossingForgetTimeMilliseconds) ||
                (gridState.ruler.distance(userGeometry.location, it.location) >
                    crossingForgetDistanceMetres)
        }

        val key = "${way.osmId}|${crossing.type}|${crossing.name}"
        if (announcedCrossings.any { it.key == key }) return null

        val radius = (userGeometry.speed * crossingTriggerLeadSeconds)
            .coerceIn(crossingTriggerMinimumRadiusMetres, crossingTriggerMaximumRadiusMetres)
        if (gridState.ruler.distance(userGeometry.location, crossing.point) > radius) return null

        announcedCrossings.add(
            AnnouncedCrossing(key, crossing.point, userGeometry.timestampMilliseconds)
        )
        return crossing
    }

    /**
     * Builds the spoken text for a river/canal or railway crossing, shared between the vehicle and
     * walking crossing callouts below - the wording doesn't depend on how the crossing is being
     * travelled.
     */
    private fun crossingCalloutText(crossing: WayCrossingInfo): String {
        val name = crossing.name
        // Whether we're going over or under has already been resolved from whichever side carried
        // the brunnel evidence (see CrossingInfo in MvtToGeoJson.kt) - worth distinguishing, since
        // "going under" reads oddly for a bridge and vice versa. It applies just as much to a
        // waterway as to a railway: an aqueduct carries a canal over the road beneath it.
        val goingUnder = crossing.position == "under"
        return if (name != null) {
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
    }

    /**
     * Announces a river/canal or railway crossing as it's passed while walking - the same
     * landmark buildCalloutForVehicleCrossing announces for car/bus travel, using the same
     * mapMatchedWay-based edge-trigger (see its doc comment), since extractCrossings detects a
     * crossing for any highway class (including footway/path), not just vehicle roads.
     */
    private fun buildCalloutForWalkingCrossing(userGeometry: UserGeometry, gridState: GridState): TrackedCallout? {
        if (userGeometry.inVehicle() || recentlyInVehicle(userGeometry)) {
            lastWalkingCrossingWayOsmId = null
            return null
        }

        val matchedWay = userGeometry.mapMatchedWay ?: return null

        val previousOsmId = lastWalkingCrossingWayOsmId
        lastWalkingCrossingWayOsmId = matchedWay.osmId

        val crossing = crossingToAnnounce(userGeometry, gridState, previousOsmId) ?: return null
        val text = crossingCalloutText(crossing)
        return TrackedCallout(
            userGeometry,
            trackedText = crossing.name ?: "railway",
            location = userGeometry.location,
            positionedStrings = listOf(
                PositionedString(
                    text = text,
                    location = userGeometry.location,
                    type = AudioType.STANDARD
                )
            ),
            isPoint = true,
            isGeneric = false,
        )
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

        // Order the candidates before walking them. Markers are in this list too, but the
        // weighting in PoiRanking is penalty-only, so nothing can be pushed in front of a marker
        // that isn't genuinely nearer than it. The walk below is otherwise untouched, and still
        // gates on the true distance from getDistanceToFeature rather than on any ranking score.
        val ordered = orderPoisForSpeech(
            pois.features,
            userGeometry.location,
            userGeometry.ruler,
            poiStrategy()
        )

        val uniquelyNamedPOIs = mutableMapOf<String, Feature>()
        ordered.map { it.feature }.filter { feature ->

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
                        buildCalloutForVehicleTransitStop(userGeometry, gridState, settlementGrid)
                    val vehicleWaterwayCrossingCallout =
                        buildCalloutForVehicleCrossing(userGeometry, gridState)
                    // On a train the road-matched crossings above are suppressed (see
                    // crossingToAnnounce), so this names the roads the line passes over/under
                    // instead.
                    val trainCrossingCallout =
                        buildCalloutForTrainCrossing(userGeometry, gridState)
                    // Always run alongside its vehicle equivalent above (rather than only in the
                    // pedestrian branch below) so its own tracked Way osmId resets correctly the
                    // moment vehicle travel starts - the same reason buildCalloutForVehicleCrossing
                    // itself needs to run on every update rather than only while driving.
                    val walkingCrossingCallout =
                        buildCalloutForWalkingCrossing(userGeometry, gridState)
                    val vehicleCallouts = listOfNotNull(
                        roadSenseCallout, vehicleLandmarkCallout, vehicleTransitStopCallout,
                        vehicleWaterwayCrossingCallout, trainCrossingCallout
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
                        if (walkingCrossingCallout != null) {
                            // Merge onto any intersection callout for the same update, the same
                            // way the Street Preview POI callout below merges rather than replaces.
                            if (trackedCallout != null) {
                                trackedCallout.positionedStrings += walkingCrossingCallout.positionedStrings
                            } else {
                                trackedCallout = walkingCrossingCallout
                            }
                        }
                        if ((intersectionCallout == null) || userGeometry.inStreetPreview) {
                            // Get normal callouts for nearby POIs, for the destination, and for beacons
                            val poiCallout = buildCalloutForNearbyPOI(userGeometry, gridState)

                            // Update time/location filter for our new position
                            if (poiCallout != null) {
                                poiCallout.locationFilter = poiFilter
                                // Merge onto any callout already queued for this update (an
                                // intersection in Street Preview, or a walking crossing - see
                                // above) rather than replacing it outright.
                                if (trackedCallout != null) {
                                    trackedCallout.positionedStrings += poiCallout.positionedStrings
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
