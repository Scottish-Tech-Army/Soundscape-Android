package org.scottishtecharmy.soundscape

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import org.scottishtecharmy.soundscape.logging.AppLog
import org.scottishtecharmy.soundscape.network.PhotonSearchProvider
import java.io.InterruptedIOException

@RunWith(AndroidJUnit4::class)
class PhotonSearchTest {
    private val logger = AppLog.tag("PhotonSearchTest")

    @Test
    fun testSearch() {

        if (BuildConfig.SEARCH_PROVIDER_URL.isEmpty())
            return

        runBlocking {
            withContext(Dispatchers.IO) {

                try {
                    val apiInterface = PhotonSearchProvider.getInstance()
                    val result = apiInterface.getSearchResults("Tarland")
                    if (result != null) {
                        logger.d { "Successfully got results:" }
                        result.let { result ->
                            for (feature in result.features) {
                                feature.properties?.let { properties ->
                                    if ((properties["name"] != null) &&
                                        (feature.geometry.type == "Point")
                                    ) {

                                        val location = feature.geometry as Point
                                        logger.d {
                                            "Name: " + properties["name"] +
                                                    " at " + location.coordinates.toString()
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        logger.e { "Failed to execute" }
                    }
                } catch (e: InterruptedIOException) {
                    logger.e(e) { "Timeout getting results" }
                }
            }
        }
    }
}