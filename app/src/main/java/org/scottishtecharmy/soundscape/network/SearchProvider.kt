package org.scottishtecharmy.soundscape.network

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * This is a proof of concept for using the photon search engine https://github.com/komoot/photon
 * In testing it returned by far the best search results and can be run on a relatively modest
 * server. This is using the demo server at https://photon.komoot.io/
 *  @param searchString is the search string to use. Photon supports partial string searches and so this can
 *  be as little as 3 characters long.
 *  @param latitude
 *  @param longitude If these are passed in, then results are prioritized based on that location.
 *  @param limit is the number of results to return.
 *  @param language is the language to use for the search.
 */
interface PhotonSearchProvider {
    @Headers(
        "Cache-control: max-age=0",
        "Connection: keep-alive"
    )
    @GET("api/")
    fun getSearchResults(
        @Query("q") searchString: String,
        @Query("lat") latitude: Double? = null,
        @Query("lon") longitude: Double? = null,
        @Query("lang") language: String? = null,
        @Query("limit") limit: UInt = 5U,
        @Query("location_bias_scale") bias: Float = 0.2f
    ): Call<FeatureCollection>

    @Headers(
        "Cache-control: max-age=0",
        "Connection: keep-alive"
    )
    @GET("reverse/")
    fun reverseGeocodeLocation(
        @Query("lat") latitude: Double? = null,
        @Query("lon") longitude: Double? = null,
        @Query("lang") language: String? = null
    ): Call<FeatureCollection>

    companion object {
        private var searchProvider: PhotonSearchProvider? = null
        private var moshi: Moshi? = null
        private val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE // Disable logging in release builds
            }
        }
        private val logging = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()

        fun getInstance(): PhotonSearchProvider {
            if (searchProvider == null) {
                moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
                searchProvider = Retrofit.Builder()
                    .baseUrl(BuildConfig.SEARCH_PROVIDER_URL).client(logging)
                    .addConverterFactory(MoshiConverterFactory.create(moshi!!))
                    .build()
                    .create(PhotonSearchProvider::class.java)
            }
            return searchProvider!!
        }
    }
}
