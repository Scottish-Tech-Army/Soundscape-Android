package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.data.LocationType
import org.scottishtecharmy.soundscape.utils.containsNumber

/**
 * The FusedGeocoder uses Photon and platform geocoders together and picks the best results. The
 * platform geocoder works best for street addresses and individual businesses, but for everything
 * else photon is better.
 */
class FusedGeocoder(
    val gridState: GridState,
    photonGeocoder: PhotonGeocoder,
    platformGeocoder: SoundscapeGeocoder? = null,
) : SoundscapeGeocoder() {

    private val geocoderList: List<SoundscapeGeocoder> =
        if (platformGeocoder != null)
            listOf(platformGeocoder, photonGeocoder)
        else
            listOf(photonGeocoder)

    override suspend fun getAddressFromLocationName(locationName: String,
                                                    nearbyLocation: LngLatAlt,
                                                    localizedStrings: LocalizedStrings?) : List<LocationDescription> {

        val deferredResults = geocoderList.map { geocoder ->
            coroutineScope {
                async {
                    geocoder.getAddressFromLocationName(locationName, nearbyLocation, localizedStrings)
                }
            }
        }

        val geocoderResults = deferredResults.awaitAll()

        val results: MutableList<LocationDescription> = mutableListOf()
        val platformResults = if(geocoderList.size > 1) geocoderResults[0] else null
        var streetResult: LocationDescription? = null
        if(platformResults != null) {
            for (platformResult in platformResults) {
                println("Platform: $platformResult")
                if(platformResult.locationType == LocationType.StreetNumber) {
                    streetResult = platformResult
                    if(locationName.containsNumber()) {
                        streetResult.name =
                            streetResult.description?.substringBefore(", ") ?: streetResult.name
                        println("Platform contains number: ${streetResult.name}")
                    }

                    results.add(streetResult)
                    println("Use platform result")
                    break
                }
            }
        }
        val photonResults = if(geocoderList.size > 1) geocoderResults[1] else geocoderResults[0]
        if(photonResults != null) {
            for (photonResult in photonResults) {
                println("Photon: $photonResult")
                if(streetResult != null) {
                    println("streetResult was set, see if we can improve the name")
                    if(photonResult.locationType == LocationType.StreetNumber) {
                        if (gridState.ruler.distance(
                                streetResult.location,
                                photonResult.location
                            ) < 100.0
                        ) {
                            println("Using photon result")
                            streetResult.name = photonResult.name
                            streetResult.location = photonResult.location
                            streetResult.typeDescription = photonResult.typeDescription
                            continue
                        }
                    }
                }
                results.add(photonResult)
            }
        }

        return results
    }

    override suspend fun getAddressFromLngLat(userGeometry: UserGeometry,
                                              localizedStrings: LocalizedStrings?,
                                              ignoreHouseNumbers: Boolean) : LocationDescription? {
        val deferredResults = geocoderList.map { geocoder ->
            coroutineScope {
                async {
                    geocoder.getAddressFromLngLat(userGeometry, localizedStrings, ignoreHouseNumbers)
                }
            }
        }

        val geocoderResults = deferredResults.awaitAll()

        val platformResult = if(geocoderList.size > 1) geocoderResults[0] else null
        if (platformResult != null) {
            if (platformResult.locationType == LocationType.StreetNumber) {
                return platformResult
            }
        }

        return if(geocoderList.size > 1) geocoderResults[1] else geocoderResults[0]
    }
}
