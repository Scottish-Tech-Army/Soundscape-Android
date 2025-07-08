package org.scottishtecharmy.soundscape.network

import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SERVER_PATH
import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SUFFIX
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import vector_tile.VectorTile

interface ITileDAO {
    // soundscape-backend functions
    @GET("tiles/16/{x}/{y}.json")
    fun getTile(
        @Path("x") x: Int,
        @Path("y") y: Int
    ): Call<String>

    @GET("tiles/16/{x}/{y}.json")
    fun getTileWithCache(
        @Path("x") x: Int,
        @Path("y") y: Int
    ): Call<String>

    // protomaps server functions
    @GET("$PROTOMAPS_SERVER_PATH/{z}/{x}/{y}.$PROTOMAPS_SUFFIX")
    fun getVectorTile(
        @Path("x") x: Int,
        @Path("y") y: Int,
        @Path("z") z: Int
    ): Call<VectorTile.Tile>

    @GET("$PROTOMAPS_SERVER_PATH/{z}/{x}/{y}.$PROTOMAPS_SUFFIX")
    fun getVectorTileWithCache(
        @Path("x") x: Int,
        @Path("y") y: Int,
        @Path("z") z: Int
    ): Call<VectorTile.Tile>
}