package org.scottishtecharmy.soundscape

import android.Manifest
import android.content.Intent
import android.net.http.HttpResponseCache
import android.os.Build
import android.os.Bundle
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import org.scottishtecharmy.soundscape.datastore.DataStoreManager
import org.scottishtecharmy.soundscape.datastore.DataStoreManager.PreferencesKeys.FIRST_LAUNCH

import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import org.scottishtecharmy.soundscape.screens.home.SetUpHomeNavGraph
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var dataStoreManager: DataStoreManager
    @Inject
    lateinit var soundscapeServiceConnection : SoundscapeServiceConnection

    data class DeviceLocation(
        var latitude : Double,
        var longitude : Double,
        var orientation : Double,
    )

    private lateinit var navController: NavHostController

    private var currentDeviceLocation by mutableStateOf<DeviceLocation?>(null)
    private var displayableTileString by mutableStateOf<String?>(null)

    //private var location by mutableStateOf<Location?>(null)
    //private var tileXY by mutableStateOf<Pair<Int, Int>?>(null)

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

        // Debug - dump preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        for(pref in sharedPreferences.all) {
            Log.d(TAG, "Preference: " + pref.key + " = " + pref.value)
        }

        var isFirstLaunch: Boolean
        runBlocking {
            isFirstLaunch = dataStoreManager.getValue(
                FIRST_LAUNCH,
                defaultValue = true
            )
        }

        Log.d(TAG, "isFirstLaunch: $isFirstLaunch")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
        }

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

        // Install HTTP cache this caches all of the UI tiles (at least?)
        try {
            val httpCacheDir = File(applicationContext.cacheDir, "http")
            val httpCacheSize = (100 * 1024 * 1024).toLong() // 100 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize)
        } catch (e: IOException) {
            Log.i("Injection", "HTTP response cache installation failed:$e")
        }

        checkAndRequestNotificationPermissions()
        soundscapeServiceConnection.tryToBindToServiceIfRunning()
        setContent {
            SoundscapeTheme {
                navController = rememberNavController()
                SetUpHomeNavGraph(
                    navController = navController
                )
            }
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

    fun toggleServiceState(newServiceState: Boolean) {

        if(!newServiceState) {
            soundscapeServiceConnection.soundscapeService?.stopForegroundService()
            soundscapeServiceConnection.soundscapeService = null
        }
        else {
            startSoundscapeService()
            soundscapeServiceConnection.tryToBindToServiceIfRunning()
        }
    }

    /**
     * Creates and starts the Soundscape foreground service.
     *
     * It also tries to bind to the service to update the UI with location updates.
     */
    private fun startSoundscapeService() {

        val serviceIntent = Intent(this, SoundscapeService::class.java)

        // Was the app started with a location that we should use?
        if (intent != null) {
            Log.d("intent", intent.data.toString())
            if (intent.data != null) {
                val uriData: String = URLDecoder.decode(intent.data.toString(), Charsets.UTF_8.name())

                // Check for geo intent which means to create a beacon at the provided location
                var regex =
                    Regex("geo:([-+]?[0-9]*\\.[0-9]+|[0-9]+),([-+]?[0-9]*\\.[0-9]+|[0-9]+).*")
                var matchResult = regex.find(uriData)
                if (matchResult != null) {
                    val latitude = matchResult.groupValues[1]
                    val longitude = matchResult.groupValues[2]

                    Log.d("intent", "beacon latitude: $latitude")
                    Log.d("intent", "beacon longitude: $longitude")

                    // We have a geo intent with a GPS position - use that as our location
                    serviceIntent.putExtra("beacon-latitude", latitude.toDouble())
                    serviceIntent.putExtra("beacon-longitude", longitude.toDouble())
                }
                else {
                    // Check for soundscape intent which can do more complex things. For now it just
                    // sets the mock location
                    regex =
                        Regex("soundscape://([-+]?[0-9]*\\.[0-9]+|[0-9]+),([-+]?[0-9]*\\.[0-9]+|[0-9]+).*")
                    matchResult = regex.find(uriData)
                    if (matchResult != null) {
                        val latitude = matchResult.groupValues[1]
                        val longitude = matchResult.groupValues[2]

                        Log.d("intent", "mock latitude: $latitude")
                        Log.d("intent", "mock longitude: $longitude")

                        // We have a geo intent with a GPS position - use that as our location
                        serviceIntent.putExtra("mock-latitude", latitude.toDouble())
                        serviceIntent.putExtra("mock-longitude", longitude.toDouble())
                    }
                }
            }
        }

        startForegroundService(serviceIntent)
    }
    companion object {
        private const val TAG = "MainActivity"

        // SharedPreference keys and default values
        const val ALLOW_CALLOUTS_DEFAULT = true
        const val ALLOW_CALLOUTS_KEY = "AllowCallouts"
        const val PLACES_AND_LANDMARKS_DEFAULT = true
        const val PLACES_AND_LANDMARKS_KEY = "PlaceAndLandmarks"
        const val MOBILITY_DEFAULT = true
        const val MOBILITY_KEY = "Mobility"
        const val DISTANCE_TO_BEACON_DEFAULT = true
        const val DISTANCE_TO_BEACON_KEY = "DistanceToBeacon"
    }
}