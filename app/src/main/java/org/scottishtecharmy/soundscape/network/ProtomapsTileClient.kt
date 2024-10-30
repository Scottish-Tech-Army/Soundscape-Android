package org.scottishtecharmy.soundscape.network

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.protobuf.ProtoConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import vector_tile.VectorTile

interface IProtomapsTileDAO {
    @GET("protomaps/{z}/{x}/{y}.mvt")
    fun getMvtTileWithCache(
        @Path("x") x: Int,
        @Path("y") y: Int,
        @Path("z") z: Int
    ): Call<VectorTile.Tile>
}
/**
 * This is a retrofit client for getting tiles from our protomaps server and parsing the protobuf
 * automatically as it goes
 *  @param x tile coordinate
 *  @param y tile coordinate
 *  @param z zoom level
 */
class ProtomapsTileClient {

    private var retrofitInstance : Retrofit? = null
    fun getClient(): IProtomapsTileDAO {
        if(retrofitInstance == null) {
            retrofitInstance = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ProtoConverterFactory.create())
                .build()
        }
        return retrofitInstance!!.create(IProtomapsTileDAO::class.java)
    }
    companion object {
        private const val BASE_URL = "https://d1wzlzgah5gfol.cloudfront.net"
    }
}
