package org.scottishtecharmy.soundscape.utils

import android.util.Log
import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx
import org.scottishtecharmy.soundscape.database.local.model.Location
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.database.local.model.RoutePoint
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

fun parseGpxFile(input : InputStream) : RouteData {
    Log.d("gpx", "Parsing GPX file")
    val routeData = RouteData()
    val parser = GPXParser()
    try {
        val parsedGpx: Gpx? = parser.parse(input)
        parsedGpx?.let {

            routeData.name = parsedGpx.metadata.name?: ""
            routeData.description = parsedGpx.metadata.desc?: ""

            // We're parsing WayPoints and RoutePoints here. RoutePoints is the way that iOS
            // Soundscape GPX are written and is intended use. However apps like RideWithGps
            // generate WayPoints when exporting GPX and it's useful to support those if no
            // RoutePoints are found.

            parsedGpx.routes.forEach { route ->
                Log.d("gpx", "RoutePoint " + route.routeName)
                route.routePoints.forEach { waypoint ->
                    val point = RoutePoint(waypoint.name, Location(waypoint.latitude, waypoint.longitude))
                    routeData.waypoints.add(point)
                }
            }
            if(routeData.waypoints.isEmpty()) {
                parsedGpx.wayPoints.forEach { waypoint ->
                    Log.d("gpx", "WayPoint " + waypoint.name)
                    val point = RoutePoint(waypoint.name, Location(waypoint.latitude, waypoint.longitude))
                    routeData.waypoints.add(point)
                }
            }
        } ?: {
            Log.e("gpx", "Error parsing GPX file")
        }
    } catch (e: IOException) {
        Log.e("gpx", "IOException whilst parsing GPX file")
        e.printStackTrace()
    } catch (e: XmlPullParserException) {
        Log.e("gpx", "XmlPullParserException whilst parsing GPX file")
        e.printStackTrace()
    }

    return routeData
}
