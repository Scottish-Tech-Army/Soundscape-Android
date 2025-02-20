package org.scottishtecharmy.soundscape

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.DeviceOrientation
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.scottishtecharmy.soundscape.database.local.model.RouteData
import org.scottishtecharmy.soundscape.geoengine.StreetPreviewState
import org.scottishtecharmy.soundscape.geojsonparser.geojson.LngLatAlt
import org.scottishtecharmy.soundscape.services.SoundscapeBinder
import org.scottishtecharmy.soundscape.services.SoundscapeService
import javax.inject.Inject

@ActivityRetainedScoped
class SoundscapeServiceConnection @Inject constructor() {
    var soundscapeService: SoundscapeService? = null

    private var _serviceBoundState = MutableStateFlow(false)
    val serviceBoundState = _serviceBoundState.asStateFlow()

    // Simplify access of flows
    fun getLocationFlow() : StateFlow<android.location.Location?>? {
        return soundscapeService?.locationProvider?.locationFlow
    }
    fun getOrientationFlow() : StateFlow<DeviceOrientation?>? {
        return soundscapeService?.directionProvider?.orientationFlow
    }
    fun getBeaconFlow(): StateFlow<LngLatAlt?>? {
        return soundscapeService?.beaconFlow
    }
    fun getStreetPreviewModeFlow(): StateFlow<StreetPreviewState>? {
        return soundscapeService?.streetPreviewFlow
    }
    fun getCurrentRouteFlow(): StateFlow<RouteData?>? {
        return soundscapeService?.routePlayer?.currentRouteFlow
    }

    fun setStreetPreviewMode(on : Boolean, location: LngLatAlt? = null) {
        Log.d(TAG, "setStreetPreviewMode $on")
        soundscapeService?.setStreetPreviewMode(on, location)
    }

    fun startRoute(routeName: String) {
        soundscapeService?.startRoute(routeName)
    }

    // needed to communicate with the service.
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // we've bound to ExampleLocationForegroundService, cast the IBinder and get ExampleLocationForegroundService instance.
            Log.d(TAG, "onServiceConnected")

            val binder = service as SoundscapeBinder
            soundscapeService = binder.getSoundscapeService()
            _serviceBoundState.value = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            // This is called when the connection with the service has been disconnected. Clean up.
            Log.d(TAG, "onServiceDisconnected")
            _serviceBoundState.value = false
            soundscapeService = null
        }
    }

    fun tryToBindToServiceIfRunning(context : Context) {
        Log.d(TAG, "tryToBindToServiceIfRunning " + serviceBoundState.value)

        if(!serviceBoundState.value) {
            Intent(context, SoundscapeService::class.java).also { intent ->
                context.bindService(intent, connection, 0)
            }
        }
    }

    fun stopService() {
        Log.d(TAG, "stopService")
        soundscapeService?.stopForegroundService()
    }

    fun streetPreviewGo() {
        soundscapeService?.streetPreviewGo()
    }

    companion object {
        private const val TAG = "SoundscapeServiceConnection"
    }
}