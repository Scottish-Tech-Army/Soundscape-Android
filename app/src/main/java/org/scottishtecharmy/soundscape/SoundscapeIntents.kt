package org.scottishtecharmy.soundscape

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.database.local.model.MarkerData
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.Navigator
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.generateRouteDetailsRoute
import org.scottishtecharmy.soundscape.utils.parseGpxFile
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject


class SoundscapeIntents
    @Inject
    constructor(
        private val navigator: Navigator,
    ) {
        private lateinit var geocoder: Geocoder

        private fun useGeocoderToGetAddress(
            location: String,
            context: Context,
        ) {
            geocoder = Geocoder(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val geocodeListener =
                    Geocoder.GeocodeListener { addresses ->
                        Log.d(TAG, "getFromLocationName results count " + addresses.size.toString())
                        val address = addresses.firstOrNull()
                        if (address != null) {
                            Log.d(TAG, "$address")
                            val ld =
                                LocationDescription(
                                    name = address.getAddressLine(0),
                                    location = LngLatAlt(address.longitude, address.latitude)
                                )
                            navigator.navigate(generateLocationDetailsRoute(ld))
                        }
                    }
                Log.d(TAG, "Call getFromLocationName on $location")
                geocoder.getFromLocationName(location, 1, geocodeListener)
            } else {
                Log.d(TAG, "Pre-API33: $location")

                @Suppress("DEPRECATION")
                val address = geocoder.getFromLocationName(location, 1)?.firstOrNull()
                if (address != null) {
                    Log.d(TAG, "Address: $address")
                    val ld =
                        LocationDescription(
                            name = address.getAddressLine(0),
                            location = LngLatAlt(address.longitude, address.latitude)
                        )
                    navigator.navigate(generateLocationDetailsRoute(ld))
                }
            }
        }

        private fun getRedirectUrl(
            url: String,
            context: Context,
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                var urlTmp: URL? = null
                var connection: HttpURLConnection? = null

                try {
                    Log.d(TAG, "Open URL $url")
                    urlTmp = URL(url)
                } catch (e1: MalformedURLException) {
                    e1.printStackTrace()
                }

                try {
                    Log.d(TAG, "Open connection")
                    connection = urlTmp!!.openConnection() as HttpURLConnection
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                try {
                    connection!!.responseCode
                    Log.d(TAG, "Response ${connection.responseCode}")
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                val redUrl = connection!!.url.toString()
                connection.disconnect()

                Log.d(TAG, "Maps URL: $redUrl")
                useGeocoderToGetAddress(redUrl, context)
            }
        }

    private fun inputStreamToJson(inputStream: InputStream): JSONObject? {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                stringBuilder.append(line)
                line = reader.readLine()
            }
            JSONObject(stringBuilder.toString())
        } catch (e: Exception) {
            // Handle exceptions like malformed JSON or IO errors
            e.printStackTrace()
            null
        }
    }

        /** There are several different types of Intent that we handle in our app

         geo: These come from clicking on a location in another app e.g. Google Calendar. It
         contains a latitude and longitude and an optional text description.

         soundscape: This is our own format and we can do what we want with it. Initially it was
         the same format as geo but put the app into 'Street Preview' mode with the user
         positioned at the location provided.

         shared plain/text : If a user selects 'share' in Google Maps and Soundscape as the
         destination app, then we receive a Google Maps URL via this type of intent. To use it
         we need to follow it to get the real (non-tiny) URL and then pass that into the
         Android Geocoder to parse it.

         The behaviour for all of these URLs is now to open a LocationDetails screen which then
         gives the options of:
         Create Beacon
         Street Preview
         Add Marker

         Navigation to the LocationDetails is done via the main activity navigator.
         */
        fun parse(
            intent: Intent,
            mainActivity: MainActivity,
        ) {
            when {
                intent.action == Intent.ACTION_SEND -> {
                    if ("text/plain" == intent.type) {
                        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { plainText ->
                            Log.d(TAG, "Intent text: $plainText")
                            if (plainText.contains("maps.app.goo.gl")) {
                                try {
                                    getRedirectUrl(plainText, mainActivity)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Exception: $e")
                                }
                            }
                        }
                    }
                }

                else -> {
                    val uriData: String =
                        URLDecoder.decode(intent.data.toString(), Charsets.UTF_8.name())

                    // Check for geo or soundscape intent which is simply a latitude and longitude
                    val regex =
                        Regex("(geo|soundscape):/*([-+]?[0-9]*\\.[0-9]+|[0-9]+),([-+]?[0-9]*\\.[0-9]+|[0-9]+).*")

                    val matchResult = regex.find(uriData)
                    if (matchResult != null) {
                        // We have a match on the link
                        val latitude = matchResult.groupValues[2]
                        val longitude = matchResult.groupValues[3]

                        if (matchResult.groupValues[1] == "soundscape") {
                            // Switch to Street Preview mode
                            mainActivity.soundscapeServiceConnection.setStreetPreviewMode(
                                true,
                                LngLatAlt(longitude.toDouble(), latitude.toDouble())
                            )
                        } else {
                            try {
                                check(Geocoder.isPresent())
                                useGeocoderToGetAddress("$latitude,$longitude", mainActivity)
                            } catch (e: Exception) {
                                // No Geocoder available, so just report the uriData
                                val ld =
                                    LocationDescription(
                                        name = URLEncoder.encode(uriData, "utf-8"),
                                        location = LngLatAlt(longitude.toDouble(), latitude.toDouble())
                                    )
                                mainActivity.navigator.navigate(generateLocationDetailsRoute(ld))
                            }
                        }
                    } else {
                        if (Intent.ACTION_VIEW == intent.action || Intent.ACTION_MAIN == intent.action) {
                            val data = intent.data
                            if (data != null) {
                                if ("file" == data.scheme) {
                                    if (data.path != null) {
                                        Log.e(TAG, "Import data from ${data.path}")
                                    }
                                } else if ("content" == data.scheme) {
                                    Log.d(TAG, "Import data from content ${data.path}")
                                    val uri = intent.data
                                    if (uri != null) {
                                        try {
                                            val input =
                                                mainActivity.contentResolver.openInputStream(uri)

                                            if (input != null) {
                                                val routeData: RouteData?
                                                if((intent.type == "application/json") or (intent.type == "application/octet-stream")) {
                                                    // This might be a saved route shared from iOS.
                                                    // We want to translate this into a RouteData
                                                    // format to write to our database.
                                                    val json = inputStreamToJson(input) ?: throw Exception("Failed to parse JSON")

                                                    routeData = RouteData()
                                                    val routeName = json.getString("name")
                                                    routeData.name = routeName

                                                    // Hand crafted decode based on sample file...
                                                    val waypoints = json.getJSONArray("waypoints")
                                                    for(i in 0 until waypoints.length()) {
                                                        val item = waypoints.get(i) as JSONObject
                                                        val marker = item.get("marker")
                                                        if(marker is JSONObject) {
                                                            val location = marker.get("location") as JSONObject
                                                            val name = location.getString("name")
                                                            val coordinate = location.get("coordinate") as JSONObject
                                                            val latitude = coordinate.get("latitude") as Double
                                                            val longitude = coordinate.get("longitude") as Double
                                                            // Soundscape community doesn't have estimatedAddress,
                                                            // but STA iOS Soundscape does
                                                            var estimatedAddress = ""
                                                            if(marker.has("estimatedAddress"))
                                                                estimatedAddress = marker.getString("estimatedAddress")
                                                            routeData.waypoints.add(
                                                                MarkerData(name, Location(latitude, longitude), estimatedAddress)
                                                            )
                                                        }
                                                    }

                                                } else {
                                                    routeData = parseGpxFile(input)
                                                }
                                                // If no exception was thrown, then the parsing has
                                                // succeeded, pass the data to the new route screen.
                                                mainActivity.navigator.navigate(generateRouteDetailsRoute(routeData))
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to import file from intent: $e")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        companion object {
            private const val TAG = "SoundscapeIntents"
        }
    }
