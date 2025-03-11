package org.scottishtecharmy.soundscape.geoengine

import android.app.Application
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.InterpolatedPointsJoiner
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.vectorTileToGeoJson
import org.scottishtecharmy.soundscape.geoengine.utils.mergeAllPolygonsInFeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.network.ITileDAO
import org.scottishtecharmy.soundscape.network.ProtomapsTileClient
import retrofit2.awaitResponse
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource

class ProtomapsGridState : GridState() {

    override fun start(application: Application) {
        tileClient = ProtomapsTileClient(application)
    }

    /**
     * updateTile is responsible for getting data from the protomaps server and translating it from
     * MVT format into a set of FeatureCollections.
     */
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
                        service?.getVectorTileWithCache(x, y, ZOOM_LEVEL)
                    }
                val result = tileReq.await()?.awaitResponse()?.body()
                if (result != null) {
                    Log.e(TAG, "Tile size ${result.serializedSize}")
                    val tileFeatureCollection = vectorTileToGeoJson(x, y, result)
                    val collections = processTileFeatureCollection(tileFeatureCollection)
                    for ((index, collection) in collections.withIndex()) {
                        featureCollections[index].plusAssign(collection)
                    }

                    ret = true
                } else {
                    Log.e(TAG, "No response for protomaps tile")
                }
            } catch (ce: CancellationException) {
                // We have to rethrow cancellation exceptions
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "Exception getting protomaps tile $e")
            }
        }
        return ret
    }

    override fun fixupCollections(featureCollections: Array<FeatureCollection>) {
        // Join up roads/paths at the tile boundary
        val joiner = InterpolatedPointsJoiner()
        for (ip in featureCollections[TreeId.INTERPOLATIONS.id]) {
            joiner.addInterpolatedPoints(ip)
        }
        val timeSource = TimeSource.Monotonic
        val mergeStartTime = timeSource.markNow()

        // Merge any overlapping Polygons that are on the tile boundaries
        val mergedPoi = mergeAllPolygonsInFeatureCollection(featureCollections[TreeId.POIS.id])
        featureCollections[TreeId.POIS.id] = mergedPoi

        val mergeFinishTime = timeSource.markNow()
        println("Time taken to merge polygons: ${mergeFinishTime - mergeStartTime}")

        joiner.addJoiningLines(featureCollections[TreeId.ROADS_AND_PATHS.id])
    }
}