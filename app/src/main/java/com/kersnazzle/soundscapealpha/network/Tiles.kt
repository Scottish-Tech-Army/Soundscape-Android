package com.kersnazzle.soundscapealpha.network


import com.kersnazzle.soundscapealpha.utils.cleanTileGeoJSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse


interface ITiles {
    suspend fun getTile(xtile: Int, ytile: Int): String?
}

class Tiles(): ITiles {

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
}