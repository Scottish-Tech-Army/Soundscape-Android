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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.HomeScreen
import org.scottishtecharmy.soundscape.screens.home.Navigator
import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import java.io.File
import java.io.IOException
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var soundscapeServiceConnection : SoundscapeServiceConnection
    @Inject
    lateinit var navigator : Navigator
    @Inject
    lateinit var soundscapeIntents : SoundscapeIntents

    data class DeviceLocation(
        var latitude : Double,
        var longitude : Double,
        var orientation : Double,
    )

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
        for (pref in sharedPreferences.all) {
            Log.d(TAG, "Preference: " + pref.key + " = " + pref.value)
        }

        val isFirstLaunch = sharedPreferences.getBoolean(FIRST_LAUNCH_KEY, true)

        Log.d(TAG, "isFirstLaunch: $isFirstLaunch")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
        }

        if (isFirstLaunch) {
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
        soundscapeServiceConnection.tryToBindToServiceIfRunning(applicationContext)

        lifecycleScope.launch {
            soundscapeServiceConnection.serviceBoundState.collect {
                Log.d(TAG, "serviceBoundState $it")
                if (it) {
                    // The service has started, so parse the Intent
                    if(intent != null) {
                        if (intent.action != "") {
                            soundscapeIntents.parse(intent, this@MainActivity)
                            // Clear the action so that it doesn't happen on every screen rotate etc.
                            intent.action = ""
                        }
                    }

                    // Pick the current route
                    setupCurrentRoute()
                }
            }
        }

        setContent {
            SoundscapeTheme {
                val navController = rememberNavController()
                val destination by navigator.destination.collectAsState()
                LaunchedEffect(destination) {
                    if(destination != "") {
                        if (navController.currentDestination?.route != destination) {
                            navController.navigate(destination)
                        }
                    }
                }
                HomeScreen(
                    navController = navController,
                    rateSoundscape = {
                        this.rateSoundscape()
                    }
                )
            }
        }
    }

    private fun rateSoundscape() {
        val reviewManager = ReviewManagerFactory.create(this)
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // We got the ReviewInfo object
                val reviewInfo = task.result

                val flow = reviewManager.launchReviewFlow(this, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.
                }
            } else {
                // There was some problem, log or handle the error code.
                @ReviewErrorCode val reviewErrorCode = (task.exception as ReviewException).errorCode
                Log.e(TAG, "Error requesting review: $reviewErrorCode")
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")
        // Pop up to home page
        navigator.navigate(HomeRoutes.Home.route)
        // And then parse the new intent which may take us to the LocationDetails screen
        soundscapeIntents.parse(intent, this@MainActivity)
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
            soundscapeServiceConnection.stopService(applicationContext)
        }
        else {
            startSoundscapeService()
            soundscapeServiceConnection.tryToBindToServiceIfRunning(applicationContext)
        }
    }

    fun setupCurrentRoute() {
        soundscapeServiceConnection.soundscapeService?.setupCurrentRoute()
    }

    /**
     * Creates and starts the Soundscape foreground service.
     *
     * It also tries to bind to the service to update the UI with location updates.
     */
    private fun startSoundscapeService() {
        val serviceIntent = Intent(this, SoundscapeService::class.java)
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
        const val UNNAMED_ROADS_DEFAULT = false
        const val UNNAMED_ROADS_KEY = "UnnamedRoads"
        const val BEACON_TYPE_DEFAULT = "Classic"
        const val BEACON_TYPE_KEY = "BeaconType"
        const val VOICE_TYPE_DEFAULT = "Default"
        const val VOICE_TYPE_KEY = "VoiceType"
        const val SPEECH_RATE_DEFAULT = 1.0f
        const val SPEECH_RATE_KEY = "SpeechRate"

        const val FIRST_LAUNCH_KEY = "FirstLaunch"
    }
}