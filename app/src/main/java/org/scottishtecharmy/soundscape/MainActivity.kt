package org.scottishtecharmy.soundscape

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.text.Html
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.geoengine.utils.ResourceMapper
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.AndroidGeocoder
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.HomeScreen
import org.scottishtecharmy.soundscape.screens.home.Navigator
import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.utils.LogcatHelper
import org.scottishtecharmy.soundscape.utils.getOfflineMapStorage
import org.scottishtecharmy.soundscape.utils.processMaps
import java.io.File
import java.util.Locale
import javax.inject.Inject

data class ThemeState(
    val hintsEnabled: Boolean = false,
    val themeIsLight: Boolean = true,
    val themeContrast: String = "High")

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var soundscapeServiceConnection : SoundscapeServiceConnection
    @Inject
    lateinit var navigator : Navigator
    @Inject
    lateinit var soundscapeIntents : SoundscapeIntents
    @Inject
    lateinit var audioTour : AudioTour

    init {
        Analytics.getInstance(false)
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

    private lateinit var sharedPreferences : SharedPreferences
    private lateinit var sharedPreferencesListener : SharedPreferences.OnSharedPreferenceChangeListener

    private val _themeStateFlow = MutableStateFlow(ThemeState())
    private var themeStateFlow: StateFlow<ThemeState> = _themeStateFlow

    private fun handlePreferenceChange(key: String?, preferences: SharedPreferences) {
        when (key) {
            THEME_LIGHTNESS_KEY -> {
                val themeLightness = preferences.getString(
                    THEME_LIGHTNESS_KEY,
                    THEME_LIGHTNESS_DEFAULT
                )
                var themeIsLight = (themeLightness == "Light")
                if(themeLightness == "Auto") {
                    themeIsLight = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_NO
                }

                // The state of the theme light/dark setting has changed, so update the UI
                _themeStateFlow.value = themeStateFlow.value.copy(themeIsLight = themeIsLight)

                Log.e(TAG, "themeIsLight $themeIsLight")
            }

            THEME_CONTRAST_KEY -> {
                val contrast = preferences.getString(THEME_CONTRAST_KEY, "High")!!
                _themeStateFlow.value = themeStateFlow.value.copy(themeContrast = contrast)
                Log.e(TAG, "themeContrast $contrast")
            }

            HINTS_KEY -> {
                _themeStateFlow.value = themeStateFlow.value.copy(
                    hintsEnabled = preferences.getBoolean(HINTS_KEY, HINTS_DEFAULT)
                )
            }
        }
    }

    private var locationPermissionGranted = -1
    private var notificationPermissionGranted = -1

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")

        val locationPermission = when (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        ) {
            android.content.pm.PackageManager.PERMISSION_GRANTED -> 1
            else -> 0
        }

        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            ) {
                android.content.pm.PackageManager.PERMISSION_GRANTED -> 1
                else -> 0
            }
        } else
            1

        var change = false
        if(locationPermissionGranted == -1)
            locationPermissionGranted = locationPermission
        else {
            change = (locationPermissionGranted != locationPermission)
            locationPermissionGranted = locationPermission
        }
        if(notificationPermissionGranted == -1)
            notificationPermissionGranted = notificationPermission
        else {
            change = change || (notificationPermissionGranted != notificationPermission)
            notificationPermissionGranted = notificationPermission
        }

        if(change) {
            Log.d(TAG, "Permissions have changed $locationPermission -> $locationPermissionGranted and $notificationPermission -> $notificationPermissionGranted")
            when(locationPermissionGranted) {
                0 -> setServiceState(false)
                1 -> setServiceState(true)
            }
        } else {
            if(soundscapeServiceConnection.soundscapeService?.running == false) {
                // This can happen if the service failed to move to the foreground.
                // Simply start the service now
                Firebase.crashlytics.log("Attempt to start non-running service from onResume")
                setServiceState(true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

//      Enable the following code to generate stack traces when tracking down "A resource failed to
//      call close messages in the log.
//
//        StrictMode.setVmPolicy(
//            StrictMode.VmPolicy.Builder()
//                     .detectLeakedClosableObjects()
//                     .penaltyListener(ContextCompat.getMainExecutor(this)) { violation ->
//                         Log.e("MainActivity", "StrictMode VmPolicy violation", violation)
//                     }
//                     .build()
//        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val timeNow = System.currentTimeMillis()
            installSplashScreen()

            // Keep the splash screen visible to allow time to see the attribution acknowledgements,
            // But not too long as this delay happens coming out of sleep too.
            val attributionDelay = 2000
            val content: View = findViewById(android.R.id.content)
            content.viewTreeObserver.addOnPreDrawListener(
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        return if((System.currentTimeMillis() - timeNow) > attributionDelay) {
                            content.viewTreeObserver.removeOnPreDrawListener(this)
                            true
                        } else {
                            false
                        }
                    }
                }
            )
        }

        super.onCreate(savedInstanceState)

        println("${Build.FINGERPRINT}")
        println("${Build.MODEL}")
        println("${Build.BRAND}")
        println("${Build.PRODUCT}")

        // Delete contents of export directory. This is used for exporting routes and markers, and
        // log data when using Contact Support. We don't want these accumulating over separate runs
        // of the app. They do have to be left around at least until they have been used by the
        // email client/WhatsApp etc.
        val directory = File("$filesDir/export/")
        if (directory.exists()) {
            directory.deleteRecursively()
        }

        // Debug - dump preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        for (pref in sharedPreferences.all) {
            Log.d(TAG, "Preference: " + pref.key + " = " + pref.value)
        }
        sharedPreferencesListener =
            SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
                handlePreferenceChange(key, preferences)
            }
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
        // Get starting values
        handlePreferenceChange(THEME_LIGHTNESS_KEY, sharedPreferences)
        handlePreferenceChange(THEME_CONTRAST_KEY, sharedPreferences)

        // Validate offline map directory
        getOfflineMapStorage(this)

        // Unpack map assets
        processMaps(applicationContext)

        // When opening a JSON file containing a route from Android File we can end up with two
        // instances of the app running. This check ensures that we have only one instance.
        if (!isTaskRoot) {
            val newIntent = Intent(intent)
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(newIntent)
            finish()
        }

        val isFirstLaunch = sharedPreferences.getBoolean(FIRST_LAUNCH_KEY, true)
        Log.d(TAG, "isFirstLaunch: $isFirstLaunch")

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
            SoundscapeTheme(themeStateFlow = themeStateFlow) {
                val navController = rememberNavController()
                val destination by navigator.destination.collectAsState()
                LaunchedEffect(destination) {
                    if(destination != "") {
                        if (navController.currentDestination?.route != destination) {
                            navController.navigate(destination)
                            // Reset the destination state ready for another
                            navigator.destination.value = ""
                        }
                    }
                }
                HomeScreen(
                    navController = navController,
                    preferences = sharedPreferences,
                    audioTour = audioTour,
                    rateSoundscape = {
                        this.rateSoundscape()
                    },
                    contactSupport = {
                        val thisActivity = this
                        lifecycleScope.launch {
                            thisActivity.contactSupport()
                        }
                    },
                    permissionsRequired = remember { locationPermissionGranted != 1}
                )
            }
        }
    }

    override fun onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
        super.onDestroy()
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

    fun tableRow(key: String, value: String): String {
        return "$key:\t\t$value<br/>"
    }

    fun talkBackDescription(builder: StringBuilder, context: Context) {
        val am = context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (!am.isEnabled) {
            builder.append(tableRow("Talkback", "Off"))
            return
        }

        builder.append(tableRow("TouchExploration Enabled", am.isTouchExplorationEnabled.toString()))

        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)
        for (serviceInfo in enabledServices) {
            builder.append(tableRow("AccessibilityService:", serviceInfo.id))
        }
    }

    suspend fun contactSupport() {
        // Get information from the phone that we'd like to pass on to support
        val appVersion = BuildConfig.VERSION_NAME
        val androidVersion = Build.VERSION.RELEASE
        val model = Build.MODEL
        val brand = Build.BRAND
        val product = Build.PRODUCT
        val manufacturer = Build.MANUFACTURER
        val language = Locale.getDefault().language + "-" + Locale.getDefault().country
        val subjectText =
            "Soundscape Feedback (Android $androidVersion, $brand $model, $language, $appVersion)"
        val preferences = sharedPreferences.all

        val bodyText = StringBuilder()

        bodyText.append("-----------------------------<br/>")
        bodyText.append(tableRow("Product", product))
        bodyText.append(tableRow("Manufacturer", manufacturer))
        talkBackDescription(bodyText, applicationContext)


        bodyText.append(tableRow("AndroidGeocoder", AndroidGeocoder.enabled.toString()))

        preferences.forEach { pref -> bodyText.append(tableRow(pref.key, pref.value.toString())) }
        bodyText.append("-----------------------------<br/><br/>")

        bodyText.append("Untranslated OSM keys:<br/>")
        val unknownOsmKeys = ResourceMapper.getUnfoundKeys()
        unknownOsmKeys.forEach { bodyText.append("\t$it<br/>") }
        bodyText.append("-----------------------------<br/><br/>")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("soundscapeAndroid@scottishtecharmy.support"))
            putExtra(Intent.EXTRA_SUBJECT, subjectText)
            putExtra(Intent.EXTRA_TEXT, Html.fromHtml(bodyText.toString(), 0))
        }

        // Attach the log file if it was created successfully
        val logPath = LogcatHelper.saveLogcatToFile(this)

        if (logPath != null) {
            val logFile = File(logPath)
            if (logFile.exists()) {
                Log.d(TAG, "Attach log file from $logPath with authority ${packageName}.provider")
                // Get a content URI for the file using FileProvider
                val logUri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "${packageName}.provider",
                    logFile
                )
                intent.putExtra(Intent.EXTRA_STREAM, logUri)
                // Grant read permission to the receiving app
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        Log.e(TAG, Html.fromHtml(bodyText.toString(), 0).toString())
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            val alternativeIntent = Intent.createChooser(intent, "")
            startActivity(alternativeIntent)
        }
    }

    fun shareRecording() {
        val shareUri = soundscapeServiceConnection.soundscapeService?.getRecordingShareUri(applicationContext)
        if(shareUri != null) {
            val sendIntent: Intent =
                Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TITLE, "Recorded GPX")
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    type = "text/plain"
                    flags += Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }
    }

    fun shareRoute(shareUri: Uri?) {
        if(shareUri != null) {
            val sendIntent: Intent =
                Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TITLE, "Shared route")
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    type = "text/plain"
                    flags += Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
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
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                )
            }
        }
    }

    var serviceSleeping = false
    fun setServiceState(newServiceState: Boolean, sleeping: Boolean? = null) {
        Log.d(TAG, "setServiceState $newServiceState, sleeping = $sleeping, serviceSleeping = $serviceSleeping")
        if(!serviceSleeping || (sleeping == false)) {
            if (!newServiceState) {
                soundscapeServiceConnection.stopService()
            } else {
                checkAndRequestNotificationPermissions()
                soundscapeServiceConnection.tryToBindToServiceIfRunning(applicationContext)
            }
        }
        if(sleeping != null) {
            serviceSleeping = sleeping
        }
    }

    /**
     * Creates and starts the Soundscape foreground service.
     *
     * It also tries to bind to the service to update the UI with location updates.
     */
    private fun startSoundscapeService() {
        Log.e(TAG, "startSoundscapeService")
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
        const val SPEECH_ENGINE_DEFAULT = ""
        const val SPEECH_ENGINE_KEY = "SpeechEngine"
        const val VOICE_TYPE_DEFAULT = "Default"
        const val VOICE_TYPE_KEY = "VoiceType"
        const val SPEECH_RATE_DEFAULT = 1.0f
        const val SPEECH_RATE_KEY = "SpeechRate"
        const val HINTS_DEFAULT = true
        const val HINTS_KEY = "Hints"
        const val THEME_LIGHTNESS_DEFAULT = "Auto"
        const val THEME_LIGHTNESS_KEY = "ThemeLightness"
        const val THEME_CONTRAST_DEFAULT = "High"
        const val THEME_CONTRAST_KEY = "ThemeContrast"
        const val RECORD_TRAVEL_DEFAULT = false
        const val RECORD_TRAVEL_KEY = "RecordTravel"
        const val SHOW_MAP_DEFAULT = true
        const val SHOW_MAP_KEY = "ShowMap"
        const val ACCESSIBLE_MAP_DEFAULT = true
        const val ACCESSIBLE_MAP_KEY = "AccessibleMap"
        const val MEASUREMENT_UNITS_DEFAULT = "Auto"
        const val MEASUREMENT_UNITS_KEY = "MeasurementUnits"
        const val SEARCH_LANGUAGE_DEFAULT = "auto"
        const val SEARCH_LANGUAGE_KEY = "SearchLanguage"
        const val SELECTED_STORAGE_DEFAULT = ""
        const val SELECTED_STORAGE_KEY = "SelectedStorage"
        const val LAST_NEW_RELEASE_DEFAULT = ""
        const val LAST_NEW_RELEASE_KEY = "LastNewRelease"
        const val GEOCODER_MODE_DEFAULT = "Auto"
        const val GEOCODER_MODE_KEY = "GeocoderMode"

        const val FIRST_LAUNCH_KEY = "FirstLaunch"
        const val AUDIO_TOUR_SHOWN_KEY = "AudioTourShown"
    }
}