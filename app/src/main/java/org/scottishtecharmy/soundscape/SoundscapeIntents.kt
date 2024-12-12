package org.scottishtecharmy.soundscape

import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.scottishtecharmy.soundscape.database.local.RealmConfiguration
import org.scottishtecharmy.soundscape.database.local.dao.RoutesDao
import org.scottishtecharmy.soundscape.database.repository.RoutesRepository
import org.scottishtecharmy.soundscape.screens.home.Navigator
import org.scottishtecharmy.soundscape.screens.home.data.LocationDescription
import org.scottishtecharmy.soundscape.screens.home.locationDetails.generateLocationDetailsRoute
import org.scottishtecharmy.soundscape.utils.parseGpxFile
import java.io.IOException
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
                                    adressName = address.getAddressLine(0),
                                    latitude = address.latitude,
                                    longitude = address.longitude,
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
                            adressName = address.getAddressLine(0),
                            latitude = address.latitude,
                            longitude = address.longitude,
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
                                latitude.toDouble(),
                                longitude.toDouble(),
                            )
                        } else {
                            try {
                                check(Geocoder.isPresent())
                                useGeocoderToGetAddress("$latitude,$longitude", mainActivity)
                            } catch (e: Exception) {
                                // No Geocoder available, so just report the uriData
                                val ld =
                                    LocationDescription(
                                        adressName = URLEncoder.encode(uriData, "utf-8"),
                                        latitude = latitude.toDouble(),
                                        longitude = longitude.toDouble(),
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
                                                val routeData = parseGpxFile(input)

                                                // The parsing has succeeded, write the result to a
                                                // new markers database.
                                                // TODO: This intent should really open up the RouteDetails
                                                //  page and defer to that the action of inserting the
                                                //  route into the database. This is a temporary solution
                                                //  so that we can work on the code that uses the routes.
                                                val realm = RealmConfiguration.getMarkersInstance(true)
                                                val routesDao = RoutesDao(realm)
                                                val routesRepository = RoutesRepository(routesDao)
                                                runBlocking {
                                                    launch {
                                                        // Write the routeData to the database
                                                        Log.d("gpx", "Inserting route")
                                                        routesRepository.insertRoute(routeData)
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to import GPX from intent: $e")
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
