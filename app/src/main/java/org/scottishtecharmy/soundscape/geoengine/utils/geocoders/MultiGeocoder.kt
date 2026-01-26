package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.scottishtecharmy.soundscape.MainActivity.Companion.GEOCODER_MODE_DEFAULT
import org.scottishtecharmy.soundscape.MainActivity.Companion.GEOCODER_MODE_KEY
import org.scottishtecharmy.soundscape.components.LocationSource
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.MvtFeature
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.NetworkUtils
import org.scottishtecharmy.soundscape.utils.fuzzyCompare
import org.scottishtecharmy.soundscape.utils.toLocationDescription

/**
 * The MultiGeocoder dynamically switches between Android, Photon and Local geocoders depending on
 * the user settings and network availability.
 */
class MultiGeocoder(applicationContext: Context,
                    val gridState: GridState,
                    settlementState: GridState,
                    tileSearch: TileSearch,
                    val networkUtils: NetworkUtils) : SoundscapeGeocoder() {

    private val fusedGeocoder = FusedGeocoder(applicationContext, gridState)
    private val localGeocoder = OfflineGeocoder(gridState, settlementState, tileSearch)

    val sharedPreferences: SharedPreferences? = PreferenceManager.getDefaultSharedPreferences(applicationContext)

    private fun pickGeocoder() : SoundscapeGeocoder? {
        val settingsChoice = sharedPreferences?.getString(GEOCODER_MODE_KEY, GEOCODER_MODE_DEFAULT)
        return if(networkUtils.hasNetwork() && (settingsChoice != "Offline"))
            fusedGeocoder
        else
            localGeocoder
    }

    override suspend fun getAddressFromLocationName(locationName: String,
                                                    nearbyLocation: LngLatAlt,
                                                    localizedContext: Context?) : List<LocationDescription>? {

        val results: MutableList<LocationDescription> = mutableListOf()

        // Always search markers
        val markers = gridState.markerTree?.getAllCollection()
        if(markers != null) {
            val needle = normalizeForSearch(locationName)
            for(marker in markers) {
                val mvt = marker as MvtFeature
                if(mvt.name != null) {
                    val haystack = normalizeForSearch(mvt.name!!)
                    val score = haystack.fuzzyCompare(needle, true)
                    if(score < 0.25) {
                        val ld = mvt.toLocationDescription(LocationSource.OfflineGeocoder)
                        results.add(ld)
                    }
                }
            }
        }

        val geocoderResults = pickGeocoder()?.getAddressFromLocationName(locationName, nearbyLocation, localizedContext)
        if(geocoderResults != null) {
            for (result in geocoderResults) {
                results.add(result)
            }
        }

        return results
    }

    override suspend fun getAddressFromLngLat(userGeometry: UserGeometry,
                                              localizedContext: Context?,
                                              ignoreHouseNumbers: Boolean) : LocationDescription? {
        return pickGeocoder()?.getAddressFromLngLat(userGeometry, localizedContext, ignoreHouseNumbers)
    }
}