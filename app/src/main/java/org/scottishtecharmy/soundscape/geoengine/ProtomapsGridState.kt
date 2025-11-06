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

    var fileTileReaders: MutableList<Reader> = mutableListOf<Reader>()

    override fun start(applicationContext: Context?,
                       offlineExtractPaths: List<String>,
                       isUnitTesting: Boolean) {
        if((tileClient == null) && (applicationContext != null))
            tileClient = ProtomapsTileClient(applicationContext)

        if(!isUnitTesting) {
            if (offlineExtractPaths.isEmpty())
                Firebase.analytics.logEvent("GridNoOfflineMap", null)
            else
                Firebase.analytics.logEvent("GridWithOfflineMap", null)
        }

        // Create a range reader for the local file
        for(extract in offlineExtractPaths)
            fileTileReaders.add(Reader(File(extract)))
    }

    override fun stop() {
        super.stop()
        for(reader in fileTileReaders)
            reader.close()
        fileTileReaders.clear()
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
        featureCollections: Array<FeatureCollection>,
        intersectionMap: HashMap<LngLatAlt, Intersection>
    ): Boolean {
        var ret = false
        withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()

                // Try getting the tile from each file in turn
                var result : VectorTile.Tile? = null
                for((index,reader) in fileTileReaders.withIndex()) {
                    val fileTile: ByteArray? = reader.getTile(zoomLevel, x, y)
                    if(fileTile != null) {
                        // Turn the byte array into a VectorTile
                        when (reader.tileCompression.toInt()) {
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
                    if(result != null) {
                        if(index != 0) {
                            // Move the file reader to the top of the queue for next time it it's
                            // not there already. There will be some hysteresis as there is overlap
                            // between all possible extracts.
                            val workingReader = fileTileReaders.removeAt(index)
                            fileTileReaders.add(0, workingReader)
                        }
                        break
                    }
                }

                // Fallback to network
                if(result == null) {
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
                    Log.e(TAG, "Tile size ${result.serializedSize}")
                    var tileFeatureCollection: FeatureCollection?
                    val mvtParseTime = measureTimeMillis {
                        tileFeatureCollection = vectorTileToGeoJson(
                            tileX = x,
                            tileY = y,
                            mvt = result,
                            intersectionMap = intersectionMap,
                            tileZoom = zoomLevel)
                    }
                    var collections: Array<FeatureCollection>?
                    val processTime = measureTimeMillis {
                        collections = processTileFeatureCollection(tileFeatureCollection!!)
                    }
                    val addTime = measureTimeMillis {
                        for ((index, collection) in collections!!.withIndex()) {
                            featureCollections[index] += collection
                        }
                    }

                    Log.e(TAG, "Request time $requestTime")
                    Log.e(TAG, "MVT parse time $mvtParseTime")
                    Log.e(TAG, "processTileFeatureCollection $processTime")
                    Log.e(TAG, "Add to FeatureCollection time $addTime")

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
        // Merge any overlapping Polygons that are on the tile boundaries
        val mergedPoi = mergeAllPolygonsInFeatureCollection(featureCollections[TreeId.POIS.id])
        featureCollections[TreeId.POIS.id] = mergedPoi
    }
}