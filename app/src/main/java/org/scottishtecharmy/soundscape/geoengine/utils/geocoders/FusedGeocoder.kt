package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.data.LocationType
import org.scottishtecharmy.soundscape.utils.containsNumber

/**
 * The FusedGeocoder uses Photon and Android geocoders together and picks the best results. The
 * Android geocoder works best for street addresses and individual businesses, but for everything
 * else photon is better.
 */
class FusedGeocoder(applicationContext: Context,
                    val gridState: GridState) : SoundscapeGeocoder() {

    private lateinit var androidGeocoder: AndroidGeocoder
    private val photonGeocoder = PhotonGeocoder(applicationContext)
    private val geocoderList: List<SoundscapeGeocoder>

    init {
        if(AndroidGeocoder.enabled) {
            androidGeocoder = AndroidGeocoder(applicationContext)
            geocoderList = listOf(androidGeocoder, photonGeocoder)
        }
        else
            geocoderList = listOf(photonGeocoder)
    }

    override suspend fun getAddressFromLocationName(locationName: String,
                                                    nearbyLocation: LngLatAlt,
                                                    localizedContext: Context?) : List<LocationDescription>? {

        val deferredResults = geocoderList.map { geocoder ->
            coroutineScope {
                async {
                    geocoder.getAddressFromLocationName(locationName, nearbyLocation, localizedContext)
                }
            }
        }

        val geocoderResults = deferredResults.awaitAll()

        val results: MutableList<LocationDescription> = mutableListOf()
        // If we have any results from the Android geocoder that include the street number, then
        // that's a direct hit and we should use that.
        val androidResults = if(geocoderList.size > 1) geocoderResults[0] else null
        var streetResult: LocationDescription? = null
        if(androidResults != null) {
            for (androidResult in androidResults) {
                if(androidResult.locationType == LocationType.StreetNumber) {
                    streetResult = androidResult
                    // If the search string contained a number then we assume that it was a street
                    // number and so copy over the street name as the name of the location
                    if(locationName.containsNumber())
                        streetResult.name = streetResult.description?.substringBefore('\n') ?: streetResult.name

                    results.add(androidResult)
                    break
                }
            }
        }
        val photonResults = if(geocoderList.size > 1) geocoderResults[1] else geocoderResults[0]
        if(photonResults != null) {
            for (photonResult in photonResults) {
                if(streetResult != null) {
                    // Check to see if Photon has returned the same place
                    if(photonResult.locationType == LocationType.StreetNumber) {
                        if (gridState.ruler.distance(
                                streetResult.location,
                                photonResult.location
                            ) < 100.0
                        ) {
                            // Copy over the photon result name - if we searched for a POI that
                            // Photon knows about then this will fill it in correctly
                            streetResult.name = photonResult.name
                            streetResult.location = photonResult.location
                            continue
                        }
                    }
                }
                results.add(photonResult)
            }
        }

        return results
    }

    override suspend fun getAddressFromLngLat(userGeometry: UserGeometry, localizedContext: Context?) : LocationDescription? {
        val deferredResults = geocoderList.map { geocoder ->
            coroutineScope {
                async {
                    geocoder.getAddressFromLngLat(userGeometry, localizedContext)
                }
            }
        }

        val geocoderResults = deferredResults.awaitAll()

        // If we have any results from the Android geocoder that include the street number, then
        // that's a direct hit and we should use that.
        val androidResult = if(geocoderList.size > 1) geocoderResults[0] else null
        if (androidResult != null) {
            if (androidResult.locationType == LocationType.StreetNumber) {
                return androidResult
            }
        }

        return if(geocoderList.size > 1) geocoderResults[1] else geocoderResults[0]
    }
}