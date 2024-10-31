package org.scottishtecharmy.soundscape.network

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
    @GET("protomaps/{z}/{x}/{y}.mvt")
    fun getVectorTile(
        @Path("x") x: Int,
        @Path("y") y: Int,
        @Path("z") z: Int
    ): Call<VectorTile.Tile>

    @GET("protomaps/{z}/{x}/{y}.mvt")
    fun getVectorTileWithCache(
        @Path("x") x: Int,
        @Path("y") y: Int,
        @Path("z") z: Int
    ): Call<VectorTile.Tile>
}