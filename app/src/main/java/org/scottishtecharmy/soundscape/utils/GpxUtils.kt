package org.scottishtecharmy.soundscape.utils

import android.util.Log
import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.parseGpx
import java.io.InputStream

fun parseGpxFile(input : InputStream) : RouteWithMarkers? {
    Log.d("gpx", "Parsing GPX file")
    try {
        val parsedGpx = parseGpx(input.bufferedReader().readText())
        val waypoints = mutableListOf<MarkerEntity>()

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
                        fullAddress = waypoint.desc,
                        longitude = waypoint.longitude,
                        latitude = waypoint.latitude
                    )
                )
            }
        }
        if(waypoints.isEmpty()) {
            parsedGpx.waypoints.forEach { waypoint ->
                Log.d("gpx", "WayPoint " + waypoint.name)
                waypoints.add(
                    MarkerEntity(
                        name = waypoint.name,
                        fullAddress = waypoint.desc,
                        longitude = waypoint.longitude,
                        latitude = waypoint.latitude
                    )
                )
            }
        }
        val routeData = RouteWithMarkers(
            RouteEntity(
                name = parsedGpx.metadata.name,
                description = parsedGpx.metadata.desc,
            ),
            waypoints
        )
        return routeData

    } catch (e: Exception) {
        Log.e("gpx", "Exception whilst parsing GPX file: ${e.message}")
        e.printStackTrace()
    }

    return null
}
