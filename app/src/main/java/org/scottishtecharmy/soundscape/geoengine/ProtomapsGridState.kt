package org.scottishtecharmy.soundscape.geoengine

import android.content.Context
import ch.poole.geo.pmtiles.Reader
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.vectorTileToGeoJson
import org.scottishtecharmy.soundscape.geoengine.utils.mergeAllPolygonsInFeatureCollection
import org.scottishtecharmy.soundscape.geoengine.utils.decompressTile
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.network.ITileDAO
import org.scottishtecharmy.soundscape.network.ProtomapsTileClient
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.utils.findExtractPaths
import retrofit2.awaitResponse
import vector_tile.VectorTile
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
open class ProtomapsGridState(
    zoomLevel: Int = MAX_ZOOM_LEVEL,
    gridSize: Int = GRID_SIZE,
    passedInTreeContext: CloseableCoroutineDispatcher? = null
) : GridState(zoomLevel, gridSize, passedInTreeContext) {

    // We need an array of pmtile readers, one for each tile in the grid.
    var fileTileReaders = arrayOfNulls<Reader?>(gridSize * gridSize)
    var currentExtracts: MutableList<String> = mutableListOf()
    var extractPath: String = ""

    override fun start(applicationContext: Context?,
                       offlineExtractPath: String) {
        if((tileClient == null) && (applicationContext != null))
            tileClient = ProtomapsTileClient(applicationContext)

        extractPath = offlineExtractPath
        currentExtracts = mutableListOf()
    }

    override fun stop() {
        super.stop()
        for(reader in fileTileReaders)
            reader?.close()
        fileTileReaders = arrayOfNulls<Reader?>(gridSize * gridSize)
    }

    override fun checkOfflineMaps() {
        // Check for change in offline map extracts and update our file readers if there's a change
        val extracts = findExtractPaths(extractPath).toMutableList()
        if (extracts != currentExtracts) {
            println("Change in offline extracts")
            currentExtracts = extracts
            if (currentExtracts.isEmpty())
                Analytics.getInstance().logEvent("GridNoOfflineMap", null)
            else
                Analytics.getInstance().logEvent("GridWithOfflineMap", null)

            // Close old file readers
            for (reader in fileTileReaders)
                reader?.close()
            fileTileReaders = arrayOfNulls<Reader?>(gridSize * gridSize)
        }
    }

    /**
     * updateTile is responsible for getting data from the protomaps server and translating it from
     * MVT format into a set of FeatureCollections.
     */
    override suspend fun updateTile(
        x: Int,
        y: Int,
        workerIndex: Int,
        featureCollections: Array<FeatureCollection>,
        intersectionMap: HashMap<LngLatAlt, Intersection>,
        streetNumberMap: HashMap<String, FeatureCollection>
    ): Boolean {
        var ret = false

        withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()

                var result : VectorTile.Tile? = null

                // Try the reader that we are currently using first
                var reader = fileTileReaders[workerIndex]
                var fileTile: ByteArray? = reader?.getTile(zoomLevel, x, y)

                if(fileTile == null)
                {
                    // We failed to get a tile from the current reader, so we need to find one that
                    // does work. Reset it, and then see if we can find an extract that does work.
                    fileTileReaders[workerIndex]?.close()
                    fileTileReaders[workerIndex] = null
                    for(extract in currentExtracts) {
                        println("Try $extract for worker $workerIndex")
                        reader = Reader(File(extract))
                        fileTile = reader.getTile(zoomLevel, x, y)
                        if(fileTile != null) {
                            // We've found an extract that works, so use that
                            fileTileReaders[workerIndex] = reader
                            break
                        }
                        reader.close()
                    }
                }
                if(fileTile != null) {
                    // Turn the byte array into a VectorTile
                    result = decompressTile(reader?.tileCompression, fileTile)
                }

                // Fallback to network
                if(result == null) {
                    //println("Network tile request for worker $workerIndex")
                    val service =
                        tileClient?.retrofitInstance?.create(ITileDAO::class.java)
                    val tileReq =
                        async {
                            service?.getVectorTileWithCache(x, y, zoomLevel)
                        }
                    result = tileReq.await()?.awaitResponse()?.body()
                }

                if (result != null) {
                    val requestTime = System.currentTimeMillis() - startTime
                    println("Tile size ${result.serializedSize}")
                    var collections: Array<FeatureCollection>?
                    val mvtParseTime = measureTimeMillis {
                        collections = vectorTileToGeoJson(
                            tileX = x,
                            tileY = y,
                            mvt = result,
                            intersectionMap = intersectionMap,
                            streetNumberMap = streetNumberMap,
                            tileZoom = zoomLevel)
                    }
                    val addTime = measureTimeMillis {
                        if(collections != null) {
                            for ((index, collection) in collections.withIndex()) {
                                featureCollections[index] += collection
                            }
                        }
                    }

                    println("Request time $requestTime")
                    println("MVT parse time $mvtParseTime")
                    println("Add to FeatureCollection time $addTime")

                    ret = true
                } else {
                    println("No response for protomaps tile")
                }
            } catch (ce: CancellationException) {
                // We have to rethrow cancellation exceptions
                throw ce
            } catch (e: Exception) {
                println("Exception getting protomaps tile $e")
            }
        }
        return ret
    }

    override fun fixupCollections(featureCollections: Array<FeatureCollection>) {
        // Merge any overlapping Polygons that are on the tile boundaries
        val mergedPoi = mergeAllPolygonsInFeatureCollection(featureCollections[TreeId.POIS.id])
        featureCollections[TreeId.POIS.id] = mergedPoi
    }
}