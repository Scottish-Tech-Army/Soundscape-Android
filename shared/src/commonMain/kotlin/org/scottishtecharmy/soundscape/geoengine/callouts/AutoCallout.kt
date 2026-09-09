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
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.WayEnd
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
    // An announced crossing or stop is forgotten once well clear of it - see updateSweepWindow.
    // Must exceed the largest lookahead below, or something announced at range is forgotten while
    // still being approached and announced again on the next fix.
    private val announcedForgetDistanceMetres = 1500.0
    private val announcedForgetTimeMilliseconds = 300_000L
    // Two records of one thing, near enough to each other to be that thing rather than another of
    // the same name: the separate bridge decks a river or a dual carriageway is carried over a
    // railway on. Generous because the name has to match as well - see
    // TrackedCallout.matchRadiusMetres - and short of announcedForgetDistanceMetres, so that
    // meeting the same name again much later in a journey is still a new callout.
    private val adjacentStructureMatchRadiusMetres = 200.0

    // Everything announced off an along-way lookup - crossings and stops alike - so that the
    // whole approach to one of them is a single announcement rather than one per fix. One history
    // across all the builders, so that changing travel mode partway across a structure can't
    // announce the same crossing twice.
    //
    // Its own radii rather than CalloutHistory's defaults: these are announced at range, keyed on
    // where the thing is rather than where the user was, so an entry has to outlive the approach
    // instead of being dropped and re-armed throughout it.
    private val alongWayCalloutHistory = CalloutHistory(
        expiryPeriodMilliseconds = announcedForgetTimeMilliseconds,
        trimRadiusMetres = announcedForgetDistanceMetres
    )
    // Where the user was on the previous update, and how far back along the Ways the along-way
    // queries should therefore look - see updateSweepWindow.
    private var lastSweepLocation: LngLatAlt? = null
    // Where the user was on the update *before* this one. Held separately because
    // lastSweepLocation is overwritten by updateSweepWindow before any builder runs, so by the
    // time sweepHeading is called it is already the current location - see sweepHeading.
    private var sweepPreviousLocation: LngLatAlt? = null
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

        // Deliberately below the bookkeeping above and not at the top of the function: the sticky
        // vehicle/train windows are read by callouts this setting has nothing to do with, so they
        // have to keep being updated whether or not this one is allowed to speak.
        if (!mobilityCalloutsEnabled()) return null

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
     *
     * This is the travel-mode counterpart of buildCalloutForNearbyPOI, so it answers to the same
     * setting. It reads TreeId.LANDMARK_POIS rather than the tree that setting selects into
     * (TreeId.SELECTED_SUPER_CATEGORIES holds what a *pedestrian* should hear, which is a wider
     * net than a car should be given), so it has to check the preference itself.
     */
    private fun buildCalloutForVehicleLandmark(
        userGeometry: UserGeometry,
        gridState: GridState
    ): TrackedCallout? {
        if (!placesAndLandmarkCalloutsEnabled()) {
            return null
        }

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

    // The transit stop kinds the "Bus and tram stops" setting covers. Deliberately not the whole
    // of TreeId.TRANSIT_STOPS: a station or a ferry terminal is a destination in its own right and
    // is passed rarely enough that nobody needs it switched off, whereas these two are what make
    // an urban street or a bus route noisy.
    private val busAndTramStopValues = setOf("bus_stop", "tram_stop")

    /**
     * The "Mobility" callout setting.
     *
     * As well as the mobility POIs it selects at grid load time (see GridState.classifyPois), it
     * covers the intersection callouts and the in-vehicle road-sense callout - which is what makes
     * its description, "Intersection and transportation information", true. Both of those are
     * read here rather than through the grid's enabled categories, so they are gated on the
     * current value rather than on whatever it was when the grid was last built.
     */
    private fun mobilityCalloutsEnabled(): Boolean =
        preferences?.getBoolean(PreferenceKeys.MOBILITY, PreferenceDefaults.MOBILITY)
            ?: PreferenceDefaults.MOBILITY

    /**
     * The "Places and Landmarks" callout setting.
     *
     * Walking callouts get this for free - the setting chooses what goes into
     * TreeId.SELECTED_SUPER_CATEGORIES at grid load time, and buildCalloutForNearbyPOI reads only
     * that. Anything reaching past that tree to a super-category tree of its own has to ask here.
     */
    private fun placesAndLandmarkCalloutsEnabled(): Boolean =
        preferences?.getBoolean(
            PreferenceKeys.PLACES_AND_LANDMARKS,
            PreferenceDefaults.PLACES_AND_LANDMARKS
        ) ?: PreferenceDefaults.PLACES_AND_LANDMARKS

    /**
     * Whether this feature is one the "Bus and tram stops" setting silences, given that setting's
     * current value. Read on each callout rather than captured once, so turning the switch off
     * takes effect on the next location update rather than at the next grid rebuild.
     */
    private fun suppressedAsBusOrTramStop(feature: MvtFeature): Boolean {
        if (feature.featureValue !in busAndTramStopValues) return false
        return !(preferences?.getBoolean(
            PreferenceKeys.BUS_AND_TRAM_STOPS,
            PreferenceDefaults.BUS_AND_TRAM_STOPS
        ) ?: PreferenceDefaults.BUS_AND_TRAM_STOPS)
    }

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
        // Not on a train, for the same reason the crossings aren't (see
        // onARoadRatherThanATrain): the
        // road matcher still latches onto whatever runs alongside the line, and this then walks a
        // hundred metres up that road and announces its bus stops to a rail passenger. The stops
        // that matter on a train are the station stops on the line itself - see
        // travellingReverseGeocodeName. Suppressed shortly after losing rail lock too, since
        // probablyOnTrain() can flicker false for an instant mid-journey.
        if (userGeometry.probablyOnTrain() || recentlyOnTrain(userGeometry)) return null
        val way = userGeometry.mapMatchedWay ?: return null

        val found = transitStopAhead(userGeometry, way) ?: return null
        val stopFeature = found.feature.feature ?: return null
        if (suppressedAsBusOrTramStop(stopFeature)) return null
        val stopText = stopFeature.getText(localized)
        if (stopText.generic) return null

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
            calloutHistory = alongWayCalloutHistory,
            // The stop itself rather than its text, so that the whole approach is one
            // announcement: the callout fires when the stop first comes within range and stays
            // quiet for the rest of the way in, however the text is worded.
            dedupText = "stop|${stopFeature.osmId}",
        )

        // Whether this has already been said. Recorded eagerly rather than left to the speak path,
        // which only sees a callout that is returned standalone - this one is commonly merged into
        // another's positionedStrings, the same reason buildCalloutForVehicleLandmark adds here.
        if (alongWayCalloutHistory.find(callout)) return null
        alongWayCalloutHistory.add(callout)
        notableVehicleEventTracker.recordEvent(userGeometry.timestampMilliseconds)
        return callout
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
        // Only as a precondition - which kerb is near is read per-Way off each candidate below.
        if (cursor.forwards == null) return null
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
                // The recorded side is relative to the START-to-END direction of the Way the stop
                // is on, so it reads directly when travelling that way and inverts when travelling
                // back. That is the walk's direction on *that* Way, not the cursor's on the one
                // the walk started from: following a road across a Way digitised the other way
                // round flips which kerb is which.
                val sideOfTravel = if (candidate.forwards) {
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

    /**
     * Announces a river/canal or railway crossing while travelling by car/bus - these are major
     * navigation points ("Passing over Allander Water", "Passing over the railway") worth calling
     * out on their own, not just as part of a "via a bridge" road name.
     *
     * All the work is in buildCalloutForCrossingOn, which is shared with the walking and train
     * cases; what is left here is who this applies to.
     */
    private fun buildCalloutForVehicleCrossing(userGeometry: UserGeometry, gridState: GridState): TrackedCallout? {
        if (!userGeometry.inVehicle() && !recentlyInVehicle(userGeometry)) return null
        if (!onARoadRatherThanATrain(userGeometry)) return null
        val way = userGeometry.mapMatchedWay ?: return null

        val callout = buildCalloutForCrossingOn(userGeometry, gridState, way) ?: return null
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
     * worth saying, the same reasoning as for an unnamed waterway crossing.
     */
    private fun buildCalloutForTrainCrossing(userGeometry: UserGeometry, gridState: GridState): TrackedCallout? {
        if (!userGeometry.probablyOnTrain()) return null
        val railway = userGeometry.mapMatchedRailway ?: return null

        return buildCalloutForCrossingOn(userGeometry, gridState, railway)
    }

    // A crossing recorded against a Way, flattened out of its AlongWayFeature. Every crossing has
    // a real point now that the firths and bays are attached alongside the waterway lines - see
    // GridState.attachWaterPolygonCrossings - which is what let the Way-change edge trigger go.
    //
    // position is the user's relationship to the structure - see AlongWayPosition for why the raw
    // OSM brunnel value isn't good enough.
    private data class WayCrossingInfo(
        val kind: AlongWayKind,
        val name: String?,
        val position: AlongWayPosition?,
        val point: LngLatAlt
    ) {
        constructor(feature: AlongWayFeature) :
            this(feature.kind, feature.name, feature.position, feature.point)
    }


    // The along-way kinds that are crossings, as opposed to the stops - an allow-list, since the
    // kinds still to come (junctions) aren't crossings either.
    //
    // One set for road and rail rather than one each, because the two railway kinds are mirrors
    // recorded on opposite Ways: GridState.attachRailwayCrossings puts RAILWAY_CROSSING on the
    // road and ROAD_CROSSING on the railway, always as a pair. So a road Way never carries a
    // ROAD_CROSSING and a railway Way never carries a RAILWAY_CROSSING, and which of them a
    // lookup can find is already decided by the Way it is asked about.
    private val crossingKinds = setOf(
        AlongWayKind.WATERWAY_CROSSING,
        AlongWayKind.RAILWAY_CROSSING,
        AlongWayKind.ROAD_CROSSING
    )

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

    /**
     * Whether crossings read off the road matcher's Way mean anything right now.
     *
     * On a train the road matcher still latches onto whatever runs alongside the line, and those
     * roads carry the crossing records for the very railway being ridden - recordings had "Passing
     * under Milngavie Branch" and "Passing over Milngavie Branch" interleaved with "On Milngavie
     * Branch". What a passenger should hear instead comes off the line itself, in
     * buildCalloutForTrainCrossing.
     *
     * False shortly after losing rail lock too, for the same reason as the vehicle landmark
     * callouts: probablyOnTrain() can flicker false for an instant mid-journey.
     */
    private fun onARoadRatherThanATrain(userGeometry: UserGeometry) =
        !userGeometry.probablyOnTrain() && !recentlyOnTrain(userGeometry)

    /**
     * What this crossing should be called, or null if it isn't worth announcing.
     *
     * The rule differs by kind rather than by who is travelling. An unnamed waterway is nothing to
     * say - "Crossing" nothing - while an unnamed railway still is, since "Passing over the
     * railway" stands on its own. A road is named through Way.getName so it reads in the user's
     * own language, but only when it has a real name or number: getName confects something for
     * anything unnamed, and "Passing over Service that joins Lennox Park and Crossveggate" tells a
     * passenger nothing.
     */
    private fun announceableCrossing(
        feature: AlongWayFeature,
        gridState: GridState
    ): WayCrossingInfo? = when (feature.kind) {
        AlongWayKind.WATERWAY_CROSSING ->
            if (feature.name.isNullOrEmpty()) null else WayCrossingInfo(feature)

        AlongWayKind.RAILWAY_CROSSING -> WayCrossingInfo(feature)

        AlongWayKind.ROAD_CROSSING -> {
            val road = feature.feature as? Way
            if ((road == null) || ((road.name == null) && (road.ref == null))) {
                null
            } else {
                val roadName = road.getName(null, gridState, localized, true)
                if (roadName.isEmpty()) null else WayCrossingInfo(feature).copy(name = roadName)
            }
        }

        else -> null
    }

    /**
     * The callout for the next crossing along [way] that hasn't been announced yet, or null.
     *
     * One function for the road being driven or walked and for the line being ridden. The two used
     * to be written out separately, but the difference between them was never in the logic - it is
     * entirely in which Way is asked and what is recorded against it, and
     * GridState.attachRailwayCrossings records both sides of a rail/road crossing as a mirrored
     * pair for exactly that reason.
     *
     * Crossings that aren't worth announcing are stepped over rather than stopping the search, so
     * an unnamed burn immediately ahead doesn't hide the river beyond it.
     */
    private fun buildCalloutForCrossingOn(
        userGeometry: UserGeometry,
        gridState: GridState,
        way: Way
    ): TrackedCallout? {
        // In the order they will be met - measured along the road or the rails, not as the crow
        // flies.
        for (found in crossingsInReach(userGeometry, way, crossingKinds)) {
            val crossing = announceableCrossing(found.feature, gridState) ?: continue
            val callout = crossingCallout(userGeometry, crossing)
            if (alongWayCalloutHistory.find(callout)) continue
            alongWayCalloutHistory.add(callout)
            return callout
        }
        return null
    }

    /**
     * Records how far the user has moved since the previous update, which is how far back along
     * the Ways the along-way queries look - crossingsInReach for the crossings, and
     * transitStopAhead for the stops. Called once per update, before any callout is built, so
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
        alongWayCalloutHistory.trim(userGeometry)

        val previous = lastSweepLocation
        val elapsed = userGeometry.timestampMilliseconds - lastSweepTimestamp
        // A gap in the record: the two fixes are too far apart in time to say anything about how
        // the user got from one to the other, so neither the window nor the heading is derived
        // from them.
        val gap = (previous == null) || (elapsed <= 0) || (elapsed > sweepMaximumGapMilliseconds)
        sweepBehindMetres = if (gap) {
            0.0
        } else {
            // Crow-fly between the two fixes, with slack for the road not being straight between
            // them, so the window is never shorter than the road actually travelled.
            (userGeometry.ruler.distance(previous, userGeometry.location) * 1.5)
                .coerceAtMost(sweepMaximumBehindMetres)
        }
        sweepPreviousLocation = if (gap) null else previous
        lastSweepLocation = userGeometry.location
        lastSweepTimestamp = userGeometry.timestampMilliseconds
    }

    /**
     * The bearing from where the user was on the previous fix, for when the fix itself carries no
     * usable travel heading. Movement between two fixes says which way they are going just as well,
     * and this is how the transit stop sweep used to decide it before the along-way queries
     * existed.
     *
     * Null below a couple of metres of movement, where the bearing is mostly noise.
     */
    private fun sweepHeading(userGeometry: UserGeometry): Double? {
        val previous = sweepPreviousLocation ?: return null
        if (userGeometry.ruler.distance(previous, userGeometry.location) < 2.0) return null
        return userGeometry.ruler.bearing(previous, userGeometry.location)
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
     * The callout for a river/canal or railway crossing, shared between the vehicle and walking
     * paths - neither the wording nor what counts as the same crossing depends on how it is being
     * travelled.
     *
     * Placed at the crossing itself. That is what the callout is about, so it is where the audio
     * belongs and what says whether this is a crossing already announced - approaching one across
     * a Way boundary and then reaching it is one crossing, and the user's own position, which is
     * different on every fix, cannot express that.
     */
    private fun crossingCallout(
        userGeometry: UserGeometry,
        crossing: WayCrossingInfo
    ): TrackedCallout {
        val location = crossing.point
        return TrackedCallout(
            userGeometry,
            trackedText = crossing.name ?: "railway",
            location = location,
            positionedStrings = listOf(
                PositionedString(
                    text = crossingCalloutText(crossing),
                    location = location,
                    type = AudioType.STANDARD
                )
            ),
            isPoint = true,
            isGeneric = false,
            calloutHistory = alongWayCalloutHistory,
            // A bridge over open water is one crossing however many Ways it is split into, and
            // every piece carries its own recorded point - see
            // GridState.attachWaterPolygonCrossings. They are the same water by name, so the
            // radius has to span the structure rather than just the road under it.
            matchRadiusMetres = adjacentStructureMatchRadiusMetres,
        )
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
     * Announces a river/canal or railway crossing as it's passed while walking - the same landmark
     * buildCalloutForVehicleCrossing announces for car/bus travel, off the same Way and the same
     * records, since crossings are detected for any highway class (including footway/path) and not
     * just for vehicle roads.
     */
    private fun buildCalloutForWalkingCrossing(userGeometry: UserGeometry, gridState: GridState): TrackedCallout? {
        if (userGeometry.inVehicle() || recentlyInVehicle(userGeometry)) return null
        if (!onARoadRatherThanATrain(userGeometry)) return null
        val way = userGeometry.mapMatchedWay ?: return null

        return buildCalloutForCrossingOn(userGeometry, gridState, way)
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

        if (!mobilityCalloutsEnabled()) return null

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

            if (suppressedAsBusOrTramStop(feature as MvtFeature)) return@filter true

            val name = feature.getText(localized)
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
                    // onARoadRatherThanATrain), so this names the roads the line passes over/under
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
