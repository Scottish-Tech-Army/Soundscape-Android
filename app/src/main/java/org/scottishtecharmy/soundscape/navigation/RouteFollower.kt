package org.scottishtecharmy.soundscape.navigation

import org.scottishtecharmy.soundscape.geoengine.utils.distance
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class RouteFollowerConfig(
    val offRouteThresholdMeters: Double = 30.0,
    val arrivalThresholdMeters: Double = 12.0,
    val rerouteDebounceMillis: Long = 5_000
)

enum class RouteProgressStatus {
    ON_ROUTE,
    OFF_ROUTE,
    ARRIVED
}

data class RouteProgressUpdate(
    val status: RouteProgressStatus,
    val isOffRoute: Boolean,
    val isArrived: Boolean,
    val nextInstructionIndex: Int?,
    val nextInstruction: NavigationInstruction?,
    val distanceFromRouteMeters: Double,
    val matchedGeometryIndex: Int
)

class RouteFollower(
    private val route: NavigationRoute,
    private val config: RouteFollowerConfig = RouteFollowerConfig()
) {
    private var latestGeometryIndex = 0

    fun update(location: NavigationPoint): RouteProgressUpdate {
        val destination = route.geometry.last()
        if (distanceMeters(location, destination) <= config.arrivalThresholdMeters) {
            latestGeometryIndex = route.geometry.lastIndex
            return RouteProgressUpdate(
                status = RouteProgressStatus.ARRIVED,
                isOffRoute = false,
                isArrived = true,
                nextInstructionIndex = null,
                nextInstruction = null,
                distanceFromRouteMeters = 0.0,
                matchedGeometryIndex = latestGeometryIndex
            )
        }

        val match = findNearestRouteMatch(location)
        latestGeometryIndex = max(latestGeometryIndex, match.geometryIndex)
        val instructionIndex = nextInstructionIndex(latestGeometryIndex)
        val isOffRoute = match.distanceMeters > config.offRouteThresholdMeters

        return RouteProgressUpdate(
            status = if (isOffRoute) RouteProgressStatus.OFF_ROUTE else RouteProgressStatus.ON_ROUTE,
            isOffRoute = isOffRoute,
            isArrived = false,
            nextInstructionIndex = instructionIndex,
            nextInstruction = instructionIndex?.let { route.instructions[it] },
            distanceFromRouteMeters = match.distanceMeters,
            matchedGeometryIndex = latestGeometryIndex
        )
    }

    private fun nextInstructionIndex(geometryIndex: Int): Int? {
        return route.instructions.indexOfFirst { instruction ->
            geometryIndex < instruction.geometryEndIndex
        }.takeIf { it >= 0 }
    }

    private fun findNearestRouteMatch(location: NavigationPoint): RouteMatch {
        if (route.geometry.size == 1) {
            return RouteMatch(
                distanceMeters = distanceMeters(location, route.geometry.first()),
                geometryIndex = 0
            )
        }

        return route.geometry.zipWithNext().mapIndexed { index, segment ->
            distanceToSegment(location, segment.first, segment.second, index)
        }.minBy { it.distanceMeters }
    }

    private fun distanceToSegment(
        location: NavigationPoint,
        start: NavigationPoint,
        end: NavigationPoint,
        segmentStartIndex: Int
    ): RouteMatch {
        val locationXY = location.toLocalMeters(start)
        val endXY = end.toLocalMeters(start)
        val segmentLengthSquared = endXY.x.pow(2) + endXY.y.pow(2)
        if (segmentLengthSquared == 0.0) {
            return RouteMatch(
                distanceMeters = distanceMeters(location, start),
                geometryIndex = segmentStartIndex
            )
        }

        val rawFraction = ((locationXY.x * endXY.x) + (locationXY.y * endXY.y)) / segmentLengthSquared
        val fraction = rawFraction.coerceIn(0.0, 1.0)
        val nearestX = endXY.x * fraction
        val nearestY = endXY.y * fraction
        val dx = locationXY.x - nearestX
        val dy = locationXY.y - nearestY

        return RouteMatch(
            distanceMeters = sqrt(dx.pow(2) + dy.pow(2)),
            geometryIndex = segmentStartIndex + fraction.roundToInt()
        )
    }

    private fun NavigationPoint.toLocalMeters(origin: NavigationPoint): LocalPoint {
        val latitudeScale = METERS_PER_DEGREE
        val longitudeScale = METERS_PER_DEGREE * cos(Math.toRadians(origin.latitude))
        return LocalPoint(
            x = (longitude - origin.longitude) * longitudeScale,
            y = (latitude - origin.latitude) * latitudeScale
        )
    }

    private fun distanceMeters(start: NavigationPoint, end: NavigationPoint): Double {
        return distance(start.latitude, start.longitude, end.latitude, end.longitude)
    }

    private data class RouteMatch(
        val distanceMeters: Double,
        val geometryIndex: Int
    )

    private data class LocalPoint(
        val x: Double,
        val y: Double
    )

    private companion object {
        private const val METERS_PER_DEGREE = 111_320.0
    }
}
