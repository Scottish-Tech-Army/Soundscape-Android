package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.i18n.LocalizedStrings
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.deferredToLocationDescription
import org.scottishtecharmy.soundscape.utils.fuzzyCompare

/**
 * The MultiGeocoder dynamically switches between platform, Photon and Local geocoders depending on
 * the user settings and network availability.
 */
class MultiGeocoder(
    val gridState: GridState,
    settlementState: GridState,
    tileSearch: TileSearcher?,
    photonGeocoder: PhotonGeocoder,
    platformGeocoder: SoundscapeGeocoder? = null,
    analyticsLogger: (String) -> Unit = {},
    private val processor: (LocationDescription) -> Unit = {},
    private val hasNetwork: () -> Boolean = { false },
    private val geocoderMode: () -> String? = { null },
) : SoundscapeGeocoder() {

    private val fusedGeocoder = FusedGeocoder(gridState, photonGeocoder, platformGeocoder)

    /**
     * Exposed so callers who specifically want the tile-derived answer can ask for it rather than
     * whatever [pickGeocoder] would choose. Somewhere already in the loaded grid - a POI the user
     * just tapped in Places Nearby, say - the offline geocoder is both faster and more relevant
     * than a network round trip, and it works with no signal at all.
     */
    val offlineGeocoder =
        OfflineGeocoder(gridState, settlementState, tileSearch, analyticsLogger, processor)

    private fun pickGeocoder(): SoundscapeGeocoder? {
        val settingsChoice = geocoderMode()
        return if (hasNetwork() && (settingsChoice != "Offline"))
            fusedGeocoder
        else
            offlineGeocoder
    }

    override suspend fun getAddressFromLocationName(
        locationName: String,
        nearbyLocation: LngLatAlt,
        localizedStrings: LocalizedStrings?
    ): List<LocationDescription> {

        val results: MutableList<LocationDescription> = mutableListOf()

        val markers = gridState.markerTree?.getAllCollection()
        if (markers != null) {
            val needle = normalizeForSearch(locationName)
            for (marker in markers) {
                val mvt = marker as MvtFeature
                val name = mvt.name
                if (name != null) {
                    val haystack = normalizeForSearch(name)
                    val score = haystack.fuzzyCompare(needle, true)
                    if (score < 0.25) {
                        val ld = mvt.deferredToLocationDescription(LocationSource.OfflineGeocoder)
                            .also(processor)
                        results.add(ld)
                    }
                }
            }
        }

        val geocoderResults = pickGeocoder()?.getAddressFromLocationName(
            locationName,
            nearbyLocation,
            localizedStrings
        )
        if (geocoderResults != null) {
            for (result in geocoderResults) {
                results.add(result)
            }
        }

        return results
    }

    override suspend fun getAddressFromLngLat(
        userGeometry: UserGeometry,
        localizedStrings: LocalizedStrings?,
        ignoreHouseNumbers: Boolean
    ): LocationDescription? {
        val firstGeocoder = pickGeocoder()
        var results =
            firstGeocoder?.getAddressFromLngLat(userGeometry, localizedStrings, ignoreHouseNumbers)
        if (results == null) {
            if (firstGeocoder != offlineGeocoder) {
                results = offlineGeocoder.getAddressFromLngLat(
                    userGeometry,
                    localizedStrings,
                    ignoreHouseNumbers
                )
            }
        }
        return results
    }
}
