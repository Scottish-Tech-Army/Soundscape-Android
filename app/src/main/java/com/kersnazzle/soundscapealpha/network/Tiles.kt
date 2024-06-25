package com.kersnazzle.soundscapealpha.network


import android.app.Application
import android.content.Context
import com.kersnazzle.soundscapealpha.utils.cleanTileGeoJSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse


interface ITiles {
    suspend fun getTile(xtile: Int, ytile: Int): String?

    suspend fun getTileWithCache(xtile: Int, ytile: Int): String?
}

class Tiles(): ITiles, Application() {
    private lateinit var okhttpClientInstance: OkhttpClientInstance


    override suspend fun getTile(xtile: Int, ytile: Int): String? {
        return withContext(Dispatchers.IO) {
            // use the RetrofitClientInstanceObject and the ITileDao
            val service = RetrofitClientInstance.retrofitInstance?.create(ITileDAO::class.java)

            val tile = async { service?.getTile(xtile, ytile) }
            val result = tile.await()?.awaitResponse()?.body()
            // use the cleanTileGeoJSON function to remove the weirdness
            return@withContext result?.let<String, String> { cleanTileGeoJSON(xtile, ytile, 16.0, it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        okhttpClientInstance = OkhttpClientInstance(this)
    }

    override suspend fun getTileWithCache(xtile: Int, ytile: Int): String? {

        return withContext(Dispatchers.IO) {
            // use the okhttpClientInstance and the ITileDao
            val service = okhttpClientInstance.retrofitInstance?.create(ITileDAO::class.java)

            val tile = async { service?.getTileWithCache(xtile, ytile) }
            val result = tile.await()?.awaitResponse()?.body()
            // use the cleanTileGeoJSON function to remove the weirdness
            return@withContext result?.let<String, String> { cleanTileGeoJSON(xtile, ytile, 16.0, it) }
        }
    }
}