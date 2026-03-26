package org.scottishtecharmy.soundscape.network

import okhttp3.Interceptor
import okhttp3.Response
import org.scottishtecharmy.soundscape.BuildConfig

class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", USER_AGENT)
            .build()
        return chain.proceed(request)
    }

    companion object {
        val USER_AGENT: String = buildString {
            append("Soundscape/${BuildConfig.VERSION_NAME}")
            if (BuildConfig.BUILD_TYPE == "releaseTest") {
                append(" (unit tests)")
            }
        }
    }
}
