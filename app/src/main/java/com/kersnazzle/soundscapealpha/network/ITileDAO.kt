package com.kersnazzle.soundscapealpha.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ITileDAO {
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
}