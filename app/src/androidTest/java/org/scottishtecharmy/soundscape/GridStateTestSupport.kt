package org.scottishtecharmy.soundscape

import android.content.Context
import org.scottishtecharmy.soundscape.geoengine.ProtomapsGridState
import org.scottishtecharmy.soundscape.network.UserAgentInterceptor
import org.scottishtecharmy.soundscape.network.createAndroidVectorTileClient
import org.scottishtecharmy.soundscape.utils.NetworkUtils

fun ProtomapsGridState.startWithContext(context: Context, offlineExtractPath: String = "") {
    tileClient = createAndroidVectorTileClient(
        baseUrl = BuildConfig.TILE_PROVIDER_URL,
        cacheDir = context.cacheDir,
        userAgent = UserAgentInterceptor.USER_AGENT,
        hasNetwork = { NetworkUtils(context).hasNetwork() },
    )
    start(offlineExtractPath)
}
