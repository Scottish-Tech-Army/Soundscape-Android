package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.CheapRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Feature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.network.PhotonSearch
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.deferredToLocationDescription

class PhotonGeocoder(
    private val photonSearch: PhotonSearch,
    private val languageProvider: () -> String? = { null },
    private val analyticsLogger: (String) -> Unit = {},
    private val processor: (LocationDescription) -> Unit = {},
) : SoundscapeGeocoder() {

    override suspend fun getAddressFromLocationName(
        locationName: String,
        nearbyLocation: LngLatAlt,
        localizedStrings: LocalizedStrings?
    ): List<LocationDescription>? {
        val searchResult = try {
            photonSearch.getSearchResults(
                searchString = locationName,
                latitude = nearbyLocation.latitude,
                longitude = nearbyLocation.longitude,
                language = languageProvider(),
            )
        } catch (e: Exception) {
            null
        }
        analyticsLogger("photonGeocode")

        if (searchResult == null) return null

        val ruler = CheapRuler(nearbyLocation.latitude)
        val deduplicate = searchResult.features
            .fold(mutableListOf<Feature>()) { accumulator, result ->
                val point = (result.geometry as? Point)
                var isDuplicate = false
                if (point != null) {
                    isDuplicate = accumulator.any {
                        val otherPoint = (it.geometry as? Point)
                        if (otherPoint != null) {
                            it.properties?.get("name") == result.properties?.get("name") &&
                                    ruler.distance(
                                        otherPoint.coordinates,
                                        point.coordinates
                                    ) < 100.0
                        } else false
                    }
                }
                if (!isDuplicate) {
                    accumulator.add(result)
                }
                accumulator
            }

        return deduplicate.map { feature ->
            feature.toPhotonLocationDescription(localizedStrings).also(processor)
        }
    }

    /**
     * Builds the [MvtFeature] scratch object Photon results are described through, and the
     * [LocationDescription] it feeds. `?.toString()` (not `.toString()`) on each property lookup
     * matters: a genuinely absent property must stay null, not become the literal string "null" -
     * that string would otherwise flow into [MvtFeature.getText] as if it were a real name.
     */
    private fun Feature.toPhotonLocationDescription(
        localizedStrings: LocalizedStrings?,
    ): LocationDescription {
        val mvt = MvtFeature()
        mvt.properties = properties
        mvt.name = properties?.get("name")?.toString()
        mvt.featureType = properties?.get("osm_key")?.toString()
        mvt.featureClass = properties?.get("osm_value")?.toString()
        if ((mvt.featureType == "highway") && (mvt.featureClass == "residential"))
            mvt.featureClass = "residential_street"
        return deferredToLocationDescription(
            LocationSource.PhotonGeocoder,
            featureName = mvt.getText(localizedStrings)
        )
    }

    override suspend fun getAddressFromLngLat(
        userGeometry: UserGeometry,
        localizedStrings: LocalizedStrings?,
        ignoreHouseNumbers: Boolean
    ): LocationDescription? {
        val location = userGeometry.mapMatchedLocation?.point ?: userGeometry.location
        val searchResult = try {
            photonSearch.reverseGeocodeLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                language = languageProvider(),
            )
        } catch (e: Exception) {
            null
        }
        analyticsLogger("photonReverseGeocode")

        return searchResult?.features?.firstNotNullOfOrNull { feature ->
            feature.toPhotonLocationDescription(localizedStrings).also(processor)
        }
    }
}
