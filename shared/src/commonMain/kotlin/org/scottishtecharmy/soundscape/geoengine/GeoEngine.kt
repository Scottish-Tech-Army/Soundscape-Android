package org.scottishtecharmy.soundscape.geoengine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.geoengine.callouts.AutoCallout
import org.scottishtecharmy.soundscape.geoengine.callouts.CalloutPoiSelection
import org.scottishtecharmy.soundscape.geoengine.callouts.CalloutVerbosity
import org.scottishtecharmy.soundscape.geoengine.callouts.PlacesToCallOut
import org.scottishtecharmy.soundscape.geoengine.callouts.buildAheadOfMeCallout
import org.scottishtecharmy.soundscape.geoengine.callouts.buildBeaconCallout
import org.scottishtecharmy.soundscape.geoengine.callouts.buildBeaconMoreInfoCallout
import org.scottishtecharmy.soundscape.geoengine.callouts.buildNoBeaconCallout
import org.scottishtecharmy.soundscape.geoengine.callouts.buildMyLocationCallout
import org.scottishtecharmy.soundscape.geoengine.callouts.buildNearbyMarkersCallout
import org.scottishtecharmy.soundscape.geoengine.callouts.buildWhatsAroundMeCallout
import org.scottishtecharmy.soundscape.geoengine.filters.MapMatchFilter
import org.scottishtecharmy.soundscape.geoengine.filters.RailMatchArbiter
import org.scottishtecharmy.soundscape.geoengine.filters.StationaryDetector
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Way
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.nameKeysForLanguage
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.PoiRankStrategy
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.extrapolatePositionForward
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.MultiGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.PhotonGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.SoundscapeGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.TileSearch
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.HeadHeading
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackingProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.locationprovider.isAccuracyUsable
import org.scottishtecharmy.soundscape.locationprovider.phoneHeldFlat
import org.scottishtecharmy.soundscape.network.PhotonSearch
import org.scottishtecharmy.soundscape.network.VectorTileClient
import org.scottishtecharmy.soundscape.platform.currentTimeMillis
import org.scottishtecharmy.soundscape.platform.getDefaultCountryCode
import org.scottishtecharmy.soundscape.platform.getDefaultLanguage
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesListener
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.onboarding.language.getAppLocale
import org.scottishtecharmy.soundscape.screens.onboarding.language.getSystemLocale
import org.scottishtecharmy.soundscape.screens.onboarding.language.supportedLanguages
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.utils.process
import kotlin.math.abs
import kotlin.time.measureTime


fun getPhotonLanguage(preferencesProvider: PreferencesProvider?): String? {

    var lang: String? = getDefaultLanguage()
    if (preferencesProvider != null) {
        val languageMode = preferencesProvider.getString(
            PreferenceKeys.SEARCH_LANGUAGE,
            PreferenceDefaults.SEARCH_LANGUAGE
        )
        when (languageMode) {
            "auto" -> {}
            else -> lang = languageMode
        }
    }

    when (lang) {
        "en", "fr", "de" -> {}
        else -> lang = null
    }
    return lang
}

/**
 * The tile keys of the name translation that features are called by - see
 * MvtFeature.translatedName. That's the app's own language, or the phone's where the app hasn't
 * been set to one. Where it's a language the app has no strings for, the app is in English, so
 * the names are too.
 */
fun getNameTranslationKeys(): List<String> {
    val locale = getAppLocale() ?: getSystemLocale()
    return if (supportedLanguages.any { it.code == locale.language }) {
        nameKeysForLanguage(locale.language, locale.region)
    } else {
        nameKeysForLanguage("en", null)
    }
}

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class GeoEngine {
    private val coroutineScope = CoroutineScope(Job())

    private var locationMonitoringJob: Job? = null
    private var audioEngineUpdateJob: Job? = null
    private var markerMonitoringJob: Job? = null

    val gridState = ProtomapsGridState()
    val settlementGrid = ProtomapsGridState(zoomLevel = 12, gridSize = 3, gridState.treeContext)

    internal lateinit var locationProvider: LocationProvider
    private lateinit var directionProvider: DirectionProvider
    private var headTrackingProvider: HeadTrackingProvider? = null
    private var mapMatchFilter = MapMatchFilter()
    private var railMapMatchFilter = MapMatchFilter(networkTree = TreeId.TRANSIT)
    // The two matchers can't see each other, so something has to weigh a confident railway match
    // against the road match before believing the user is on a train - see RailMatchArbiter.
    private var railMatchArbiter = RailMatchArbiter()
    // RailMatchArbiter's verdict from the most recent location update.
    private var arbitratedRailway: Way? = null
    // Whether the user has actually gone anywhere lately, which instantaneous speed cannot say -
    // see StationaryDetector. Driven below, ahead of the matchers, since the arbiter reads it.
    private var stationaryDetector = StationaryDetector()
    private var userStationary = false

    // Running total of the time fixes have been arriving too inaccurate to place, and the
    // bookkeeping behind it - see UserGeometry.unobservedMillis and
    // AutoCallout.discountUninformativeTime.
    private var unobservedMillis = 0L
    private var lastUsableFixMillis: Long? = null
    private var blindSinceLastUsableFix = false

    fun setHeadTrackingProvider(provider: HeadTrackingProvider?) {
        headTrackingProvider = provider
    }

    private lateinit var localizedStrings: LocalizedStrings
    private lateinit var preferencesProvider: PreferencesProvider

    lateinit var geocoder: SoundscapeGeocoder
    private lateinit var multiGeocoder: MultiGeocoder
    lateinit var tileSearch: TileSearch

    var appInForeground = false

    private lateinit var autoCallout: AutoCallout

    private val streetPreview = StreetPreview()

    var phoneHeldFlat = false
    var lastPhoneHeading: Double? = null

    var beaconLocation: LngLatAlt? = null
    fun updateBeaconLocation(location: LngLatAlt?) {
        beaconLocation = location
    }

    var ruler = CheapRuler(0.0)

    private lateinit var analytics: Analytics
    private lateinit var listener: GeoEngineListener
    private var hasNetwork: () -> Boolean = { false }

    private fun speedFromLocation(location: SoundscapeLocation?): Double {
        var speed = 0.0
        if (location?.hasSpeed == true) {
            if (location.hasSpeedAccuracy) {
                val lowestSpeed = location.speed - location.speedAccuracyMetersPerSecond
                if (lowestSpeed > 0.1) {
                    speed = location.speed.toDouble()
                }
            } else {
                speed = location.speed.toDouble()
            }
        }
        return speed
    }

    private fun createUserGeometry(
        location: SoundscapeLocation?,
        orientation: DeviceDirection?,
        headingMode: UserGeometry.HeadingMode,
        mapMatchFilter: MapMatchFilter? = null,
        headHeading: Double? = null,
    ): UserGeometry {

        var latLng = LngLatAlt(0.0, 0.0)
        var errorDistance = 0.0
        var errorHeading = 0.0
        if (location != null) {
            latLng = LngLatAlt(location.longitude, location.latitude)
            errorDistance = if (location.hasAccuracy) location.accuracy.toDouble() else 0.0
            errorHeading =
                if (location.hasBearingAccuracy) location.bearingAccuracyDegrees.toDouble() else 0.0
        }
        if (ruler.needsReplacing(latLng.latitude))
            ruler = CheapRuler(latLng.latitude)

        phoneHeldFlat = phoneHeldFlat(orientation)
        lastPhoneHeading = orientation?.headingDegrees?.toDouble()
        val phoneHeading =
            if (appInForeground or phoneHeldFlat)
                lastPhoneHeading
            else
                null

        var travelHeading: Double? = null
        if (location?.hasBearing == true) {
            if (location.hasBearingAccuracy) {
                if (location.bearingAccuracyDegrees < 45.0)
                    travelHeading = location.bearing.toDouble()
            } else {
                travelHeading = location.bearing.toDouble()
            }
        }

        val speed = speedFromLocation(location)

        // See extrapolatePositionForward - keeps spatialized audio from snapping to a new azimuth
        // every time a fresh fix lands, by projecting this fix forward using its speed/heading.
        if (location != null) {
            latLng = extrapolatePositionForward(
                latLng, ruler, speed, travelHeading, location.timestampMilliseconds, currentTimeMillis()
            )
        }

        return UserGeometry(
            location = latLng,
            errorDistance = errorDistance,
            errorHeading = errorHeading,
            phoneHeading = phoneHeading,
            fovDistance = 50.0,
            speed = speed,
            headingMode = headingMode,
            ruler = ruler,
            travelHeading = travelHeading,
            headHeading = headHeading,
            mapMatchedWay = mapMatchFilter?.matchedWay,
            mapMatchedLocation = mapMatchFilter?.matchedLocation,
            // Decided by RailMatchArbiter when the filters ran, since it needs to compare the two
            // matchers against each other rather than trust the railway one on its own.
            mapMatchedRailway = arbitratedRailway,
            currentBeacon = beaconLocation,
            inStreetPreview = streetPreview.running,
            timestampMilliseconds = currentTimeMillis(),
            unobservedMillis = unobservedMillis,
            // Also decided when the filters ran, and for the same reason: a UserGeometry is one
            // location update, and whether the user has gone anywhere is a question about the last
            // minute of them. See StationaryDetector.
            stationary = userStationary,
            stationaryMillis = stationaryDetector.stationaryMillis
        )
    }

    private fun getCurrentUserGeometry(
        headingMode: UserGeometry.HeadingMode
    ): UserGeometry {
        return createUserGeometry(
            location = locationProvider.filteredLocationFlow.value,
            orientation = directionProvider.orientationFlow.value,
            headingMode = headingMode,
            mapMatchFilter = mapMatchFilter
        )
    }

    private var recordTravel = false
    var locationRecorder: LocationRecorder? = null

    // Whether any location has yet made it past the accuracy gate in startMonitoringLocation - see
    // isAccuracyUsable for why the first fix is let through however inaccurate it is.
    private var haveUsableLocation = false

    fun updateRecordingState(recordState: Boolean) {
        recordTravel = recordState
    }

    private lateinit var preferencesListener: PreferencesListener

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(
        newLocationProvider: LocationProvider,
        newDirectionProvider: DirectionProvider,
        listener: GeoEngineListener,
        localizedStrings: LocalizedStrings,
        preferencesProvider: PreferencesProvider,
        analytics: Analytics,
        tileClient: VectorTileClient,
        routeDao: RouteDao,
        offlineExtractPath: String,
        hasNetwork: () -> Boolean,
        photonSearch: PhotonSearch,
        platformGeocoder: SoundscapeGeocoder?,
        streetPreviewEnabled: Boolean,
    ) {
        this.listener = listener
        this.localizedStrings = localizedStrings
        this.preferencesProvider = preferencesProvider
        PlacesToCallOut.migrate(preferencesProvider)
        CalloutVerbosity.migrate(preferencesProvider)
        this.analytics = analytics
        this.hasNetwork = hasNetwork

        recordTravel = preferencesProvider.getBoolean(
            PreferenceKeys.RECORD_TRAVEL,
            PreferenceDefaults.RECORD_TRAVEL
        )
        updateMeasurementUnits(preferencesProvider)

        preferencesListener = PreferencesListener { key ->
            if (key == PreferenceKeys.RECORD_TRAVEL) {
                println("GeoEngine: RECORD_TRAVEL changed")
                recordTravel = preferencesProvider.getBoolean(
                    PreferenceKeys.RECORD_TRAVEL,
                    PreferenceDefaults.RECORD_TRAVEL
                )
            } else if (key == PreferenceKeys.MEASUREMENT_UNITS) {
                updateMeasurementUnits(preferencesProvider)
            } else if ((key == PreferenceKeys.CALLOUT_VERBOSITY) ||
                (key == PreferenceKeys.PLACES_TO_CALL_OUT)
            ) {
                // TreeId.SELECTED_SUPER_CATEGORIES holds what these two settings allow, so it
                // has to be built again - but only it: the tiles, ways and intersections behind
                // it are unchanged.
                gridState.updatePoiSelection(calloutPoiSelection())
            }
        }
        preferencesProvider.addListener(preferencesListener)

        val analyticsAdapter = GridStateAnalytics { name ->
            analytics.logCostlyEvent(name, null)
        }
        gridState.tileClient = tileClient
        settlementGrid.tileClient = tileClient
        gridState.analytics = analyticsAdapter
        settlementGrid.analytics = analyticsAdapter
        val nameKeys = getNameTranslationKeys()
        gridState.nameKeys = nameKeys
        settlementGrid.nameKeys = nameKeys
        gridState.start(offlineExtractPath)
        settlementGrid.start(offlineExtractPath)
        // The high-zoom tiles gridState is built from don't carry the "place" layer, so it can't
        // see settlements at all - give it a way to ask the low-zoom grid, which it has no other
        // reference to. Used when associating POIs with an address at tile load time.
        gridState.settlementNameProvider = { location ->
            nearestSettlement(settlementGrid, location).displayName
        }
        tileSearch = TileSearch(offlineExtractPath, gridState, settlementGrid)

        autoCallout = AutoCallout(localizedStrings, preferencesProvider)

        val photonGeocoder = PhotonGeocoder(
            photonSearch = photonSearch,
            languageProvider = { getPhotonLanguage(preferencesProvider) },
            analyticsLogger = { name -> analytics.logEvent(name, null) },
            processor = { it.process(localizedStrings) }
        )
        val analyticsLoggerFn = { name: String -> analytics.logEvent(name, null) }
        val processorFn: (LocationDescription) -> Unit = { it.process(localizedStrings) }
        multiGeocoder = MultiGeocoder(
            gridState = gridState,
            settlementState = settlementGrid,
            tileSearch = tileSearch,
            photonGeocoder = photonGeocoder,
            platformGeocoder = platformGeocoder,
            analyticsLogger = analyticsLoggerFn,
            processor = processorFn,
            hasNetwork = hasNetwork,
            geocoderMode = {
                preferencesProvider.getString(
                    PreferenceKeys.GEOCODER_MODE,
                    PreferenceDefaults.GEOCODER_MODE
                )
            },
            poiStrategy = {
                PoiRankStrategy.fromPreference(
                    preferencesProvider.getString(
                        PreferenceKeys.POI_RANK_STRATEGY,
                        PreferenceDefaults.POI_RANK_STRATEGY
                    )
                )
            }
        )
        geocoder = multiGeocoder
        locationProvider = newLocationProvider
        directionProvider = newDirectionProvider

        startMonitoringLocation()

        if (streetPreviewEnabled)
            streetPreview.start()

        markerMonitoringJob?.cancel()
        markerMonitoringJob = coroutineScope.launch {
            routeDao.getAllMarkersFlow().collect { markers ->
                val featureCollection = FeatureCollection()
                for (marker in markers) {
                    val geoFeature = MvtFeature()
                    geoFeature.geometry =
                        Point(marker.longitude, marker.latitude)
                    val properties: HashMap<String, Any?> = hashMapOf()
                    geoFeature.name = marker.name
                    properties["description"] = marker.fullAddress
                    geoFeature.superCategory = SuperCategoryId.MARKER
                    geoFeature.properties = properties
                    featureCollection.addFeature(geoFeature)
                }
                runBlocking {
                    withContext(gridState.treeContext) {
                        gridState.markerTree = FeatureTree(featureCollection)
                        println("GeoEngine: Marker tree size ${featureCollection.features.size}")
                    }
                }
            }
        }
    }

    fun stop() {
        streetPreview.stop()

        locationMonitoringJob?.cancel()
        audioEngineUpdateJob?.cancel()
        markerMonitoringJob?.cancel()

        settlementGrid.stop()
        gridState.stop()
        locationProvider.destroy()
        directionProvider.destroy()
        preferencesProvider.removeListener(preferencesListener)
    }

    /**
     * Called when the on-disk offline map extracts have changed (a download completed, or an
     * extract was deleted) so that both grids pick up the change on their next location update
     * instead of only opportunistically the next time the user crosses a grid boundary.
     */
    fun refreshOfflineMaps() {
        gridState.refreshOfflineMaps()
        settlementGrid.refreshOfflineMaps()
        tileSearch.refreshOfflineMaps()
    }

    /** What the walking POI callouts may announce - see [CalloutPoiSelection]. */
    fun calloutPoiSelection() = CalloutPoiSelection.read(preferencesProvider)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startMonitoringLocation() {
        println("GeoEngine: startTileGridService")
        locationMonitoringJob?.cancel()
        locationMonitoringJob = coroutineScope.launch {
            locationProvider.filteredLocationFlow.collect { newLocation ->

                newLocation?.let { location ->

                    // GPS point recording is a data-capture concern, not an audio one - it must not
                    // be skipped just because a callout (e.g. from "My Location") is being spoken,
                    // otherwise repeatedly triggering callouts silently drops recorded track points.
                    // It also deliberately runs ahead of the accuracy gate below and records every
                    // fix, including the ones the geoengine goes on to reject: a recording made in
                    // a tunnel is only useful for diagnosing what happened there if the bad fixes
                    // are actually in it.
                    //
                    // The unfiltered fix, not the filtered one this flow carries. Recordings exist
                    // to be replayed, and only the raw stream can reconstruct both of the streams
                    // below: the Kalman filter is reproducible from a raw fix, but nothing
                    // recovers a raw fix from a smoothed one, and the smoothed position is the one
                    // thing StationaryDetector and MapMatchFilter must not be given (see the
                    // unfilteredLocation block below). Recording the filtered stream is what
                    // recorder v1 did - see GpxRecorder.RECORDER_VERSION and GpxLocationStream.
                    if (recordTravel) {
                        // Both flows are written together by every provider, raw first, so the
                        // raw counterpart of this fix is already in place. Falling back to the
                        // filtered fix only matters for a provider that publishes one flow and
                        // not the other, where a smoothed point still beats no recording at all.
                        locationRecorder?.storeLocation(
                            locationProvider.locationFlow.value ?: location
                        )
                    }

                    // A fix too inaccurate to say which street the user is on is worse than no fix
                    // at all - map matching, callouts and the grid all take a location at face
                    // value - so hold the last good one instead. See isAccuracyUsable. The very
                    // first fix is taken whatever it reports, since a rough position beats none at
                    // all. This deliberately doesn't gate the flows themselves: the map dot and the
                    // audio engine still follow the raw fix.
                    if (!isAccuracyUsable(location) && haveUsableLocation) {
                        // Fixes are arriving, they just can't be placed. That's the geoengine
                        // going blind rather than idle, and the two mean opposite things to the
                        // callout sticky windows - see AutoCallout.discountUninformativeTime.
                        blindSinceLastUsableFix = true
                        return@let
                    }
                    haveUsableLocation = true

                    val nowMillis = currentTimeMillis()
                    if (blindSinceLastUsableFix) {
                        lastUsableFixMillis?.let { unobservedMillis += nowMillis - it }
                        blindSinceLastUsableFix = false
                    }
                    lastUsableFixMillis = nowMillis

                    analytics.crashSetCustomKey("latitude", newLocation.latitude.toString())
                    analytics.crashSetCustomKey("longitude", newLocation.longitude.toString())

                    // The settlement grid goes first: gridState's load-time POI address pass
                    // asks it which settlement each POI is in, so it has to already hold the
                    // settlements covering this location by the time gridState rebuilds.
                    settlementGrid.locationUpdate(
                        LngLatAlt(location.longitude, location.latitude),
                        calloutPoiSelection(),
                        localizedStrings
                    )
                    val updated = gridState.locationUpdate(
                        LngLatAlt(location.longitude, location.latitude),
                        calloutPoiSelection(),
                        localizedStrings
                    )

                    runBlocking {
                        withContext(gridState.treeContext) {
                            locationProvider.locationFlow.value?.let { unfilteredLocation ->
                                val unfilteredSpeed = speedFromLocation(unfilteredLocation)

                                // Ahead of both matchers, because RailMatchArbiter reads the
                                // verdict below. MvtTileTest.testMovingGrid mirrors this ordering
                                // by hand - if the two drift apart the replays stop reflecting
                                // what the app does, silently.
                                //
                                // The unfiltered flow, deliberately: the same stream the matchers
                                // are fed, and the one the detector's thresholds were measured on.
                                // The Kalman-filtered position has had exactly the jitter the
                                // detector measures smoothed out of it.
                                //
                                // The course flag is the GPS course and nothing else - not the
                                // phone's compass, not the head tracker. Someone standing still
                                // holding the phone up to read the screen has a rock-steady
                                // compass heading and has gone nowhere. Stricter than the
                                // travelHeading gate below, which trusts an ungated bearing: a
                                // bearing with no accuracy beside it is fine to steer audio with,
                                // but says nothing about whether the user is moving.
                                userStationary = stationaryDetector.update(
                                    LngLatAlt(
                                        unfilteredLocation.longitude,
                                        unfilteredLocation.latitude
                                    ),
                                    if (unfilteredLocation.hasAccuracy)
                                        unfilteredLocation.accuracy.toDouble()
                                    else
                                        null,
                                    unfilteredLocation.hasBearing &&
                                        unfilteredLocation.hasBearingAccuracy &&
                                        (unfilteredLocation.bearingAccuracyDegrees < 45.0),
                                    nowMillis
                                )

                                val mapMatchTime = measureTime {
                                    mapMatchFilter.filter(
                                        LngLatAlt(
                                            unfilteredLocation.longitude,
                                            unfilteredLocation.latitude
                                        ),
                                        gridState,
                                        FeatureCollection(),
                                        false,
                                        localizedStrings,
                                        unfilteredSpeed > UserGeometry.VEHICLE_SPEED_THRESHOLD_MPS
                                    )
                                }
                                val matchedWay = mapMatchFilter.matchedWay
                                println("MapMatch: $mapMatchTime to get ${matchedWay?.getName(strings = localizedStrings)}")

                                railMapMatchFilter.filter(
                                    LngLatAlt(
                                        unfilteredLocation.longitude,
                                        unfilteredLocation.latitude
                                    ),
                                    gridState,
                                    FeatureCollection(),
                                    false,
                                    localizedStrings
                                )

                                // Both matchers have now run for this location, so the railway
                                // match can be weighed against the road one. The speed goes with
                                // them: a ride can only end somewhere the train has slowed enough
                                // to be got off at.
                                arbitratedRailway = railMatchArbiter.update(
                                    mapMatchFilter, railMapMatchFilter, unfilteredSpeed,
                                    userStationary
                                )
                            }
                        }
                    }

                    if (updated) {
                        analytics.logCostlyEvent("gridUpdated", null)
                        listener.tileGridUpdated()
                    }

                    if ((!listener.isAudioEngineBusy() || streetPreview.running) && !listener.menuActive) {
                        val callout =
                            autoCallout.updateLocation(
                                getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto),
                                gridState,
                                settlementGrid
                            )
                        if (callout != null) {
                            listener.speakCallout(callout, false)
                        }
                    }
                }
            }
        }

        audioEngineUpdateJob?.cancel()
        audioEngineUpdateJob = coroutineScope.launch {
            var lastGeometry: UserGeometry? = null
            while (true) {
                val geometry = withTimeoutOrNull(100) {
                    val headFlow =
                        headTrackingProvider?.headHeadingFlow ?: flowOf<HeadHeading?>(null)
                    combine(
                        directionProvider.orientationFlow,
                        locationProvider.filteredLocationFlow,
                        headFlow,
                    ) { orientation: DeviceDirection?, location: SoundscapeLocation?, head: HeadHeading? ->

                        createUserGeometry(
                            location = location,
                            orientation = orientation,
                            headingMode = UserGeometry.HeadingMode.CourseAuto,
                            headHeading = head?.degrees,
                        )

                    }.collect { geometry ->
                        lastGeometry = geometry
                        listener.updateAudioEngineGeometry(geometry)
                        checkStreetPreviewBestChoice(
                            listener.getStreetPreviewChoices(),
                            geometry.phoneHeading
                        )
                    }
                }
                if (geometry == null) {
                    lastGeometry?.let { last ->
                        if (appInForeground or phoneHeldFlat)
                            last.phoneHeading = lastPhoneHeading
                        else
                            last.phoneHeading = null

                        listener.updateAudioEngineGeometry(last)
                        checkStreetPreviewBestChoice(
                            listener.getStreetPreviewChoices(),
                            last.phoneHeading
                        )
                    }
                }
            }
        }
    }

    fun myLocation(): TrackedCallout? {
        analytics.logEvent("myLocation", null)
        return buildMyLocationCallout(
            userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto),
            hasValidLocation = locationProvider.hasValidLocation(),
            geocoder = geocoder,
            localizedStrings = localizedStrings,
            gridState = gridState,
        )
    }

    suspend fun searchResult(searchString: String): List<LocationDescription>? {
        return withContext(org.scottishtecharmy.soundscape.platform.ioDispatcher) {
            return@withContext geocoder.getAddressFromLocationName(
                searchString,
                getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto).location,
                localizedStrings
            )
        }
    }

    fun whatsAroundMe(): TrackedCallout {
        analytics.logEvent("whatsAroundMe", null)
        return buildWhatsAroundMeCallout(
            userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto),
            hasValidLocation = locationProvider.hasValidLocation(),
            localizedStrings = localizedStrings,
            gridState = gridState,
        )
    }

    fun aheadOfMe(): TrackedCallout? {
        analytics.logEvent("aheadOfMe", null)
        return buildAheadOfMeCallout(
            userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.HeadAuto),
            hasValidLocation = locationProvider.hasValidLocation(),
            localizedStrings = localizedStrings,
            gridState = gridState,
        )
    }

    /**
     * The beacon's distance on demand, for the "Call out Beacon" action on the home screen's
     * beacon card. Unlike the automatic updates this ignores the DISTANCE_TO_BEACON preference
     * and the update throttle - the user just asked for it.
     */
    fun calloutBeacon(): TrackedCallout? {
        analytics.logEvent("calloutBeacon", null)
        val userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto)
        return buildBeaconCallout(
            userGeometry = userGeometry,
            localizedStrings = localizedStrings,
        ) ?: buildNoBeaconCallout(userGeometry, localizedStrings)
    }

    /**
     * The beacon's name, distance, direction and - when the offline geocoder knows it - street
     * address, for the "More Info" action on the home screen's beacon card and the audio menu.
     * [beaconName] is null when nothing is playing, which says "No beacon active".
     */
    suspend fun beaconMoreInfo(beaconName: String?): TrackedCallout? {
        analytics.logEvent("beaconMoreInfo", null)
        val userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto)
        val beacon = userGeometry.currentBeacon
        if (beaconName == null || beacon == null) {
            return buildNoBeaconCallout(userGeometry, localizedStrings)
        }
        return buildBeaconMoreInfoCallout(
            userGeometry = userGeometry,
            localizedStrings = localizedStrings,
            beaconName = beaconName,
            address = getOfflineAddress(beacon),
        )
    }

    fun nearbyMarkers(): TrackedCallout {
        analytics.logEvent("nearbyMarkers", null)
        return buildNearbyMarkersCallout(
            userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto),
            hasValidLocation = locationProvider.hasValidLocation(),
            localizedStrings = localizedStrings,
            gridState = gridState,
        )
    }

    var lastGoTime = 0L
    private var bestChoiceAnnouncementPending = false

    private fun checkStreetPreviewBestChoice(
        choices: List<StreetPreviewChoice>,
        phoneHeading: Double?
    ) {
        if (streetPreview.running && choices.isNotEmpty() && phoneHeading != null) {
            val now = currentTimeMillis()
            if (now - lastGoTime > 2000) {
                val newBest = streetPreview.updateBestChoice(choices, phoneHeading)
                if (newBest != null) {
                    listener.updateStreetPreviewBestChoice(newBest)
                    bestChoiceAnnouncementPending = true
                }
            }

            if (bestChoiceAnnouncementPending && !listener.isAudioEngineBusy()) {
                val currentBest = listener.getStreetPreviewBestChoice()
                if (currentBest != null) {
                    listener.announceStreetPreviewBestChoice(currentBest)
                    bestChoiceAnnouncementPending = false
                }
            }
        }
    }

    fun recomputeStreetPreviewBestChoice() {
        streetPreview.resetBestChoice()
        bestChoiceAnnouncementPending = false
        lastGoTime = currentTimeMillis()
        checkStreetPreviewBestChoice(listener.getStreetPreviewChoices(), lastPhoneHeading)
    }

    fun streetPreviewGo(): List<StreetPreviewChoice> {
        return streetPreviewGoInternal()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun streetPreviewGoWander() {

        CoroutineScope(Job()).launch(gridState.treeContext) {
            val userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.Phone)
            val choices = streetPreview.getDirectionChoices(gridState, userGeometry.location, localizedStrings)
            var heading = 0.0
            if (choices.isNotEmpty()) {
                val lastHeading = streetPreview.getLastHeading()
                heading = choices.random().heading
                if (!lastHeading.isNaN()) {
                    val trimmedChoices = mutableListOf<StreetPreviewChoice>()
                    for (choice in choices) {
                        if ((choice.heading != lastHeading) && (!choice.heading.isNaN())) {
                            trimmedChoices.add(choice)
                            if (abs(choice.heading - lastHeading) > 140.0) {
                                trimmedChoices.add(choice)
                                trimmedChoices.add(choice)
                                trimmedChoices.add(choice)
                            }
                        }
                    }
                    if (trimmedChoices.isNotEmpty()) {
                        heading = trimmedChoices.random().heading
                    }
                }
            }
            userGeometry.phoneHeading = heading
            streetPreview.go(userGeometry, gridState, locationProvider, localizedStrings)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun streetPreviewGoInternal(): List<StreetPreviewChoice> {
        val results = runBlocking {
            withContext(gridState.treeContext) {
                val userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.Phone)
                val newLocation = streetPreview.go(userGeometry, gridState, locationProvider, localizedStrings)
                if (newLocation != null) {
                    streetPreview.getDirectionChoices(gridState, newLocation, localizedStrings)
                } else {
                    streetPreview.getDirectionChoices(gridState, userGeometry.location, localizedStrings)
                }
            }
        }
        return results
    }

    /**
     * The offline geocoder's full address for a point, including a house number where the tile
     * data supports deriving one. This is the suspend counterpart of [getLocationDescription],
     * which wraps the same work in runBlocking and so can't be called from a composition.
     *
     * Deliberately the offline geocoder rather than whichever one [MultiGeocoder] would pick: it's
     * used for places which are already in the loaded grid, where going to the network would be
     * both slower and less relevant, and it keeps working with no signal.
     *
     * getAddressForFeature rather than getAddressFromLngLat: the caller is describing somewhere
     * which already has a name, so an answer of "the nearest named feature" would just hand back
     * that same name.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getOfflineAddress(location: LngLatAlt): LocationDescription? {
        return withContext(gridState.treeContext) {
            multiGeocoder.offlineGeocoder.getAddressForFeature(
                UserGeometry(location),
                localizedStrings,
                ignoreHouseNumbers = false
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getLocationDescription(location: LngLatAlt): LocationDescription {

        val geocode = runBlocking {
            withContext(gridState.treeContext) {
                geocoder.getAddressFromLngLat(UserGeometry(location), localizedStrings, false)
            }
        }
        if (geocode != null) {
            if (ruler.distance(geocode.location, location) < 50.0) {
                geocode.location = location
                return geocode
            }
        }
        return LocationDescription(
            name = "New location",
            location = location
        )
    }

    companion object {
        private const val TAG = "GeoEngine"
    }
}

fun updateMeasurementUnits(preferencesProvider: PreferencesProvider) {
    val unitsString = preferencesProvider.getString(
        PreferenceKeys.MEASUREMENT_UNITS,
        PreferenceDefaults.MEASUREMENT_UNITS
    )
    if (unitsString == "Auto") {
        val country = getDefaultCountryCode()
        val imperialCountries = listOf("US", "LR", "MM")
        metric = !imperialCountries.contains(country.uppercase())
    } else
        metric = (unitsString == "Metric")
}
