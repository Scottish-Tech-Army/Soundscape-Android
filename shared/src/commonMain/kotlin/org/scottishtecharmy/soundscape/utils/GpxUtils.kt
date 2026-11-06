package org.scottishtecharmy.soundscape.utils

import org.scottishtecharmy.soundscape.database.local.model.MarkerEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.database.local.model.RouteWithMarkers
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.parseGpx
import org.scottishtecharmy.soundscape.logging.AppLog

private val logger = AppLog.tag("gpx")

fun parseGpxFile(input: String): RouteWithMarkers? {
    logger.d { "Parsing GPX file" }
    try {
        val parsedGpx = parseGpx(input)
        val waypoints = mutableListOf<MarkerEntity>()

        // We're parsing WayPoints and RoutePoints here. RoutePoints is the way that iOS
        // Soundscape GPX are written and is intended use. However apps like RideWithGps
        // generate WayPoints when exporting GPX and it's useful to support those if no
        // RoutePoints are found.

        parsedGpx.routes.forEach { route ->
            logger.d { "RoutePoint " + route.routeName }
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
        if (waypoints.isEmpty()) {
            parsedGpx.waypoints.forEach { waypoint ->
                logger.d { "WayPoint " + waypoint.name }
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
        return RouteWithMarkers(
            RouteEntity(
                name = parsedGpx.metadata.name,
                description = parsedGpx.metadata.desc,
            ),
            waypoints
        )
    } catch (e: Exception) {
        logger.e(e) { "Exception whilst parsing GPX file" }
        e.printStackTrace()
    }

    return null
}
