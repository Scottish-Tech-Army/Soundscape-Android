package org.scottishtecharmy.soundscape.network

import org.scottishtecharmy.soundscape.BuildConfig

object UserAgentInterceptor {
    val USER_AGENT: String = buildString {
        append("Soundscape/${BuildConfig.VERSION_NAME}")
        if (BuildConfig.BUILD_TYPE == "releaseTest") {
            append(" (unit tests)")
        }
    }
}
