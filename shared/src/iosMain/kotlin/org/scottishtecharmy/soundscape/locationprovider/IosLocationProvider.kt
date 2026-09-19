package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.scottishtecharmy.soundscape.geoengine.filters.KalmanLocationFilter
import platform.CoreLocation.CLActivityTypeOther
import platform.CoreLocation.CLActivityTypeOtherNavigation
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.CoreLocation.kCLLocationAccuracyNearestTenMeters
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

/** How old CLLocationManager's cached fix may be before it is ignored on start. */
private const val MAX_CACHED_FIX_AGE_SECONDS = 60.0

class IosLocationProvider : LocationProvider() {

    private val locationManager = CLLocationManager()
    private val delegate = LocationDelegate(this)

    private val filter = KalmanLocationFilter()

    init {
        locationManager.allowsBackgroundLocationUpdates = true
        start()
    }

    fun requestPermission() {
        locationManager.requestAlwaysAuthorization()
    }

    override fun start(accuracy: Accuracy) {
        locationManager.delegate = delegate

        locationManager.desiredAccuracy = when (accuracy) {
            Accuracy.High -> kCLLocationAccuracyBest
            Accuracy.Balanced -> kCLLocationAccuracyNearestTenMeters
        }

        locationManager.distanceFilter = accuracy.minimumDistanceM.toDouble()

        locationManager.pausesLocationUpdatesAutomatically = when (accuracy) {
            Accuracy.Balanced -> true
            Accuracy.High -> false
        }

        locationManager.activityType = when (accuracy) {
            Accuracy.Balanced -> CLActivityTypeOther
            Accuracy.High -> CLActivityTypeOtherNavigation
        }

        locationManager.startUpdatingLocation()
        seedFromCachedFix()
    }

    /**
     * Publishes CLLocationManager's cached fix straight away, if it is recent enough.
     *
     * Until the first delegate callback arrives, locationFlow stays null, and on a cold
     * start that can take longer than an assistant command is willing to wait — Siri
     * answering "Soundscape doesn't have your location yet" while a perfectly good
     * recent fix sat unread in the location manager.
     *
     * Bounded by age deliberately: for callouts a stale position is worse than none,
     * because describing surroundings the user has already walked away from misleads
     * rather than merely disappoints.
     */
    private fun seedFromCachedFix() {
        val cached = locationManager.location ?: return
        val ageSeconds = NSDate().timeIntervalSince1970 - cached.timestamp.timeIntervalSince1970
        if (ageSeconds in 0.0..MAX_CACHED_FIX_AGE_SECONDS) {
            onLocationUpdate(cached)
        }
    }

    fun pause() {
        locationManager.stopUpdatingLocation()
        locationManager.delegate = null
    }

    override fun destroy() {
        pause()
    }

    /**
     * Publishes a fix on both flows, mirroring the Android providers.
     *
     * CoreLocation reports an invalid course, speed, course accuracy or speed accuracy as a
     * negative sentinel rather than by a separate flag, so each one is turned into the
     * corresponding has* flag here. Without that the geoengine reads -1 as a real value: see
     * GeoEngine.createUserGeometry, where a bearing with no accuracy beside it is trusted
     * ungated, and GeoEngine.startMonitoringLocation, where the stationary detector's
     * "moving by bearing" input is false for every fix if hasBearingAccuracy never gets set.
     */
    @OptIn(ExperimentalForeignApi::class)
    internal fun onLocationUpdate(location: CLLocation) {
        val coordinate = location.coordinate.useContents {
            SoundscapeLocation(
                latitude = latitude,
                longitude = longitude,
                accuracy = location.horizontalAccuracy.toFloat(),
                bearing = location.course.toFloat(),
                bearingAccuracyDegrees = location.courseAccuracy.toFloat(),
                speed = location.speed.toFloat(),
                speedAccuracyMetersPerSecond = location.speedAccuracy.toFloat(),
                hasAccuracy = location.horizontalAccuracy >= 0,
                hasBearing = location.course >= 0,
                hasBearingAccuracy = location.courseAccuracy >= 0,
                hasSpeed = location.speed >= 0,
                hasSpeedAccuracy = location.speedAccuracy >= 0,
                timestampMilliseconds = (location.timestamp.timeIntervalSince1970 * 1000).toLong(),
            )
        }
        mutableLocationFlow.value = coordinate
        mutableFilteredLocationFlow.value = filter.filterPosition(coordinate)
    }
}

private class LocationDelegate(
    private val provider: IosLocationProvider
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        provider.onLocationUpdate(location)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        // Location errors are logged but not fatal
        println("Location error: ${didFailWithError.localizedDescription}")
    }
}
