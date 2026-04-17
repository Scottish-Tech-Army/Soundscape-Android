package org.scottishtecharmy.soundscape.locationprovider

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.GpxData
import org.scottishtecharmy.soundscape.geoengine.utils.gpx.parseGpx
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.GeodesicRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import java.io.InputStream

class GpxDrivenProvider  {

    var locationProvider = StaticLocationProvider(LngLatAlt())
    var directionProvider = DirectionProvider()

    private var parsedGpx: GpxData? = null
    private val coroutineScope = CoroutineScope(Job())
    private var trackPointIndex = 0

    private var stepsInPoint = 0
    private var currentStep = 0
    private var latStep = 0.0
    private var lngStep = 0.0

    private val msWait = 1000.0
    private val walkingSpeed = 1.0

    fun start(context : Context) {
        val input = context.assets.open("gpx/milngavie-centre.gpx")
        parseGpxStream(input)

        coroutineScope.launch {
            val ruler = GeodesicRuler()
            while (true) {
                val point = parsedGpx?.tracks?.getOrNull(0)?.trackSegments?.getOrNull(0)?.trackPoints?.getOrNull(trackPointIndex)

                var heading = 0.0
                point?.let {
                    if(stepsInPoint == 0) {
                        val pointLngLatAlt = LngLatAlt(it.longitude, it.latitude)
                        val nextPoint =
                            parsedGpx?.tracks?.getOrNull(0)?.trackSegments?.getOrNull(0)?.trackPoints?.getOrNull(
                                trackPointIndex + 1
                            )
                        nextPoint?.let { itNext ->
                            val nextPointLngLatAlt = LngLatAlt(itNext.longitude, itNext.latitude)
                            val distance = ruler.distance(pointLngLatAlt, nextPointLngLatAlt)
                            stepsInPoint = (distance / (walkingSpeed * (msWait/1000.0))).toInt()
                            if(stepsInPoint == 0) stepsInPoint = 1
                            currentStep = 0
                            latStep = (nextPoint.latitude - point.latitude) / stepsInPoint
                            lngStep = (nextPoint.longitude - point.longitude) / stepsInPoint

                            heading = bearingFromTwoPoints(
                                LngLatAlt(point.longitude, point.latitude),
                                LngLatAlt(nextPoint.longitude, nextPoint.latitude))

                            val orientation = DeviceDirection(
                                attitude = FloatArray(4),
                                headingDegrees = heading.toFloat(),
                                headingAccuracyDegrees = 0.0F,
                                elapsedRealtimeNanos = 1000000
                            )
                            directionProvider.mutableOrientationFlow.value = orientation
                        }
                    }
                    // Interpolate next point location
                    val interpolatedPoint = LngLatAlt(
                        point.longitude + (lngStep * currentStep),
                        point.latitude + (latStep * currentStep)
                    )
                    locationProvider.updateLocation(
                        LngLatAlt(
                            interpolatedPoint.longitude,
                            interpolatedPoint.latitude
                        ),
                        heading.toFloat(),
                        walkingSpeed.toFloat()
                    )
                    directionProvider.audioEngine?.updateGeometry(
                        interpolatedPoint.latitude,
                        interpolatedPoint.longitude,
                        heading,
                        focusGained = true,
                        duckingAllowed = false,
                        15.0
                    )

                    currentStep++
                    if (currentStep == stepsInPoint) {
                        trackPointIndex++
                        if (trackPointIndex >= (parsedGpx?.tracks?.getOrNull(0)?.trackSegments?.getOrNull(0)?.trackPoints?.size
                                ?: 0)
                        ) {
                            trackPointIndex = 0
                        }
                        stepsInPoint = 0
                    }
                }
                delay(msWait.toLong())
            }
        }
    }

    fun parseGpxStream(input : InputStream) {
        Log.d(TAG, "Parsing GPX file")

        try {
            parsedGpx = parseGpx(input.bufferedReader().readText())
            parsedGpx?.let { gpx ->
                gpx.tracks.forEach { track ->
                    track.trackSegments.forEach { segment ->
                        segment.trackPoints.forEach { trackPoint ->
                            Log.d("gpx", "TrackPoint: ${trackPoint.time} ${trackPoint.latitude} ${trackPoint.longitude}")
                        }
                    }
                }
            } ?: {
                Log.e(TAG, "Error parsing GPX file")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception whilst parsing GPX file: ${e.message}")
            e.printStackTrace()
        }
    }
    companion object {
        private const val TAG = "GpxDrivenProvider"
    }
}
