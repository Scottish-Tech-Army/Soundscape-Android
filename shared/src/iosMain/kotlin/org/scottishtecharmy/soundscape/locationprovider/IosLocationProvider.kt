package org.scottishtecharmy.soundscape.locationprovider

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLDistanceFilterNone
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject

class IosLocationProvider : LocationProvider() {

    private val locationManager = CLLocationManager()
    private val delegate = LocationDelegate(this)

    init {
        locationManager.delegate = delegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = kCLDistanceFilterNone
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
    }

    override fun destroy() {
        locationManager.stopUpdatingLocation()
        locationManager.delegate = null
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
