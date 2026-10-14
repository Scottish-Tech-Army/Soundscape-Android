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

        return deduplicate(searchResult.features, nearbyLocation).map { feature ->
            feature.toPhotonLocationDescription(localizedStrings).also(processor)
        }
    }

    override suspend fun searchByCategory(
        category: SearchCategory,
        name: String?,
        nearbyLocation: LngLatAlt,
        localizedStrings: LocalizedStrings?
    ): List<LocationDescription>? {
        // A value-only tag - ":school" - would also find every building=school, which is the
        // same school again
        val osmTags = category.tags.map { it.toPhoton() } + "!building"
        var features: List<Feature>? = null
        // Photon only looks 1km away by default, which in a town is plenty but in the country can
        // find nothing, so look further if there's not much nearby
        for (radius in CATEGORY_SEARCH_RADII_KM) {
            val searchResult = try {
                photonSearch.getNearbyByTag(
                    latitude = nearbyLocation.latitude,
                    longitude = nearbyLocation.longitude,
                    osmTags = osmTags,
                    radius = radius,
                    limit = CATEGORY_SEARCH_LIMIT,
                    nameFilter = name,
                    language = languageProvider(),
                )
            } catch (e: Exception) {
                null
            } ?: return null
            features = searchResult.features
            if (searchResult.features.size >= CATEGORY_SEARCH_ENOUGH_RESULTS) break
        }
        analyticsLogger("photonCategorySearch")

        return deduplicate(features.orEmpty(), nearbyLocation).map { feature ->
            feature.toPhotonLocationDescription(localizedStrings).also(processor)
        }
    }

    /** [features] without those with the same name as one nearby, which are the same place */
    private fun deduplicate(features: List<Feature>, nearbyLocation: LngLatAlt): List<Feature> {
        val ruler = CheapRuler(nearbyLocation.latitude)
        return features
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

    companion object {
        private val CATEGORY_SEARCH_RADII_KM = listOf(5.0, 25.0)
        private const val CATEGORY_SEARCH_LIMIT = 10U
        private const val CATEGORY_SEARCH_ENOUGH_RESULTS = 3
    }
}
