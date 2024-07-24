package org.scottishtecharmy.soundscape

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.scottishtecharmy.soundscape.datastore.DataStoreManager
import org.scottishtecharmy.soundscape.datastore.DataStoreManager.PreferencesKeys.FIRST_LAUNCH
import org.scottishtecharmy.soundscape.screens.navigation.SetUpNavGraph

import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.utils.getXYTile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var dataStoreManager: DataStoreManager

    private lateinit var navController: NavHostController

    private var soundscapeService: SoundscapeService? = null

    private var serviceBoundState by mutableStateOf(false)
    private var displayableLocation by mutableStateOf<String?>(null)
    private var displayableOrientation by mutableStateOf<String?>(null)
    private var displayableTileString by mutableStateOf<String?>(null)

    private var location by mutableStateOf<Location?>(null)
    //private var tileXY by mutableStateOf<Pair<Int, Int>?>(null)



    // needed to communicate with the service.
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // we've bound to ExampleLocationForegroundService, cast the IBinder and get ExampleLocationForegroundService instance.
            Log.d(TAG, "onServiceConnected")

            val binder = service as SoundscapeService.LocalBinder
            soundscapeService = binder.getService()
            serviceBoundState = true

            onServiceConnected()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            // This is called when the connection with the service has been disconnected. Clean up.
            Log.d(TAG, "onServiceDisconnected")

            serviceBoundState = false
            soundscapeService = null
        }
    }

    // we need notification permission to be able to display a notification for the foreground service
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            // if permission was denied, the service can still run only the notification won't be visible
        }

    // we need location permission to be able to start the service
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted, service can run
                startForegroundService()
            }

            else -> {
                // No location access granted, service can't be started as it will crash
                Toast.makeText(this, "Fine Location permission is required.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var isFirstLaunch: Boolean

        runBlocking {
            isFirstLaunch = dataStoreManager.getValue(
                FIRST_LAUNCH,
                defaultValue = true
            )
        }
        Log.d(TAG, "isFirstLaunch: $isFirstLaunch")
        installSplashScreen()

        setContent {
            SoundscapeTheme {
                navController = rememberNavController()
                SetUpNavGraph(
                    navController = navController,
                    isFirstLaunch = isFirstLaunch
                )
            }
           /* ForegroundServiceScreen(
                serviceRunning = serviceBoundState,
                currentLocation = displayableLocation,
                currentOrientation = displayableOrientation,
                tileString = displayableTileString,
                location = location,
                onClick = ::onStartOrStopForegroundServiceClick
            )*/
        }

        checkAndRequestNotificationPermission()

        tryToBindToServiceIfRunning()




    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }

    /**
     * Check for notification permission before starting the service so that the notification is visible
     */
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            )) {
                android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    // permission already granted
                }

                else -> {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }


    private fun onStartOrStopForegroundServiceClick() {
        if (soundscapeService == null) {
            // service is not yet running, start it after permission check
            locationPermissionRequest.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
            )
        } else {
            // service is already running, stop it
            soundscapeService?.stopForegroundService()
        }
    }

    /**
     * Creates and starts the LocationService as a foreground service.
     *
     * It also tries to bind to the service to update the UI with location updates.
     */
    private fun startForegroundService() {
        // start the service
        startForegroundService(Intent(this, SoundscapeService::class.java))

        // bind to the service to update UI
        tryToBindToServiceIfRunning()
    }

    private fun tryToBindToServiceIfRunning() {
        Intent(this, SoundscapeService::class.java).also { intent ->
            bindService(intent, connection, 0)
        }
    }

    private fun onServiceConnected() {

        lifecycleScope.launch {
            // observe location updates from the service
            soundscapeService?.locationFlow?.map {
                it?.let { location ->
                    "Latitude: ${location.latitude}, Longitude: ${location.longitude} Accuracy: ${location.accuracy}"
                }
            }?.collectLatest {
                displayableLocation = it
            }
        }

        lifecycleScope.launch {
            soundscapeService?.orientationFlow?.map {
                it?.let {
                    orientation ->
                    "Device orientation: ${orientation.headingDegrees}"
                }
            }?.collect {
                displayableOrientation = it
            }
        }


        lifecycleScope.launch {
            delay(10000)
            val test = soundscapeService?.getTileGrid(application)

            println("Number of tiles in grid: ${test?.size}")
        }



    }

    companion object {
        private const val TAG = "MainActivity"
    }
}