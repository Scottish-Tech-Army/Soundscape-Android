package org.scottishtecharmy.soundscape.geoengine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.geoengine.utils.cleanTileGeoJSON
import org.scottishtecharmy.soundscape.geoengine.utils.deduplicateFeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.network.ITileDAO
import org.scottishtecharmy.soundscape.network.SoundscapeBackendTileClient
import retrofit2.awaitResponse
import kotlin.coroutines.cancellation.CancellationException

class SoundscapeBackendGridState : GridState() {

    override fun start(applicationContext: Context) {
        tileClient = SoundscapeBackendTileClient(applicationContext)
    }

    override suspend fun updateTile(
        x: Int,
        y: Int,
        featureCollections: Array<FeatureCollection>,
    ): Boolean {
        var ret = false

        withContext(Dispatchers.IO) {
            try {
                val service =
                    tileClient.retrofitInstance?.create(ITileDAO::class.java)
                val tileReq =
                    async {
                        service?.getTileWithCache(x, y)
                    }
                val result = tileReq.await()?.awaitResponse()?.body()
                // clean the tile, process the string, perform an insert into db using the clean tile data
                Log.e(TAG, "Tile size ${result?.length}")
                val cleanedTile =
                    result?.let { cleanTileGeoJSON(x, y, ZOOM_LEVEL, it) }

                if (cleanedTile != null) {
                    val tileData = processTileString(cleanedTile)
                    for ((index, collection) in tileData.withIndex()) {
                        featureCollections[index].plusAssign(collection)
                    }

                    ret = true
                } else {
                    Log.e(TAG, "Failed to get clean soundscape-backend tile")
                }
            } catch (ce: CancellationException) {
                // We have to rethrow cancellation exceptions
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "Exception getting soundscape-backend tile $e")
            }
        }
        return ret
    }

    override fun fixupCollections(featureCollections: Array<FeatureCollection>){
        // De-duplicate
        val deDuplicatedCollection =
            Array(TreeId.MAX_COLLECTION_ID.id) { FeatureCollection() }
        for ((index, fc) in featureCollections.withIndex()) {
            val existingSet: MutableSet<Any> = mutableSetOf()
            deduplicateFeatureCollection(
                deDuplicatedCollection[index],
                fc,
                existingSet,
            )
        }
    }
}