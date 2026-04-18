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
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.Navigator
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import org.scottishtecharmy.soundscape.screens.markers_routes.screens.addandeditroutescreen.generateRouteDetailsRoute
import org.scottishtecharmy.soundscape.utils.AnalyticsProvider
import org.scottishtecharmy.soundscape.utils.fuzzyCompare
import org.scottishtecharmy.soundscape.utils.parseGpxFile
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
class SoundscapeIntents(
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
                        Log.d(TAG, "getFromLocationName $location has ${addresses.size} result")
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
        context: Context?,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            getRedirectUrlSync(url, context)
        }
    }

    fun getRedirectUrlSync(
        url: String,
        context: Context?,
    ) : String {
        var urlTmp: URL?
        var connection: HttpURLConnection?

        try {
            Log.d(TAG, "Open URL $url")
            urlTmp = URL(url)

            Log.d(TAG, "Open connection")
            connection = urlTmp.openConnection() as HttpURLConnection
            Log.d(TAG, "Response ${connection.responseCode}")

            val redUrl = connection.url.toString()
            connection.disconnect()

            if(connection.responseCode != 200)
                return ""

            // Parse URL
            Log.d(TAG, "Maps URL: $redUrl")
            val decodedUrl = URLDecoder.decode(redUrl, "UTF-8")
            Log.d(TAG, "Decoded maps URL: $decodedUrl")

            // The URL will have the text description of the location, followed by some data
            // which includes the FTID of the place. The FTID is a Google place identifier, but
            // that isn't useful unless we pay Google for an API key and use the API to decode
            // it. We'll just try and use text description instead.

            // Strip off initial https://www.google.com/maps/place/ and anything after "/data="
            val placeName =
                decodedUrl.substringAfter("maps/place/").substringBefore("/")
            Log.d(TAG, "Place name: $placeName")
            if (context == null)
                return placeName

            // For unit tests we pass in a null context and so don't get this far
            useGeocoderToGetAddress(placeName, context)
            return placeName

        } catch (e: IOException) {
            e.printStackTrace()
            return ""
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

         soundscape: This is our own format, and we can do what we want with it. It can be in the
         same format as geo: but forces opening with Soundscape rather than the app associated with
         the geo: URL.

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
                                AnalyticsProvider.getInstance().logEvent("intentGoogleMapShare", null)
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
                    val uri = intent.data ?: return
                    val scheme = uri.scheme
                    val path = uri.path

                    // Check for soundscape://feature/{name} intent to open a feature screen
                    if (scheme == "soundscape" && uri.host == "feature") {
                        val feature = path?.removePrefix("/")
                        if (feature == "routes" || feature == "markers") {
                            Log.d(TAG, "Opening feature from intent: $feature")
                            AnalyticsProvider.getInstance().logEvent("intentOpenFeature", null)
                            navigator.navigate("${HomeRoutes.MarkersAndRoutes.route}?tab=$feature")
                        }
                        return
                    }

                    // Check for soundscape://route/stop intent to stop route playback
                    if (scheme == "soundscape" && uri.host == "route" && path == "/stop") {
                        Log.d(TAG, "Stopping route from intent")
                        AnalyticsProvider.getInstance().logEvent("intentStopRoute", null)
                        mainActivity.soundscapeServiceConnection.routeStop()
                        return
                    }

                    // Check for soundscape://route/{name} intent to start a saved route
                    if (scheme == "soundscape" && uri.host == "route") {
                        val routeName = path?.removePrefix("/")
                        if (!routeName.isNullOrBlank()) {
                            Log.d(TAG, "Starting route from intent: name=$routeName")
                            val db = org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabase
                                .getMarkersInstance(mainActivity)
                            val route = db.routeDao().getAllRoutes()
                                .map { it to routeName.fuzzyCompare(it.name, true) }
                                .filter { it.second < 0.3 }
                                .minByOrNull { it.second }
                                ?.first
                            if (route != null) {
                                Log.d(TAG, "Matched route: ${route.name} (id=${route.routeId})")
                                AnalyticsProvider.getInstance().logEvent("intentStartRoute", null)
                                mainActivity.soundscapeServiceConnection.routeStart(route.routeId)
                            } else {
                                Log.w(TAG, "No route found matching name: $routeName")
                            }
                        }
                        return
                    }

                    // Check for geo or soundscape intent with latitude,longitude
                    if (scheme == "geo" || scheme == "soundscape") {
                        val ssp = uri.schemeSpecificPart ?: return
                        val coordinateRegex =
                            Regex("/*(-?[0-9]+\\.?[0-9]*),(-?[0-9]+\\.?[0-9]*)")
                        val matchResult = coordinateRegex.find(ssp)
                        if (matchResult != null) {
                            val latitude = matchResult.groupValues[1]
                            val longitude = matchResult.groupValues[2]
                            try {
                                AnalyticsProvider.getInstance().logEvent("intentGeoSchemaUrl", null)
                                check(Geocoder.isPresent())
                                useGeocoderToGetAddress("$latitude,$longitude", mainActivity)
                            } catch (e: Exception) {
                                val ld =
                                    LocationDescription(
                                        name = "$latitude,$longitude",
                                        location = LngLatAlt(longitude.toDouble(), latitude.toDouble())
                                    )
                                mainActivity.navigator.navigate(generateLocationDetailsRoute(ld))
                            }
                            return
                        }
                    }

                    // Use intent.data (android.net.Uri) for safe per-parameter decoding
                    // rather than decoding the whole URI string which can corrupt structure
                    val shareMarkerUri = intent.data
                    if (shareMarkerUri != null &&
                        shareMarkerUri.path == "/v1/sharemarker") {
                        val lat = shareMarkerUri.getQueryParameter("lat")?.toDoubleOrNull()
                        val lon = shareMarkerUri.getQueryParameter("lon")?.toDoubleOrNull()
                        if (lat != null && lon != null) {
                            // Prefer "nickname" over "name" as the display name
                            val nickname = shareMarkerUri.getQueryParameter("nickname")
                            val name = shareMarkerUri.getQueryParameter("name")
                            val displayName = when {
                                !nickname.isNullOrBlank() -> nickname
                                !name.isNullOrBlank() -> name
                                else -> "$lat,$lon"
                            }
                            Log.d(TAG, "Share marker: name=$displayName lat=$lat lon=$lon")
                            AnalyticsProvider.getInstance().logEvent("intentShareMarker", null)
                            val ld = LocationDescription(
                                name = displayName,
                                location = LngLatAlt(lon, lat)
                            )
                            navigator.navigate(generateLocationDetailsRoute(ld))
                        } else {
                            Log.w(TAG, "Share marker missing valid lat/lon: $shareMarkerUri")
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
                                                val routeData: RouteWithMarkers?
                                                if((intent.type == "application/json") or (intent.type == "application/octet-stream")) {
                                                    // This might be a saved route shared from iOS.
                                                    // We want to translate this into a RouteData
                                                    // format to write to our database.

                                                    // Limit the size of routes to 1MB which would
                                                    // be several thousand markers.
                                                    if(input.available() > 1000000) {
                                                        throw Exception("File too large")
                                                    }
                                                    val json = inputStreamToJson(input) ?: throw Exception("Failed to parse JSON")

                                                    // Hand crafted decode based on sample file...
                                                    val waypoints = json.getJSONArray("waypoints")
                                                    val markers = mutableListOf<MarkerEntity>()
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
                                                            markers.add(
                                                                MarkerEntity(
                                                                    name = name,
                                                                    longitude = longitude,
                                                                    latitude = latitude,
                                                                    fullAddress = estimatedAddress
                                                                )
                                                            )
                                                        }
                                                    }
                                                    AnalyticsProvider.getInstance().logEvent("intentJsonImport", null)
                                                    routeData = RouteWithMarkers(
                                                        RouteEntity(
                                                            0,
                                                            json.getString("name"),
                                                            ""
                                                        ),
                                                        markers
                                                    )


                                                } else {
                                                    routeData = parseGpxFile(input)
                                                }
                                                // If no exception was thrown, then the parsing has
                                                // succeeded, pass the data to the new route screen.
                                                mainActivity.navigator.navigate(generateRouteDetailsRoute(routeData!!))
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
