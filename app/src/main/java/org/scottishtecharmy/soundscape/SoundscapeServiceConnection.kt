package org.scottishtecharmy.soundscape

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

import org.scottishtecharmy.soundscape.services.SoundscapeService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundscapeServiceConnection @Inject constructor(@ApplicationContext context: Context) {

    var soundscapeService: SoundscapeService? = null
    private val appContext = context

    private var _serviceBoundState = MutableStateFlow(false)
    val serviceBoundState = _serviceBoundState.asStateFlow()

    // needed to communicate with the service.
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // we've bound to ExampleLocationForegroundService, cast the IBinder and get ExampleLocationForegroundService instance.
            Log.d(TAG, "onServiceConnected")

            val binder = service as SoundscapeService.LocalBinder
            soundscapeService = binder.getService()
            _serviceBoundState.value = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            // This is called when the connection with the service has been disconnected. Clean up.
            Log.d(TAG, "onServiceDisconnected")

            _serviceBoundState.value = false
        }
    }

    fun create() {
        Log.d(TAG, "create")
        tryToBindToServiceIfRunning()
    }

    private fun destroy() {

        Log.d(TAG, "destroy")

        // If this was the first launch
        if(serviceBoundState.value) {
            appContext.unbindService(connection)
            _serviceBoundState.value = false
        }
    }

    fun stopServiceAndExit() {
        Log.d(TAG, "stopServiceAndExit")
        // service is already running, stop it
        soundscapeService?.stopForegroundService()

        destroy()
    }

    fun tryToBindToServiceIfRunning() {
        Log.d(TAG, "tryToBindToServiceIfRunning " + serviceBoundState.value)

        if(!serviceBoundState.value) {
            Intent(appContext, SoundscapeService::class.java).also { intent ->
                appContext.bindService(intent, connection, 0)
            }
        }
    }

    companion object {
        private const val TAG = "SoundscapeServiceConnection"
    }
}