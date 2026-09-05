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
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.AlongWayFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.AlongWayKind
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.AlongWayPosition
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.utils.CountryBoundaries
import org.scottishtecharmy.soundscape.geoengine.utils.DrivingSide
import org.scottishtecharmy.soundscape.geoengine.utils.AlongWayFeatureAhead
import org.scottishtecharmy.soundscape.geoengine.utils.PoiRankStrategy
import org.scottishtecharmy.soundscape.geoengine.utils.Side
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.getDistanceToFeature
import org.scottishtecharmy.soundscape.geoengine.utils.WayContinuation
import org.scottishtecharmy.soundscape.geoengine.utils.forEachAlongWayFeatureAhead
import org.scottishtecharmy.soundscape.geoengine.utils.nextAlongWayFeature
import org.scottishtecharmy.soundscape.geoengine.utils.getFovTriangle
import org.scottishtecharmy.soundscape.geoengine.utils.orderPoisForSpeech
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.i18n.StringKey
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider

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
    // null means "no baseline yet" (just entered this travel mode), not "currently unmatched" -
    // losing map-match briefly (mapMatchedWay == null) deliberately leaves this alone rather than
    // resetting it, so a transient map-match gap right as the user reaches a bridge doesn't
    // suppress the callout. See buildCalloutForVehicleCrossing/buildCalloutForWalkingCrossing.
    private var lastVehicleCrossingWayOsmId: Long? = null
    private var lastWalkingCrossingWayOsmId: Long? = null
    // Along-way features already announced - crossings and transit stops alike. Shared across the
    // builders so that flipping travel mode partway across a structure can't announce the same
    // crossing twice.
    //
    // CalloutHistory isn't usable for any of these: its trim() hardcodes a 50m radius, so an entry
    // is dropped and re-armed while the user is still approaching the thing it was recorded for -
    // which is most of the approach when a stop is announced 100m out.
    private val announcedAlongWayFeatures = mutableListOf<AnnouncedAlongWayFeature>()
    // Where the user was on the previous update, and how far back along the Ways the along-way
    // queries should therefore look - see updateSweepWindow.
    private var lastSweepLocation: LngLatAlt? = null
    private var lastSweepTimestamp = 0L
    private var sweepBehindMetres = 0.0
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

    // How far ahead a transit stop is announced while travelling by vehicle. Far enough to be of
    // any use - being told about a stop as it goes past is too late to do anything with - and this
    // is expected to be tuned once it has been ridden with.
    private val transitStopLookaheadMetres = 100.0

    /**
     * Announces a bus/tram stop on the approach to it while travelling by car/bus, about
     * [transitStopLookaheadMetres] before it is reached.
     *
     * The stop is found by walking up the road being driven -
     * GridState.attachTransitStopsToWays records each stop against the road it serves, at its
     * position along it - so this is a lookup on the road ahead rather than a search of everything
     * near the path travelled. That search could only judge by proximity, and so couldn't tell a
     * stop on this road from one on the street behind the hedge.
     *
     * The walk follows the road by name or ref through junctions (WayContinuation.SAME_ROAD). A
     * hundred metres of an urban main road crosses several side streets, and stopping dead at the
     * first of them would put almost every stop out of reach.
     */
    private fun buildCalloutForVehicleTransitStop(
        userGeometry: UserGeometry,
        gridState: GridState,
        settlementGrid: GridState
    ): TrackedCallout? {
        // Also covers a brief stop (red light, station dwell) via recentlyInVehicle, so an
        // approaching stop isn't dropped the moment the traffic does - only skipped once genuinely
        // no longer in a vehicle, e.g. actually got out and started walking.
        if (!userGeometry.inVehicle() && !recentlyInVehicle(userGeometry)) return null
        val way = userGeometry.mapMatchedWay ?: return null

        val found = transitStopAhead(userGeometry, way) ?: return null
        val stopFeature = found.feature.feature ?: return null
        val stopText = stopFeature.getText(localized)
        if (stopText.generic) return null

        // Keyed on the stop itself rather than on its text, so that the whole approach is one
        // announcement: the callout fires when the stop first comes within range and stays quiet
        // for the rest of the way in. CalloutHistory can't do this - see announcedAlongWayFeatures.
        val key = "stop|${stopFeature.osmId}|${found.feature.point}"
        if (announcedAlongWayFeatures.any { it.key == key }) return null

        val calloutText = if (stopFeature.name == null) {
            enrichUnnamedTransitStopText(
                stopText.text, found.feature.point, gridState, settlementGrid
            )
        } else {
            stopText.text
        }
        val callout = TrackedCallout(
            userGeometry,
            trackedText = calloutText,
            // The stop's own position, not the nearest point on it to the user: it's ahead, and
            // that's where the spatialised audio should come from.
            location = found.feature.point,
            positionedStrings = listOf(
                PositionedString(
                    // "Approaching", not "Near": this fires while the stop is still ahead, and
                    // "Near X" reads as a note of passing something rather than a warning that
                    // it's coming up.
                    text = localized?.get(StringKey.DirectionsApproachingName, calloutText)
                        ?: "Approaching $calloutText",
                    location = found.feature.point,
                    type = AudioType.LOCALIZED
                )
            ),
            isPoint = stopFeature.geometry.type == "Point",
            isGeneric = false,
        )

        announcedAlongWayFeatures.add(
            AnnouncedAlongWayFeature(key, found.feature.point, userGeometry.timestampMilliseconds)
        )
        notableVehicleEventTracker.recordEvent(userGeometry.timestampMilliseconds)
        return callout
    }

    // How far ahead a railway stop is announced. Longer than the road equivalent because a train
    // covers ground faster and there is more a passenger might want to do with the warning - at
    // 30m/s this is about seventeen seconds. Expected to be tuned, like the road one.
    private val railwayStopLookaheadMetres = 500.0

    /**
     * Announces the next station the line stops at, on the approach to it.
     *
     * Read off the line being ridden, from the railway=stop nodes OSM places on the line itself
     * (see GridState.attachRailwayStopsToWays). A station POI could only be matched to a line by
     * proximity, and where lines run close together the nearest station to a train is often one
     * its line runs straight past - which is exactly the mistake this avoids.
     *
     * Follows the line by name through junctions (WayContinuation.SAME_ROAD), since half a
     * kilometre of railway crosses junctions the way a main road crosses side streets.
     */
    private fun buildCalloutForTrainStop(
        userGeometry: UserGeometry,
        gridState: GridState
    ): TrackedCallout? {
        if (!userGeometry.probablyOnTrain()) return null
        val railway = userGeometry.mapMatchedRailway ?: return null
        val cursor = userGeometry.cursorOn(railway, sweepHeading(userGeometry)) ?: return null
        if (cursor.forwards == null) return null

        val found = nextAlongWayFeature(
            cursor,
            railwayStopLookaheadMetres,
            AlongWayKind.RAILWAY_STOP,
            WayContinuation.SAME_ROAD
        ) ?: return null
        val name = found.feature.name ?: return null

        // Keyed on the name, not the node: a station is commonly several stop nodes, one per
        // platform, and they are all the same station to a passenger.
        val key = "railwaystop|$name"
        if (announcedAlongWayFeatures.any { it.key == key }) return null
        announcedAlongWayFeatures.add(
            AnnouncedAlongWayFeature(key, found.feature.point, userGeometry.timestampMilliseconds)
        )

        val text = localized?.get(StringKey.DirectionsApproachingName, name)
            ?: "Approaching $name"
        return TrackedCallout(
            userGeometry,
            trackedText = name,
            location = found.feature.point,
            positionedStrings = listOf(
                PositionedString(
                    text = text,
                    location = found.feature.point,
                    type = AudioType.LOCALIZED
                )
            ),
            isPoint = true,
            isGeneric = false,
        )
    }

    /**
     * The next transit stop up the road, within [transitStopLookaheadMetres], or null.
     *
     * Stops on the far kerb serve the opposite direction and are skipped. Which kerb is the near
     * one is a property of the country - left of the direction of travel where traffic drives on
     * the left, right where it drives on the right (see CountryBoundaries) - and which kerb the
     * stop is on was settled when it was attached, as a side relative to the road's own direction.
     * So this only has to flip that when travelling against the road's direction. Where the
     * country can't be determined, no filtering happens rather than a guess: naming the stop
     * across the road beats naming none.
     *
     * A known direction of travel is required. Without one there is no "ahead" to look down, and
     * no way to tell which kerb is near - and no direction means barely moving, when nothing is
     * being approached anyway.
     */
    private fun transitStopAhead(
        userGeometry: UserGeometry,
        way: Way
    ): AlongWayFeatureAhead? {
        val cursor = userGeometry.cursorOn(way, sweepHeading(userGeometry)) ?: return null
        val forwards = cursor.forwards ?: return null
        val nearSide = CountryBoundaries.drivingSide(userGeometry.location)?.let {
            if (it == DrivingSide.LEFT) Side.LEFT else Side.RIGHT
        }

        var found: AlongWayFeatureAhead? = null
        forEachAlongWayFeatureAhead(
            cursor,
            transitStopLookaheadMetres,
            WayContinuation.SAME_ROAD
        ) { candidate ->
            if (candidate.feature.kind != AlongWayKind.TRANSIT_STOP) return@forEachAlongWayFeatureAhead true
            val side = candidate.feature.side
            if ((nearSide != null) && (side != null)) {
                // The recorded side is relative to the road's START-to-END direction, so it reads
                // directly when travelling that way and inverts when travelling back.
                val sideOfTravel = if (forwards) {
                    side
                } else {
                    when (side) {
                        Side.LEFT -> Side.RIGHT
                        Side.RIGHT -> Side.LEFT
                        Side.INLINE -> Side.INLINE
                    }
                }
                if (sideOfTravel != nearSide) return@forEachAlongWayFeatureAhead true
            }
            found = candidate
            false
        }
        return found
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
     * navigation points ("Passing over Allander Water", "Passing over the railway") worth calling out on
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
     * Announces the roads a train passes over and under, and the rivers and canals it crosses.
     *
     * Both come straight off the line the passenger is riding. The map matcher has already decided
     * which railway Way that is, and GridState.attachRailwayCrossings records every road crossing
     * and every named river/canal crossing onto the railway Ways themselves - so this is a walk of
     * two short pre-sorted lists on one known Way, with no geographic search at all.
     *
     * That matters for the roads in particular. They used to be found by searching the road tree
     * around the user and keeping the ones whose recorded crossing named this line, which meant
     * sifting every road within the trigger radius - and on a railway most of those are running
     * *alongside* the line rather than crossing it. The crossing is now recorded on both sides
     * when it's found, which is the same single geometric test either way.
     *
     * The stored position is the train's own in both lists: for a road it was inverted at attach
     * time, since a road recorded as going over the line is a bridge the train passes beneath.
     *
     * Only grade-separated crossings appear, because that's all attachRailwayCrossings records - a
     * level crossing has no brunnel on either side and is deliberately left to the explicit
     * railway=level_crossing point. Unnamed roads are skipped: "Crossing" an unnamed track isn't
     * worth saying, the same reasoning as for an unnamed waterway in wayCrossingInfo.
     */
    private fun buildCalloutForTrainCrossing(userGeometry: UserGeometry, gridState: GridState): TrackedCallout? {
        if (!userGeometry.probablyOnTrain()) return null
        val railway = userGeometry.mapMatchedRailway ?: return null

        // Everything the line meets within reach, in the order it will be met - measured along the
        // rails, not as the crow flies.
        for (found in crossingsInReach(userGeometry, railway, railwaySideCrossingKinds)) {
            val crossing = found.feature
            if (crossing.kind == AlongWayKind.WATERWAY_CROSSING) {
                val waterName = crossing.name ?: continue

                // Keyed on the water's name, so a line crossing a river on two adjacent bridge
                // decks announces it once.
                val key = "water|$waterName"
                if (announcedAlongWayFeatures.any { it.key == key }) continue

                // The recorded position already describes the train's own relationship to the
                // water, as it does for the roads - both were inverted when they were attached.
                announcedAlongWayFeatures.add(
                    AnnouncedAlongWayFeature(key, crossing.point, userGeometry.timestampMilliseconds)
                )
                return trainCrossingCallout(
                    userGeometry, waterName, crossingCalloutText(WayCrossingInfo(crossing))
                )
            }

            // Only genuinely named roads. Way.getName confects a name for anything unnamed, which
            // from a train reads as noise rather than a landmark - "Passing over Service that
            // joins Lennox Park and Crossveggate" tells a passenger nothing.
            val road = crossing.feature as? Way ?: continue
            if ((road.name == null) && (road.ref == null)) continue
            val roadName = road.getName(null, gridState, localized, true)
            if (roadName.isEmpty()) continue

            // Keyed on the name rather than the osmId, so a dual carriageway carried on two
            // separate bridge decks is announced once rather than twice. Genuinely crossing the
            // same road again later in the journey still re-announces, once the earlier entry has
            // aged or fallen far enough behind to be pruned above.
            val key = "road|$roadName"
            if (announcedAlongWayFeatures.any { it.key == key }) continue

            val text = if (crossing.position == AlongWayPosition.UNDER) {
                localized?.get(StringKey.DirectionsGoingUnderRailway, roadName)
                    ?: "Passing under $roadName"
            } else {
                localized?.get(StringKey.DirectionsCrossingWaterway, roadName)
                    ?: "Passing over $roadName"
            }

            announcedAlongWayFeatures.add(
                AnnouncedAlongWayFeature(key, crossing.point, userGeometry.timestampMilliseconds)
            )
            return trainCrossingCallout(userGeometry, roadName, text)
        }
        return null
    }

    // The union of a crossing recorded against a Way (an AlongWayFeature) and the live
    // named-water-polygon fallback in wayCrossingInfo below, which has no crossing point at all -
    // hence the nullable point, which an AlongWayFeature doesn't have.
    //
    // position is the user's relationship to the structure - see AlongWayPosition for why the raw
    // OSM brunnel value isn't good enough.
    private data class WayCrossingInfo(
        val kind: AlongWayKind,
        val name: String?,
        val position: AlongWayPosition?,
        val point: LngLatAlt?
    ) {
        constructor(feature: AlongWayFeature) :
            this(feature.kind, feature.name, feature.position, feature.point)
    }

    private data class AnnouncedAlongWayFeature(
        val key: String,
        val location: LngLatAlt,
        val timestampMilliseconds: Long
    )

    /**
     * Reads the crossing AlongWayFeature that extractCrossings (see MvtToGeoJson.kt) or
     * GridState.attachRailwayCrossings attaches to the Way(s) that cross a named river/canal or a
     * railway, if any. An unnamed waterway crossing isn't worth announcing - there's nothing
     * useful to say beyond "Crossing" nothing - but an unnamed railway still is, since "Crossing
     * the railway" is meaningful on its own even without a line name.
     *
     * Where a Way carries more than one crossing (a viaduct over both a river and a railway), the
     * waterway is preferred: it's the bigger landmark of the two.
     *
     * extractCrossings only covers named river/canal `waterway` lines - a firth/bay/strait is
     * tagged `natural=bay`/`natural=strait` in OSM, not as a waterway, so it never gets a crossing
     * attached at parse time. Rather than trying to compute a specific crossing point for those at
     * parse time (unreliable - a firth is commonly wider than a single MVT tile, so a bridge
     * across one can straddle several tiles), fall back to a live check here instead: if we're on
     * a bridge with no pre-attached crossing info, look for a named water polygon (see
     * TreeId.NAMED_WATER_POLYGONS) at/near the current location.
     */
    private fun wayCrossingInfo(way: Way, gridState: GridState, location: LngLatAlt): WayCrossingInfo? {
        val attached = way.firstAlongWayFeature(AlongWayKind.WATERWAY_CROSSING)
            ?: way.firstAlongWayFeature(AlongWayKind.RAILWAY_CROSSING)
        if (attached != null) {
            if ((attached.kind == AlongWayKind.WATERWAY_CROSSING) && attached.name.isNullOrEmpty()) {
                return null
            }
            return WayCrossingInfo(attached)
        }

        // A bridge carries the user over the water, a tunnel takes them under it - the Clyde
        // Tunnel under the River Clyde being the obvious example. Neither is reachable through
        // extractCrossings, because a firth/bay/tidal river is a water polygon rather than a
        // waterway line.
        val position = when (way.properties?.get("brunnel")) {
            "bridge" -> AlongWayPosition.OVER
            "tunnel" -> AlongWayPosition.UNDER
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
        return WayCrossingInfo(AlongWayKind.WATERWAY_CROSSING, waterName, position, null)
    }

    private fun trainCrossingCallout(
        userGeometry: UserGeometry,
        trackedText: String,
        text: String
    ) = TrackedCallout(
        userGeometry,
        trackedText = trackedText,
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

    // What a train passenger can meet: the roads crossing the line, and the water the line itself
    // crosses. RAILWAY_CROSSING is the road-side mirror and belongs to the road user.
    private val railwaySideCrossingKinds =
        setOf(AlongWayKind.WATERWAY_CROSSING, AlongWayKind.ROAD_CROSSING)

    // The crossing kinds a road user can meet. ROAD_CROSSING is the railway-side mirror and would
    // be nonsense here, and the kinds still to come (transit stops, junctions) aren't crossings at
    // all - so this is an allow-list rather than an exclusion.
    private val roadSideCrossingKinds =
        setOf(AlongWayKind.WATERWAY_CROSSING, AlongWayKind.RAILWAY_CROSSING)

    // Bounds on the backward window in updateSweepWindow. A minute without a fix is a gap in
    // tracking rather than a long step, and 1km is further than any single step at line speed -
    // beyond either, the ground in between wasn't necessarily travelled.
    private val sweepMaximumGapMilliseconds = 60_000L
    private val sweepMaximumBehindMetres = 1000.0

    // How much warning to give before reaching a crossing the user passes under, and the bounds
    // the resulting radius is clamped to. Scaling with speed matters: at 30m/s locations arrive
    // roughly 30m apart, so a small fixed radius would be stepped straight over on a motorway.
    private val crossingTriggerLeadSeconds = 3.0
    private val crossingTriggerMinimumRadiusMetres = 25.0
    private val crossingTriggerMaximumRadiusMetres = 150.0
    // An announced crossing or stop is forgotten once well clear of it - see updateSweepWindow.
    // Must exceed the largest lookahead below, or something announced at range is forgotten while
    // still being approached and announced again on the next fix.
    private val announcedForgetDistanceMetres = 1500.0
    private val announcedForgetTimeMilliseconds = 300_000L

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
        // being ridden - recordings had "Passing under Milngavie Branch" and "Passing over Milngavie
        // Branch" interleaved with "On Milngavie Branch". Announcing a train's own crossings would
        // mean attaching them to railway Ways, which is a separate job from this one.
        //
        // Suppressed shortly after losing rail lock too, for the same reason as the vehicle
        // landmark callouts above: probablyOnTrain() can flicker false for an instant mid-journey.
        if (userGeometry.probablyOnTrain() || recentlyOnTrain(userGeometry)) return null

        val way = userGeometry.mapMatchedWay ?: return null

        // A Way carrying its own brunnel is the structure, so arriving on it *is* the crossing and
        // the Way-change edge is already an accurate trigger. It's also the only route to the
        // water-polygon fallback in wayCrossingInfo, which needs the brunnel as its evidence and
        // has no recorded crossing to measure a distance to.
        //
        // Anything else falls through to the walk below, including a Way carrying no crossing of
        // its own - the crossing being announced is often on a Way further along, and that's the
        // whole point of walking rather than reading the matched Way alone.
        if (way.properties?.get("brunnel") != null) {
            val crossing = wayCrossingInfo(way, gridState, userGeometry.location) ?: return null
            val edgeFired = previousOsmId != null && previousOsmId != way.osmId
            return if (edgeFired) crossing else null
        }

        val found = crossingsInReach(userGeometry, way, roadSideCrossingKinds).firstOrNull()
            ?: return null
        val crossing = WayCrossingInfo(found.feature)
        if ((crossing.kind == AlongWayKind.WATERWAY_CROSSING) && crossing.name.isNullOrEmpty()) {
            return null
        }

        // Keyed on the Way the crossing is recorded against rather than the one we're matched to,
        // so that approaching it across a Way boundary and then reaching it doesn't announce twice.
        val key = "${found.way.osmId}|${crossing.kind}|${crossing.name}"
        if (announcedAlongWayFeatures.any { it.key == key }) return null

        announcedAlongWayFeatures.add(
            AnnouncedAlongWayFeature(key, found.feature.point, userGeometry.timestampMilliseconds)
        )
        return crossing
    }

    /**
     * Records how far the user has moved since the previous update, which is how far back along
     * the Ways the along-way queries look - crossingsInReach for the crossings, and
     * transitStopsPassed for the stops. Called once per update, before any callout is built, so
     * that they all see the same window.
     *
     * Gated on elapsed time rather than on distance moved. Distance is no help in telling travel
     * from a jump - 400m between fixes is thirteen seconds of motorway, and rejecting it would
     * throw away exactly the sparse-fix case this window exists for. A long gap between fixes is
     * the real signal that the intervening ground wasn't travelled: a resumed session, a Street
     * Preview teleport, or tracking that stopped and restarted somewhere else. The cap catches
     * what's left, a jump inside the time limit.
     */
    private fun updateSweepWindow(userGeometry: UserGeometry) {
        // Forget what was announced long ago or far behind, so that genuinely coming back to the
        // same crossing or stop later announces it again.
        announcedAlongWayFeatures.removeAll {
            ((userGeometry.timestampMilliseconds - it.timestampMilliseconds) >
                announcedForgetTimeMilliseconds) ||
                (userGeometry.ruler.distance(userGeometry.location, it.location) >
                    announcedForgetDistanceMetres)
        }

        val previous = lastSweepLocation
        val elapsed = userGeometry.timestampMilliseconds - lastSweepTimestamp
        sweepBehindMetres = if (
            (previous == null) || (elapsed <= 0) || (elapsed > sweepMaximumGapMilliseconds)
        ) {
            0.0
        } else {
            // Crow-fly between the two fixes, with slack for the road not being straight between
            // them, so the window is never shorter than the road actually travelled.
            (userGeometry.ruler.distance(previous, userGeometry.location) * 1.5)
                .coerceAtMost(sweepMaximumBehindMetres)
        }
        lastSweepLocation = userGeometry.location
        lastSweepTimestamp = userGeometry.timestampMilliseconds
    }

    /**
     * Every along-way feature of [kinds] within reach of the user along the Way network, nearest
     * first.
     *
     * Distance is measured *along the road* from where the user is on it, walking into the Ways
     * beyond the end of this one - not as the crow flies from the user's location, which is what
     * this used to do. Crow-fly is only an approximation of "how far until I reach it", and it
     * gets worse the less straight the road is: on a road curving back towards a bridge it reads
     * small while the distance still to drive is large.
     *
     * Two windows. Ahead is the lead distance, so there's time to say it before it arrives. Behind
     * is however far the user has come since the last update, because a crossing that fell between
     * two fixes was never inside the lookahead on either of them, and saying "Passing under X" a
     * moment late beats never saying it. That second window is normally inert - fixes arrive about
     * a second apart and the lookahead is three seconds of travel - and earns its place when fixes
     * are sparse, or at line speed where the lookahead is clamped.
     *
     * When the direction of travel isn't known - stationary, or no travel heading yet - the walk
     * goes both ways and the nearest wins, which beats guessing which way the user is pointing.
     *
     * Ties go to the waterway. A Way can carry both (a viaduct over a river and a railway at once)
     * and the river is the bigger landmark.
     */
    /**
     * The bearing from where the user was on the previous fix, for when the fix itself carries no
     * usable travel heading. Movement between two fixes says which way they are going just as well,
     * and this is how the transit stop sweep used to decide it before the along-way queries
     * existed.
     *
     * Null below a couple of metres of movement, where the bearing is mostly noise.
     */
    private fun sweepHeading(userGeometry: UserGeometry): Double? {
        val previous = lastSweepLocation ?: return null
        if (userGeometry.ruler.distance(previous, userGeometry.location) < 2.0) return null
        return userGeometry.ruler.bearing(previous, userGeometry.location)
    }

    private fun crossingsInReach(
        userGeometry: UserGeometry,
        way: Way,
        kinds: Set<AlongWayKind>
    ): List<AlongWayFeatureAhead> {
        val cursor = userGeometry.cursorOn(way, sweepHeading(userGeometry))
            ?: return emptyList()
        val lookahead = (userGeometry.speed * crossingTriggerLeadSeconds)
            .coerceIn(crossingTriggerMinimumRadiusMetres, crossingTriggerMaximumRadiusMetres)

        val found = mutableListOf<AlongWayFeatureAhead>()
        forEachAlongWayFeatureAhead(cursor, lookahead) {
            if (it.feature.kind in kinds) found.add(it)
            true
        }

        // Only needed when the direction is known, since the walk above then went one way only.
        val forwards = cursor.forwards
        if ((forwards != null) && (sweepBehindMetres > 0.0)) {
            forEachAlongWayFeatureAhead(
                cursor.copy(forwards = !forwards),
                sweepBehindMetres
            ) {
                if (it.feature.kind in kinds) found.add(it)
                true
            }
        }
        return found.sortedWith(compareBy({ it.distance }, { it.feature.kind.ordinal }))
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
        val goingUnder = crossing.position == AlongWayPosition.UNDER
        return if (name != null) {
            if (goingUnder) {
                localized?.get(StringKey.DirectionsGoingUnderRailway, name) ?: "Passing under $name"
            } else {
                localized?.get(StringKey.DirectionsCrossingWaterway, name) ?: "Passing over $name"
            }
        } else if (goingUnder) {
            localized?.get(StringKey.DirectionsGoingUnderRailwayGeneric) ?: "Passing under the railway"
        } else {
            localized?.get(StringKey.DirectionsCrossingRailwayGeneric) ?: "Passing over the railway"
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

    /**
     * Where the user last was while matched to a tunnel, or null when they're reckoned to be out in
     * the open - see [buildCalloutForTunnel]. Deliberately a position rather than the tunnel's
     * identity: a long tunnel is several OSM ways (the Charing Cross tunnel on the North Clyde Line
     * is four, and the two tracks through it have their own ids again), each split further at
     * intersections and tile seams, so keying on which way is matched announces the same tunnel
     * over and over as the match steps between them.
     */
    private var lastTunnelLocation: LngLatAlt? = null

    /**
     * Where the current spell underground began, used to measure how far into a tunnel the user has
     * actually got - see [unnamedTunnelAnnounceDistanceMetres]. Survives the short surface gaps
     * [tunnelForgetDistanceMetres] rides out, so a stub tunnel immediately before the real one
     * doesn't restart the measurement.
     */
    private var tunnelEntryLocation: LngLatAlt? = null

    /** Whether the current spell underground has already been announced. */
    private var tunnelAnnounced = false

    /**
     * How far the user has to get from the last place they were matched to a tunnel before the
     * callout re-arms. Covers both the match flickering between a tunnel way and the surface way
     * beside it at a tunnel mouth, and a run of dropped fixes underground (see isAccuracyUsable)
     * leaving a gap in the sequence. Two genuinely separate tunnels closer together than this are
     * announced once, which is the better reading of them anyway.
     */
    private val tunnelForgetDistanceMetres = 200.0

    /**
     * How far into an *unnamed* tunnel the user has to get before it's worth mentioning.
     *
     * OSM only names tunnels that are actually tunnels. What it leaves unnamed, on the railway at
     * least, is overwhelmingly the few metres of cover where a road bridges the line: measured over
     * the central-belt extract, unnamed rail tunnel segments have a median length of 49m and a 90th
     * percentile of 100m, against 142m and 597m for named ones. Announcing those is noise - a
     * passenger is out the far side before the callout finishes - and worse, an unnamed 21m stub
     * sits immediately before the mouth of the Finnieston Tunnel, so announcing it swallowed the
     * real tunnel's callout through the re-arm rule above.
     *
     * Measuring distance travelled rather than filtering on the matched Way's own length is what
     * makes this safe: a long tunnel is split into pieces at intersections and tile seams, and
     * named ones come in pieces as short as 5m (the Queen Street High Level Tunnel), so a
     * per-segment length test would throw away exactly the tunnels worth announcing.
     */
    private val unnamedTunnelAnnounceDistanceMetres = 100.0

    /**
     * Announces going into a tunnel, whether walking, driving or riding a train.
     *
     * Worth saying on its own account - a tunnel is a landmark, and a long one is a notable part of
     * a journey - but it's also the honest explanation for what follows. Underground, GPS doesn't
     * stop, it just degrades: recorded train journeys through central Glasgow keep producing usable
     * fixes for a couple of hundred metres past the tunnel mouth, then collapse to 200-700m
     * accuracy, at which point isAccuracyUsable has GeoEngine drop them and the journey simply goes
     * quiet. "Entering a tunnel" tells the user why.
     *
     * Both kinds of tunnel are answered the same way, from the Way the user is already map matched
     * to: a tunnel is a `brunnel=tunnel` segment of the road or of the railway, and being matched
     * to one is what it means to be in it. No geometric search is needed, and no guessing - which
     * matters, because a road directly above a rail tunnel is common in a city and a proximity test
     * couldn't tell the two apart. The railway answer is preferred when both are available: on a
     * train the road matcher still latches onto whatever runs overhead, and the line is the better
     * account of where the user actually is.
     */
    private fun buildCalloutForTunnel(
        userGeometry: UserGeometry,
        gridState: GridState
    ): TrackedCallout? {
        val tunnel = tunnelWay(userGeometry)
        if (tunnel == null) {
            // Only count as out in the open once well clear of where we last were underground, so
            // that the callout re-arms for the next tunnel but not for a wobble at this one's mouth.
            lastTunnelLocation?.let { last ->
                if (gridState.ruler.distance(userGeometry.location, last) >
                    tunnelForgetDistanceMetres
                ) {
                    lastTunnelLocation = null
                    tunnelEntryLocation = null
                    tunnelAnnounced = false
                }
            }
            return null
        }

        val entry = tunnelEntryLocation ?: userGeometry.location.also { tunnelEntryLocation = it }
        lastTunnelLocation = userGeometry.location
        if (tunnelAnnounced) return null

        // OSM's `tunnel:name`, carried through the tile pipeline as `tunnel_name` - "Finnieston
        // Tunnel", "Charing Cross Tunnel". Only about half the tunnels in an extract have one (the
        // short covered stretches generally don't), so a generic "Entering a tunnel" is the normal
        // case rather than the exception.
        //
        // The Way's own name is deliberately not used as a fallback. On a railway it names the
        // *line* running through the tunnel ("North Clyde Line"), which the preceding "On North
        // Clyde Line" callouts have already said; on a road it names the road ("M8"), so "Entering
        // M8" would be actively misleading. Way.getName is worse still, confecting things like
        // "Path via tunnel to Station Road". Better to say nothing than the wrong thing.
        val name = tunnel.properties?.get("tunnel_name") as? String
        if ((name == null) &&
            (gridState.ruler.distance(userGeometry.location, entry) <
                unnamedTunnelAnnounceDistanceMetres)
        ) {
            // Not far enough in to know this is a tunnel rather than a bridge overhead. Wait -
            // either the distance mounts up, or a named piece of the same tunnel turns up.
            return null
        }
        tunnelAnnounced = true

        val text = if (name != null) {
            localized?.get(StringKey.DirectionsEnteringTunnelNamed, name) ?: "Entering $name"
        } else {
            localized?.get(StringKey.DirectionsEnteringTunnel) ?: "Entering a tunnel"
        }

        return TrackedCallout(
            userGeometry,
            trackedText = name ?: "tunnel",
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

    /**
     * The tunnel Way the user is in right now, or null if they're out in the open.
     */
    private fun tunnelWay(userGeometry: UserGeometry): Way? {
        userGeometry.mapMatchedRailway?.let { railway ->
            if (railway.properties?.get("brunnel") == "tunnel") return railway
        }
        userGeometry.mapMatchedWay?.let { way ->
            if (way.properties?.get("brunnel") == "tunnel") return way
        }
        return null
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

                // Before any builder runs, so every along-way query shares one window.
                updateSweepWindow(userGeometry)

                val destinationCallout = buildCalloutForDestination(userGeometry)
                if (destinationCallout != null) {
                    // Update the destination filter if we're outputting it
                    destinationCallout.locationFilter = destinationFilter
                    trackedCallout = destinationCallout
                } else if (preferences?.getBoolean(PreferenceKeys.ALLOW_CALLOUTS, true) != false) {
                    // Going into a tunnel is worth saying however the user is travelling, so it's
                    // computed outside the vehicle/pedestrian split below and merged onto whatever
                    // else this update produced.
                    val tunnelCallout = buildCalloutForTunnel(userGeometry, gridState)
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
                    val trainStopCallout = buildCalloutForTrainStop(userGeometry, gridState)
                    // Always run alongside its vehicle equivalent above (rather than only in the
                    // pedestrian branch below) so its own tracked Way osmId resets correctly the
                    // moment vehicle travel starts - the same reason buildCalloutForVehicleCrossing
                    // itself needs to run on every update rather than only while driving.
                    val walkingCrossingCallout =
                        buildCalloutForWalkingCrossing(userGeometry, gridState)
                    val vehicleCallouts = listOfNotNull(
                        roadSenseCallout, vehicleLandmarkCallout, vehicleTransitStopCallout,
                        vehicleWaterwayCrossingCallout, trainCrossingCallout, trainStopCallout
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
                    if (tunnelCallout != null) {
                        if (trackedCallout != null) {
                            trackedCallout.positionedStrings += tunnelCallout.positionedStrings
                        } else {
                            trackedCallout = tunnelCallout
                        }
                    }
                }
                trackedCallout
            }
        }
    }
}
