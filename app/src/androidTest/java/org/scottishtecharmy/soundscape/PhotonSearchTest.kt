package org.scottishtecharmy.soundscape

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.scottishtecharmy.soundscape.network.PhotonSearchProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.scottishtecharmy.soundscape.geojsonparser.geojson.Point
import java.io.InterruptedIOException

@RunWith(AndroidJUnit4::class)
class PhotonSearchTest {

   @Test
   fun testSearch() {

       if(BuildConfig.SEARCH_PROVIDER_URL.isEmpty())
           return

       runBlocking {
           withContext(Dispatchers.IO) {

               try {
                   val apiInterface = PhotonSearchProvider.getInstance()
                   val call = apiInterface.getSearchResults("Tarland")
                   val response = call.execute()
                   if (call.isExecuted) {
                       Log.d("PhotonSearchTest", "Successfully got results:")
                       response.body()?.let { result ->
                           for (feature in result.features) {
                               feature.properties?.let { properties ->
                                   if ((properties["name"] != null) &&
                                       (feature.geometry.type == "Point")
                                   ) {

                                       val location = feature.geometry as Point
                                       Log.d(
                                           "PhotonSearchTest",
                                           "Name: " + properties["name"] +
                                                   " at " + location.coordinates.toString()
                                       )
                                   }
                               }
                           }
                       }
                   } else {
                       Log.e("PhotonSearchTest", "Failed to execute")
                   }
               } catch(e : InterruptedIOException) {
                   Log.e("PhotonSearchTest", "Timeout getting results: $e")
               }
           }
       }
   }
}