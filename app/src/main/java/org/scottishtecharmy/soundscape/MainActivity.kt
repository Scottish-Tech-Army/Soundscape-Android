package org.scottishtecharmy.soundscape

import android.Manifest
import android.content.Intent
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
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SERVER_BASE
import org.scottishtecharmy.soundscape.geoengine.PROTOMAPS_SERVER_PATH
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.HomeScreen
import org.scottishtecharmy.soundscape.screens.home.Navigator
import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.utils.extractAssets
import java.io.File
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

    inner class AppLifecycleObserver : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_START -> {
                    soundscapeServiceConnection.soundscapeService?.appInForeground(true)
                }
                Lifecycle.Event.ON_STOP -> {
                    soundscapeServiceConnection.soundscapeService?.appInForeground(false)
                }
                else -> {
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract the maplibre style assets
        Log.d("ExtractAssets", "Start extraction")
        extractAssets(applicationContext, "osm-bright-gl-style","osm-bright-gl-style")
        Log.d("ExtractAssets", "Completed extraction")

        // Update extracted style.json with protomaps server URI
        val filesDir = applicationContext.filesDir.toString()
        val outputStyleStream = File("$filesDir/osm-bright-gl-style/processedstyle.json").outputStream()
        val inputStyleStream = File("$filesDir/osm-bright-gl-style/style.json").inputStream()
        inputStyleStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                if(line.contains("PROTOMAPS_SERVER_URL")) {
                    val newline = line.replace("PROTOMAPS_SERVER_URL", "$PROTOMAPS_SERVER_BASE/$PROTOMAPS_SERVER_PATH.json")
                    outputStyleStream.write(newline.toByteArray())
                }
                else {
                    outputStyleStream.write(line.toByteArray())
                }
            }
        }
        inputStyleStream.close()
        outputStyleStream.close()

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

        // When opening a JSON file containing a route from Android File we can end up with two
        // instances of the app running. This check ensures that we have only one instance.
        if (!isTaskRoot) {
            val newIntent = Intent(intent)
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(newIntent)
            finish()
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

        checkAndRequestNotificationPermissions()
        soundscapeServiceConnection.tryToBindToServiceIfRunning(applicationContext)

        lifecycleScope.launch {
            soundscapeServiceConnection.serviceBoundState.collect {
                Log.d(TAG, "serviceBoundState $it")
                if (it) {
                    // The service has started

                    // Update the app state in the service
                    this@MainActivity.lifecycle.addObserver(AppLifecycleObserver())

                    // Parse any Intent
                    if(intent != null) {
                        if (intent.action != "") {
                            soundscapeIntents.parse(intent, this@MainActivity)
                            // Clear the action so that it doesn't happen on every screen rotate etc.
                            intent.action = ""
                        }
                    }
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

    fun setServiceState(newServiceState: Boolean) {
        if(!newServiceState) {
            soundscapeServiceConnection.stopService()
        }
        else {
            startSoundscapeService()
            soundscapeServiceConnection.tryToBindToServiceIfRunning(applicationContext)
        }
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