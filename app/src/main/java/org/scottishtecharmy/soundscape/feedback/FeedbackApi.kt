package org.scottishtecharmy.soundscape.feedback

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.network.UserAgentInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Submits a single feedback response to the server (SA-315). Fire-and-forget:
 * payload is built by [FeedbackPrompter] and POSTed; failures are logged and
 * dropped — disengaged users are out of scope per SA-310.
 */
interface FeedbackApi {
    @Headers("Content-Type: application/json")
    @POST("submit")
    fun submit(@Body payload: FeedbackPayload): Call<Unit>

    companion object {
        private var instance: FeedbackApi? = null
        private val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        private val client = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(loggingInterceptor)
            .callTimeout(5, TimeUnit.SECONDS)
            .build()

        fun getInstance(): FeedbackApi {
            if (instance == null) {
                val moshi = Moshi.Builder().build()
                instance = Retrofit.Builder()
                    .baseUrl(BuildConfig.FEEDBACK_PROVIDER_URL.ifEmpty { PLACEHOLDER_BASE_URL })
                    .client(client)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(FeedbackApi::class.java)
            }
            return instance!!
        }

        // Used only when local.properties has no feedbackProviderUrl yet (pre-SA-315).
        // Retrofit requires a non-empty base URL; the prompter won't actually POST when
        // the BuildConfig value is empty.
        private const val PLACEHOLDER_BASE_URL = "https://example.invalid/"
    }
}
