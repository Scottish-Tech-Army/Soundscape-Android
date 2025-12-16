package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.scottishtecharmy.soundscape.MainActivity.Companion.GEOCODER_MODE_DEFAULT
import org.scottishtecharmy.soundscape.MainActivity.Companion.GEOCODER_MODE_KEY
import org.scottishtecharmy.soundscape.geoengine.GridState
import org.scottishtecharmy.soundscape.geoengine.UserGeometry
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.utils.NetworkUtils

/**
 * The MultiGeocoder dynamically switches between Android, Photon and Local geocoders depending on
 * the user settings and network availability.
 */
class MultiGeocoder(applicationContext: Context,
                    gridState: GridState,
                    settlementState: GridState,
                    tileSearch: TileSearch,
                    val networkUtils: NetworkUtils) : SoundscapeGeocoder() {

    private val androidGeocoder = AndroidGeocoder(applicationContext)
    private val localGeocoder = OfflineGeocoder(gridState, settlementState, tileSearch)
    private val photonGeocoder = PhotonGeocoder(applicationContext)

    val sharedPreferences: SharedPreferences? = PreferenceManager.getDefaultSharedPreferences(applicationContext)

    private fun pickGeocoder() : SoundscapeGeocoder? {
        val settingsChoice = sharedPreferences?.getString(GEOCODER_MODE_KEY, GEOCODER_MODE_DEFAULT)
        val networkGeocoder = (settingsChoice != "Offline")
        if(networkGeocoder) {
            if (networkUtils.hasNetwork()) {
                return if (AndroidGeocoder.enabled && (settingsChoice != "Photon")) {
                    androidGeocoder
                } else {
                    photonGeocoder
                }
            }
        }
        return localGeocoder
    }

    override suspend fun getAddressFromLocationName(locationName: String,
                                                    nearbyLocation: LngLatAlt,
                                                    localizedContext: Context?) : List<LocationDescription>? {
        return pickGeocoder()?.getAddressFromLocationName(locationName, nearbyLocation, localizedContext)
    }

    override suspend fun getAddressFromLngLat(userGeometry: UserGeometry, localizedContext: Context?) : LocationDescription? {
        return pickGeocoder()?.getAddressFromLngLat(userGeometry, localizedContext)
    }
}