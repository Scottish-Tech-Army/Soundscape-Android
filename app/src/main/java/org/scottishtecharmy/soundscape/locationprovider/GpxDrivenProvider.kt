package org.scottishtecharmy.soundscape.locationprovider

import android.content.Context
import android.util.Log
import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.geoengine.utils.bearingFromTwoPoints
import org.scottishtecharmy.soundscape.geoengine.utils.rulers.GeodesicRuler
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

class GpxDrivenProvider  {

    var locationProvider = StaticLocationProvider(LngLatAlt())
    var directionProvider = DirectionProvider()

    private var parsedGpx: Gpx? = null
    private val coroutineScope = CoroutineScope(Job())
    private var trackPointIndex = 0

    private var stepsInPoint = 0
    private var currentStep = 0
    private var latStep = 0.0
    private var lngStep = 0.0

    private val msWait = 100.0
    private val walkingSpeed = 10.0

    fun start(context : Context) {
        val input = context.assets.open("gpx/rideWithGps.gpx")
        parseGpx(input)

        coroutineScope.launch {
            val ruler = GeodesicRuler()
            while (true) {
                val point = parsedGpx?.tracks?.get(0)?.trackSegments?.get(0)?.trackPoints?.get(trackPointIndex)

                var heading = 0.0
                point?.let { it ->
                    if(stepsInPoint == 0) {
                        val pointLngLatAlt = LngLatAlt(it.longitude, it.latitude)
                        val nextPoint =
                            parsedGpx?.tracks?.get(0)?.trackSegments?.get(0)?.trackPoints?.get(
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
                        if (trackPointIndex >= (parsedGpx?.tracks?.get(0)?.trackSegments?.get(0)?.trackPoints?.size
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

    fun parseGpx(input : InputStream) {
        Log.d(TAG, "Parsing GPX file")

        val parser = GPXParser()
        try {
            parsedGpx = parser.parse(input)
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
        } catch (e: IOException) {
            Log.e(TAG, "IOException whilst parsing GPX file")
            e.printStackTrace()
        } catch (e: XmlPullParserException) {
            Log.e(TAG,  "XmlPullParserException whilst parsing GPX file")
            e.printStackTrace()
        }
    }
    companion object {
        private const val TAG = "GpxDrivenProvider"
    }
}