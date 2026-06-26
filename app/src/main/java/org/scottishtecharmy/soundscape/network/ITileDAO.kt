package org.scottishtecharmy.soundscape.network

import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SERVER_PATH
import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SUFFIX
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import vector_tile.VectorTile

interface ITileDAO {
    @GET("$PROTOMAPS_SERVER_PATH/{z}/{x}/{y}.$PROTOMAPS_SUFFIX")
    fun getVectorTileWithCache(
        @Path("x") x: Int,
        @Path("y") y: Int,
        @Path("z") z: Int
    ): Call<VectorTile.Tile>
}