package org.scottishtecharmy.soundscape.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NetworkUtils(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun hasNetwork(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val activeNetworkCapabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return when {
            activeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            activeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }

    /**
     * Whether Android has confirmed that the active network actually reaches the internet - the
     * check behind "connected, no internet". Unlike [hasNetwork] this is false on Wi-Fi behind a
     * captive portal, or with DNS that never answers, which is where a network request would
     * otherwise sit until its timeout. True through a VPN whose underlying network is validated.
     *
     * Not a replacement for [hasNetwork]: on networks which block Android's connectivity check
     * (some corporate networks, China) this stays false even though the internet works. That's
     * fine for choosing the offline geocoder, which still works, but would leave the tile client
     * cache-only and never fetching new tiles.
     */
    fun hasValidatedInternet(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val activeNetworkCapabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return activeNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            activeNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
