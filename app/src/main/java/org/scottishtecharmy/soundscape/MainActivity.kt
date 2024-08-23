package org.scottishtecharmy.soundscape

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import org.scottishtecharmy.soundscape.datastore.DataStoreManager
import org.scottishtecharmy.soundscape.datastore.DataStoreManager.PreferencesKeys.FIRST_LAUNCH

import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.scottishtecharmy.soundscape.screens.Home
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var dataStoreManager: DataStoreManager

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
            // Next, get the location permissions
            checkAndRequestLocationPermissions()
        }

    // we need location permission to be able to start the service
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted, service can run
                startSoundscapeService()
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

        installSplashScreen()

        if(isFirstLaunch) {
            // On the first launch, we want to take the user through the OnboardingActivity so
            // switch to it immediately.
            val intent = Intent(this, OnboardingActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()

            // No need to carry on with the rest of the initialization as we are switching activities
            return
        }

        setContent {
            SoundscapeTheme {
                Home()
            }
        }

        checkAndRequestNotificationPermissions()

        tryToBindToServiceIfRunning()
    }

    override fun onDestroy() {
        super.onDestroy()

        // If this was the first launch
        if(serviceBoundState) {
            unbindService(connection)
            serviceBoundState = false
        }
    }

    /**
     * Check for notification permission before starting the service so that the notification is visible
     */
    private fun checkAndRequestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )) {
                android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    checkAndRequestLocationPermissions()
                }

                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return
                }
            }
        }else{
            checkAndRequestLocationPermissions()
        }

    }
    private fun checkAndRequestLocationPermissions() {
        when (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )) {
            android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                // permission already granted
                startSoundscapeService()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    )
                )
            }
        }
    }

    fun stopServiceAndExit() {
        // service is already running, stop it
        soundscapeService?.stopForegroundService()
        // And exit application
        finishAndRemoveTask()
    }

    /**
     * Creates and starts the Soundscape foreground service.
     *
     * It also tries to bind to the service to update the UI with location updates.
     */
    private fun startSoundscapeService() {
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