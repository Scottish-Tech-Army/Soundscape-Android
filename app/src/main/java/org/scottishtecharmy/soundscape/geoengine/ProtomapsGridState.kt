package org.scottishtecharmy.soundscape.geoengine

import android.content.Context
import android.util.Log
import ch.poole.geo.pmtiles.Reader
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.Intersection
import org.scottishtecharmy.soundscape.geoengine.mvttranslation.vectorTileToGeoJson
import org.scottishtecharmy.soundscape.geoengine.utils.mergeAllPolygonsInFeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.FeatureCollection
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.network.ITileDAO
import org.scottishtecharmy.soundscape.network.ProtomapsTileClient
import org.scottishtecharmy.soundscape.utils.findExtractPaths
import retrofit2.awaitResponse
import vector_tile.VectorTile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.GZIPInputStream
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
    var startedUnitTesting: Boolean = false

    override fun start(applicationContext: Context?,
                       offlineExtractPath: String,
                       isUnitTesting: Boolean) {
        if((tileClient == null) && (applicationContext != null))
            tileClient = ProtomapsTileClient(applicationContext)

        extractPath = offlineExtractPath
        currentExtracts = mutableListOf()
        startedUnitTesting = isUnitTesting
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
            if (!startedUnitTesting) {
                if (currentExtracts.isEmpty())
                    Firebase.analytics.logEvent("GridNoOfflineMap", null)
                else
                    Firebase.analytics.logEvent("GridWithOfflineMap", null)
            }

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
    fun decompressGzip(compressedData: ByteArray): ByteArray? {
        // Create a ByteArrayInputStream from the compressed data
        val byteArrayInputStream = ByteArrayInputStream(compressedData)
        var gzipInputStream: GZIPInputStream? = null
        val outputStream = ByteArrayOutputStream()

        try {
            // Wrap the ByteArrayInputStream with GZIPInputStream
            gzipInputStream = GZIPInputStream(byteArrayInputStream)

            // Buffer for reading decompressed data
            val buffer = ByteArray(1024) // Adjust buffer size as needed
            var len: Int

            // Read from GZIPInputStream and write to ByteArrayOutputStream
            while (gzipInputStream.read(buffer).also { len = it } > 0) {
                outputStream.write(buffer, 0, len)
            }

            return outputStream.toByteArray()

        } catch (e: IOException) {
            // Handle potential IOExceptions during decompression
            e.printStackTrace() // Log the error or handle it appropriately
            return null
        } finally {
            // Ensure streams are closed
            try {
                gzipInputStream?.close()
                outputStream.close()
                byteArrayInputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun updateTile(
        x: Int,
        y: Int,
        workerIndex: Int,
        featureCollections: Array<FeatureCollection>,
        intersectionMap: HashMap<LngLatAlt, Intersection>
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
                    //println("File reader got a tile for worker $workerIndex")
                    when (reader?.tileCompression?.toInt()) {
                        1 -> {
                            // No compression
                            result = VectorTile.Tile.parseFrom(fileTile)
                        }

                        2 -> {
                            // Gzip compression
                            val decompressedTile = decompressGzip(fileTile)
                            result = VectorTile.Tile.parseFrom(decompressedTile)
                        }

                        else -> assert(false)
                    }
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