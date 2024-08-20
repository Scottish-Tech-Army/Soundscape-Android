package org.scottishtecharmy.soundscape.viewmodels

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.SoundscapeServiceConnection
import org.scottishtecharmy.soundscape.utils.getGpsFromNormalizedMapCoordinates
import org.scottishtecharmy.soundscape.utils.getNormalizedFromGpsMapCoordinates
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.centerOnMarker
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.hasMarker
import ovh.plrapps.mapcompose.api.moveMarker
import ovh.plrapps.mapcompose.api.onLongPress
import ovh.plrapps.mapcompose.api.onMarkerLongPress
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import java.net.URL
import javax.inject.Inject
import kotlin.math.pow
import androidx.compose.material.icons.rounded.Navigation

@HiltViewModel
class HomeViewModel @Inject constructor(private val soundscapeServiceConnection : SoundscapeServiceConnection): ViewModel() {

    private val tileStreamProvider = TileStreamProvider { row, col, zoomLvl ->
        val style = "atlas"
        val apikey = BuildConfig.TILE_PROVIDER_API_KEY

        try {
            URL("https://tile.thunderforest.com/$style/$zoomLvl/$col/$row.png?apikey=$apikey").openStream()
        } catch (e : Exception) {
            Log.e("TileProvider", "Exception $e")
            null
        }
    }

    private var serviceConnection : SoundscapeServiceConnection? = null
    private var x : Double = 0.0
    private var y : Double = 0.0
    private var heading : Float = 0.0F

    private val maxLevel = 20
    private val minLevel = 5
    private val mapSize = mapSizeAtLevel(maxLevel, tileSize = 256)

    private fun mapSizeAtLevel(wmtsLevel: Int, tileSize: Int): Int {
        return tileSize * 2.0.pow(wmtsLevel).toInt()
    }

    val state = MapState(levelCount = maxLevel + 1, mapSize, mapSize, workerCount = 16) {
        minimumScaleMode(Forced((1 / 2.0.pow(maxLevel - minLevel)).toFloat()))
    }.apply {
        addLayer(tileStreamProvider)
        onMarkerLongPress { id, _, _ ->
            // A long press on the beacon marker will delete it and the audio beacon
            if(id == "beacon") {
                soundscapeServiceConnection.soundscapeService?.destroyBeacon()
                removeMarker("beacon")
            }
        }
        onLongPress { x, y ->
            // A long press for now will add an audio beacon at that point
            soundscapeServiceConnection.soundscapeService?.destroyBeacon()
            removeMarker("beacon")

            val coordinates = getGpsFromNormalizedMapCoordinates(x, y)
            soundscapeServiceConnection.soundscapeService?.createBeacon(coordinates.first, coordinates.second)
        }
        enableRotation()
        scale = 0.5f
    }

    private fun startMonitoringLocation() {
        Log.d(TAG, "ViewModel startMonitoringLocation")
        viewModelScope.launch {
            // Observe location updates from the service
            serviceConnection?.soundscapeService?.locationFlow?.collectLatest { value ->
                if (value != null) {
                    val coordinates = getNormalizedFromGpsMapCoordinates(
                        value.latitude,
                        value.longitude
                    )
                    x = coordinates.first
                    y = coordinates.second

                    if (!state.hasMarker("position")) {
                        state.addMarker("position", x, y) {
                            Icon(
                                imageVector = Icons.Rounded.Navigation,
                                contentDescription = null,
                                modifier = Modifier.rotate(heading),
                                tint = Color(0xCC2196F3)
                            )
                        }
                        state.centerOnMarker("position")
                    } else {
                        state.moveMarker("position", x, y)
                    }
                }
            }
        }
        viewModelScope.launch {
            // Observe orientation updates from the service
            serviceConnection?.soundscapeService?.orientationFlow?.collectLatest { value ->
                if (value != null) {
                    heading = value.headingDegrees
                    state.removeMarker("position")
                    state.addMarker("position", x, y) {
                        Icon(
                            //painter = painterResource(id = ),
                            imageVector = Icons.Rounded.Navigation,
                            contentDescription = null,
                            modifier = Modifier.rotate(heading),
                            tint = Color(0xCC2196F3)
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            // Observe beacon location update from the service so we can show it on the map
            serviceConnection?.soundscapeService?.beaconFlow?.collectLatest { value ->
                if (value != null) {
                    val coordinates = getNormalizedFromGpsMapCoordinates(
                        value.latitude,
                        value.longitude
                    )

                    if (!state.hasMarker("beacon")) {
                        state.addMarker("beacon", coordinates.first, coordinates.second) {
                            Icon(
                                painter = painterResource(id = R.drawable.nearby_markers_24px),
                                contentDescription = null,
                                tint = Color.Red
                            )
                        }
                    }
                    else {
                        state.moveMarker("beacon", coordinates.first, coordinates.second)
                    }
                }
            }
        }
    }

    init {
        serviceConnection = soundscapeServiceConnection
        viewModelScope.launch {
            soundscapeServiceConnection.serviceBoundState.collect {
                Log.d(TAG, "serviceBoundState $it")
                if(it) {
                    startMonitoringLocation()
                }
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}