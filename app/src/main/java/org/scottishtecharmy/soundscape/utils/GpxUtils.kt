package org.scottishtecharmy.soundscape.utils

import android.util.Log
import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

fun parseGpxFile(input : InputStream) : RouteWithMarkers? {
    Log.d("gpx", "Parsing GPX file")
    val parser = GPXParser()
    try {
        val parsedGpx: Gpx? = parser.parse(input)
        val waypoints = mutableListOf<MarkerEntity>()
        parsedGpx?.let {

            // We're parsing WayPoints and RoutePoints here. RoutePoints is the way that iOS
            // Soundscape GPX are written and is intended use. However apps like RideWithGps
            // generate WayPoints when exporting GPX and it's useful to support those if no
            // RoutePoints are found.

            parsedGpx.routes.forEach { route ->
                Log.d("gpx", "RoutePoint " + route.routeName)
                route.routePoints.forEach { waypoint ->
                    waypoints.add(
                        MarkerEntity(
                            name = waypoint.name,
                            longitude = waypoint.longitude,
                            latitude = waypoint.latitude
                        )
                    )
                }
            }
            if(waypoints.isEmpty()) {
                parsedGpx.wayPoints.forEach { waypoint ->
                    Log.d("gpx", "WayPoint " + waypoint.name)
                    waypoints.add(
                        MarkerEntity(
                            name = waypoint.name,
                            longitude = waypoint.longitude,
                            latitude = waypoint.latitude
                        )
                    )
                }
            }
        } ?: {
            Log.e("gpx", "Error parsing GPX file")
        }
        val routeData = RouteWithMarkers(
            RouteEntity(
                name = parsedGpx?.metadata?.name?: "",
                description = parsedGpx?.metadata?.desc?: "",
            ),
            waypoints
        )
        return routeData

    } catch (e: IOException) {
        Log.e("gpx", "IOException whilst parsing GPX file")
        e.printStackTrace()
    } catch (e: XmlPullParserException) {
        Log.e("gpx", "XmlPullParserException whilst parsing GPX file")
        e.printStackTrace()
    }

    return null
}
