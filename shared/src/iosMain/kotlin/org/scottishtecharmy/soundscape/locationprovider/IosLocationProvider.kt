package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLActivityTypeOther
import platform.CoreLocation.CLActivityTypeOtherNavigation
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.CoreLocation.kCLLocationAccuracyNearestTenMeters
import platform.Foundation.NSError
import platform.darwin.NSObject

class IosLocationProvider : LocationProvider() {

    private val locationManager = CLLocationManager()
    private val delegate = LocationDelegate(this)

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
    }

    fun pause() {
        locationManager.stopUpdatingLocation()
        locationManager.delegate = null
    }

    override fun destroy() {
        pause()
    }

    @OptIn(ExperimentalForeignApi::class)
    internal fun onLocationUpdate(location: CLLocation) {
        val coordinate = location.coordinate.useContents {
            SoundscapeLocation(
                latitude = latitude,
                longitude = longitude,
                accuracy = location.horizontalAccuracy.toFloat(),
                bearing = location.course.toFloat(),
                speed = location.speed.toFloat(),
                hasAccuracy = location.horizontalAccuracy >= 0,
                hasBearing = location.course >= 0,
                hasSpeed = location.speed >= 0,
                timestampMilliseconds = (location.timestamp.timeIntervalSince1970 * 1000).toLong(),
            )
        }
        mutableLocationFlow.value = coordinate
        mutableFilteredLocationFlow.value = coordinate
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
