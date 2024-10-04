package org.scottishtecharmy.soundscape.network

import com.squareup.moshi.Moshi
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.GeoMoshi
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

const val BASE_URL = "https://photon.komoot.io/"

/**
 * This is a proof of concept for using the photon search engine https://github.com/komoot/photon
 * In testing it returned by far the best search results and can be run on a relatively modest
 * server. This is using the demo server at https://photon.komoot.io/
 *  @param q is the search string to use. Photon supports partial string searches and so this can
 *  be as little as 3 characters long.
 *  @param latitude
 *  @param longitude If these are passed in, then results are prioritized based on that location.
 *  @param limit is the number of results to return.
 */
interface PhotonSearchProvider {
    @GET("api/")
    fun getSearchResults(@Query("q") searchString : String,
                         @Query("lat") latitude : Double? = null,
                         @Query("lon") longitude : Double? = null,
                         @Query("limit") limit : UInt = 5U
                         ): Call<FeatureCollection>

    companion object {
        private var searchProvider: PhotonSearchProvider? = null
        private var moshi: Moshi? = null
        fun getInstance(): PhotonSearchProvider {
            if (searchProvider == null) {
                moshi = GeoMoshi.registerAdapters(Moshi.Builder()).build()
                searchProvider = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(MoshiConverterFactory.create(moshi!!))
                    .build().create(PhotonSearchProvider::class.java)
            }
            return searchProvider!!
        }
    }
}
