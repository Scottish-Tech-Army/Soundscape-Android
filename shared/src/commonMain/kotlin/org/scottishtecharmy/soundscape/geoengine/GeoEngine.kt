package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation
import org.scottishtecharmy.soundscape.locationprovider.DeviceDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.scottishtecharmy.soundscape.database.local.dao.RouteDao
import org.scottishtecharmy.soundscape.geoengine.callouts.AutoCallout
import org.scottishtecharmy.soundscape.geoengine.callouts.buildAheadOfMeCallout
import org.scottishtecharmy.soundscape.geoengine.callouts.buildMyLocationCallout
import org.scottishtecharmy.soundscape.geoengine.callouts.buildNearbyMarkersCallout
import org.scottishtecharmy.soundscape.geoengine.callouts.buildWhatsAroundMeCallout
import org.scottishtecharmy.soundscape.geoengine.filters.MapMatchFilter
import org.scottishtecharmy.soundscape.geoengine.filters.TrackedCallout
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.FeatureTree
import org.scottishtecharmy.soundscape.geoengine.utils.SuperCategoryId
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.MultiGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.SoundscapeGeocoder
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.TileSearch
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import org.scottishtecharmy.soundscape.locationprovider.phoneHeldFlat
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.network.PhotonSearch
import org.scottishtecharmy.soundscape.network.VectorTileClient
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.PhotonGeocoder
import org.scottishtecharmy.soundscape.platform.currentTimeMillis
import org.scottishtecharmy.soundscape.platform.getDefaultLanguage
import org.scottishtecharmy.soundscape.preferences.PreferenceDefaults
import org.scottishtecharmy.soundscape.preferences.PreferenceKeys
import org.scottishtecharmy.soundscape.preferences.PreferencesListener
import org.scottishtecharmy.soundscape.preferences.PreferencesProvider
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.platform.getDefaultCountryCode
import org.scottishtecharmy.soundscape.utils.process
import kotlin.math.abs
import kotlin.time.measureTime


fun getPhotonLanguage(preferencesProvider: PreferencesProvider?) : String? {

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

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class GeoEngine {
    private val coroutineScope = CoroutineScope(Job())

    private var locationMonitoringJob: Job? = null
    private var audioEngineUpdateJob: Job? = null
    private var markerMonitoringJob: Job? = null

    val gridState = ProtomapsGridState()
    val settlementGrid = ProtomapsGridState(zoomLevel = 12, gridSize = 3, gridState.treeContext)

    internal lateinit var locationProvider : LocationProvider
    private lateinit var directionProvider : DirectionProvider
    private var mapMatchFilter = MapMatchFilter()

    private lateinit var localizedStrings: LocalizedStrings
    private lateinit var preferencesProvider: PreferencesProvider

    lateinit var geocoder: SoundscapeGeocoder
    lateinit var tileSearch: TileSearch

    var appInForeground = false

    private lateinit var autoCallout: AutoCallout
    private var autoCalloutDisabled = false
    fun toggleAutoCallouts() {
        autoCalloutDisabled = autoCalloutDisabled.xor(true)
    }
    private val streetPreview = StreetPreview()

    var phoneHeldFlat = false
    var lastPhoneHeading : Double? = null

    var beaconLocation: LngLatAlt? = null
    fun updateBeaconLocation(location: LngLatAlt?) {
        beaconLocation = location
    }

    var ruler = CheapRuler(0.0)

    private lateinit var analytics: Analytics
    private lateinit var listener: GeoEngineListener
    private var hasNetwork: () -> Boolean = { false }

    private fun createUserGeometry(
        location: SoundscapeLocation?,
        orientation: DeviceDirection?,
        headingMode: UserGeometry.HeadingMode,
        mapMatchFilter: MapMatchFilter? = null,
    ) : UserGeometry {

        var latLng = LngLatAlt(0.0, 0.0)
        var errorDistance = 0.0
        var errorHeading = 0.0
        if(location != null) {
            latLng = LngLatAlt(location.longitude, location.latitude)
            errorDistance = if(location.hasAccuracy) location.accuracy.toDouble() else 0.0
            errorHeading = if(location.hasBearingAccuracy) location.bearingAccuracyDegrees.toDouble() else 0.0
        }
        if(ruler.needsReplacing(latLng.latitude))
            ruler = CheapRuler(latLng.latitude)

        phoneHeldFlat = phoneHeldFlat(orientation)
        lastPhoneHeading = orientation?.headingDegrees?.toDouble()
        val phoneHeading =
            if (appInForeground or phoneHeldFlat)
                lastPhoneHeading
            else
                null

        var travelHeading: Double? = null
        if(location?.hasBearing == true) {
            if(location.hasBearingAccuracy) {
                if(location.bearingAccuracyDegrees < 45.0)
                    travelHeading = location.bearing.toDouble()
            } else {
                travelHeading = location.bearing.toDouble()
            }
        }

        var speed = 0.0
        if(location?.hasSpeed == true) {
            if(location.hasSpeedAccuracy) {
                val lowestSpeed = location.speed - location.speedAccuracyMetersPerSecond
                if(lowestSpeed > 0.1) {
                    speed = location.speed.toDouble()
                }
            } else {
                speed = location.speed.toDouble()
            }
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
            mapMatchedWay = mapMatchFilter?.matchedWay,
            mapMatchedLocation = mapMatchFilter?.matchedLocation,
            currentBeacon = beaconLocation,
            inStreetPreview = streetPreview.running,
            timestampMilliseconds = currentTimeMillis()
        )
    }

    private fun getCurrentUserGeometry(
        headingMode: UserGeometry.HeadingMode
    ) : UserGeometry {
        return createUserGeometry(
            location = locationProvider.filteredLocationFlow.value,
            orientation = directionProvider.orientationFlow.value,
            headingMode = headingMode,
            mapMatchFilter = mapMatchFilter
        )
    }

    private var recordTravel = false
    var locationRecorder: LocationRecorder? = null

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
        gridState.start(offlineExtractPath)
        settlementGrid.start(offlineExtractPath)
        tileSearch = TileSearch(offlineExtractPath, gridState, settlementGrid)

        autoCallout = AutoCallout(localizedStrings, preferencesProvider)

        val photonGeocoder = PhotonGeocoder(
            photonSearch = photonSearch,
            languageProvider = { getPhotonLanguage(preferencesProvider) },
            analyticsLogger = { name -> analytics.logEvent(name, null) },
            processor = { it.process() }
        )
        val analyticsLoggerFn = { name: String -> analytics.logEvent(name, null) }
        val processorFn: (LocationDescription) -> Unit = { it.process() }
        geocoder = MultiGeocoder(
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
            }
        )
        locationProvider = newLocationProvider
        directionProvider = newDirectionProvider

        startMonitoringLocation()

        if(streetPreviewEnabled)
            streetPreview.start()

        markerMonitoringJob?.cancel()
        markerMonitoringJob = coroutineScope.launch {
            routeDao.getAllMarkersFlow().collect { markers ->
                val featureCollection = FeatureCollection()
                for (marker in markers) {
                    val geoFeature = MvtFeature()
                    geoFeature.geometry =
                        Point(marker.longitude, marker.latitude)
                    val properties : HashMap<String, Any?> = hashMapOf()
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

    fun createSuperCategoriesSet() : Set<String> {
        val enabledCategories = mutableSetOf<String>()
        if (preferencesProvider.getBoolean(PLACES_AND_LANDMARKS_KEY, true))
            enabledCategories.add(PLACES_AND_LANDMARKS_KEY)

        if (preferencesProvider.getBoolean(MOBILITY_KEY, true))
            enabledCategories.add(MOBILITY_KEY)

        return enabledCategories
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startMonitoringLocation() {
        println("GeoEngine: startTileGridService")
        locationMonitoringJob?.cancel()
        locationMonitoringJob = coroutineScope.launch {
            locationProvider.filteredLocationFlow.collect { newLocation ->

                newLocation?.let { location ->

                    analytics.crashSetCustomKey("latitude", newLocation.latitude.toString())
                    analytics.crashSetCustomKey("longitude", newLocation.longitude.toString())

                    val updated = gridState.locationUpdate(
                        LngLatAlt(location.longitude, location.latitude),
                        createSuperCategoriesSet()
                    )
                    settlementGrid.locationUpdate(
                        LngLatAlt(location.longitude, location.latitude),
                        createSuperCategoriesSet()
                    )

                    runBlocking {
                        withContext(gridState.treeContext) {
                            locationProvider.locationFlow.value?.let { unfilteredLocation ->
                                val mapMatchTime = measureTime {
                                    mapMatchFilter.filter(
                                        LngLatAlt(unfilteredLocation.longitude, unfilteredLocation.latitude),
                                        gridState,
                                        FeatureCollection(),
                                        false
                                    )
                                }
                                val matchedWay = mapMatchFilter.matchedWay
                                println("MapMatch: $mapMatchTime to get ${matchedWay?.getName()}")
                            }
                        }
                    }

                    if(updated) {
                        analytics.logCostlyEvent("gridUpdated", null)
                        listener.tileGridUpdated()
                    }

                    if((!listener.isAudioEngineBusy() || streetPreview.running) && !autoCalloutDisabled && !listener.menuActive) {
                        val callout =
                            autoCallout.updateLocation(
                                getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto),
                                gridState,
                                settlementGrid)
                        if (callout != null) {
                            listener.speakCallout(callout, false)
                        }

                        if (recordTravel) {
                            locationRecorder?.storeLocation(location)
                        }
                    }
                }
            }
        }

        audioEngineUpdateJob?.cancel()
        audioEngineUpdateJob = coroutineScope.launch {
            var lastGeometry : UserGeometry? = null
            while(true) {
                val geometry = withTimeoutOrNull(100) {
                    combine(
                        directionProvider.orientationFlow,
                        locationProvider.filteredLocationFlow,

                        ) { orientation: DeviceDirection?, location: SoundscapeLocation? ->

                            createUserGeometry(
                                location = location,
                                orientation = orientation,
                                headingMode = UserGeometry.HeadingMode.CourseAuto
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
                if(geometry == null) {
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

    fun myLocation() : TrackedCallout? {
        analytics.logEvent("myLocation", null)
        return buildMyLocationCallout(
            userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto),
            hasValidLocation = locationProvider.hasValidLocation(),
            geocoder = geocoder,
            localizedStrings = localizedStrings,
            gridState = gridState,
        )
    }

    suspend fun searchResult(searchString: String) : List<LocationDescription>? {
        return withContext(org.scottishtecharmy.soundscape.platform.ioDispatcher) {
            return@withContext geocoder.getAddressFromLocationName(
                searchString,
                getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto).location,
                localizedStrings)
        }
    }

    fun whatsAroundMe() : TrackedCallout {
        analytics.logEvent("whatsAroundMe", null)
        return buildWhatsAroundMeCallout(
            userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.CourseAuto),
            hasValidLocation = locationProvider.hasValidLocation(),
            localizedStrings = localizedStrings,
            gridState = gridState,
        )
    }

    fun aheadOfMe() : TrackedCallout? {
        analytics.logEvent("aheadOfMe", null)
        return buildAheadOfMeCallout(
            userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.HeadAuto),
            hasValidLocation = locationProvider.hasValidLocation(),
            localizedStrings = localizedStrings,
            gridState = gridState,
        )
    }

    fun nearbyMarkers() : TrackedCallout {
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
                if(newBest != null) {
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

    fun streetPreviewGo() : List<StreetPreviewChoice> {
        return streetPreviewGoInternal()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun streetPreviewGoWander() {

        CoroutineScope(Job()).launch(gridState.treeContext) {
            val userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.Phone)
            val choices = streetPreview.getDirectionChoices(gridState, userGeometry.location)
            var heading = 0.0
            if(choices.isNotEmpty()) {
                val lastHeading = streetPreview.getLastHeading()
                heading = choices.random().heading
                if(!lastHeading.isNaN()) {
                    val trimmedChoices = mutableListOf<StreetPreviewChoice>()
                    for (choice in choices) {
                        if ((choice.heading != lastHeading) && (!choice.heading.isNaN())) {
                            trimmedChoices.add(choice)
                            if(abs(choice.heading - lastHeading) > 140.0) {
                                trimmedChoices.add(choice)
                                trimmedChoices.add(choice)
                                trimmedChoices.add(choice)
                            }
                        }
                    }
                    if(trimmedChoices.isNotEmpty()) {
                        heading = trimmedChoices.random().heading
                    }
                }
            }
            userGeometry.phoneHeading = heading
            streetPreview.go(userGeometry, gridState, locationProvider)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun streetPreviewGoInternal() : List<StreetPreviewChoice> {
        val results = runBlocking {
            withContext(gridState.treeContext) {
                val userGeometry = getCurrentUserGeometry(UserGeometry.HeadingMode.Phone)
                val newLocation = streetPreview.go(userGeometry, gridState, locationProvider)
                if(newLocation != null) {
                    streetPreview.getDirectionChoices(gridState, newLocation)
                } else {
                    streetPreview.getDirectionChoices(gridState, userGeometry.location)
                }
            }
        }
        return results
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getLocationDescription(location: LngLatAlt) : LocationDescription {

        val geocode = runBlocking {
            withContext(gridState.treeContext) {
                geocoder.getAddressFromLngLat(UserGeometry(location), localizedStrings, false)
            }
        }
        if(geocode != null) {
            if(ruler.distance(geocode.location, location) < 50.0) {
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
