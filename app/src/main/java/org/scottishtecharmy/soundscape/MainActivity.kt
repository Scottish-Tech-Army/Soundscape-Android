package org.scottishtecharmy.soundscape

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.provider.Settings
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
import androidx.core.content.edit
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.audio.AudioTour
import org.scottishtecharmy.soundscape.geoengine.utils.ResourceMapper
import org.scottishtecharmy.soundscape.geoengine.utils.geocoders.AndroidGeocoder
import org.scottishtecharmy.soundscape.screens.home.HomeRoutes
import org.scottishtecharmy.soundscape.screens.home.HomeScreen
import org.scottishtecharmy.soundscape.screens.home.Navigator
import org.scottishtecharmy.soundscape.services.SoundscapeService
import org.scottishtecharmy.soundscape.ui.theme.SoundscapeTheme
import org.scottishtecharmy.soundscape.utils.Analytics
import org.scottishtecharmy.soundscape.utils.LogcatHelper
import org.scottishtecharmy.soundscape.database.local.model.RouteEntity
import org.scottishtecharmy.soundscape.utils.findExtracts
import org.scottishtecharmy.soundscape.utils.getOfflineMapStorage
import org.scottishtecharmy.soundscape.utils.processMaps
import java.io.File
import java.util.Locale
import javax.inject.Inject

data class ThemeState(
    val hintsEnabled: Boolean = false,
    val themeIsLight: Boolean = true,
    val themeContrast: String = "High")

fun hasPlayServices(context: Context): Boolean {
    return try {
        val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context)
        availability == com.google.android.gms.common.ConnectionResult.SUCCESS
    } catch (e: Exception) {
        // GMS classes not available at all (e.g., device without Play Services)
        false
    }
}

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
                Analytics.getInstance().crashLogNotes("Attempt to start non-running service from onResume")
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

        // Use dummy analytics if any of the following is true:
        //
        //  1. DUMMY_ANALYTICS is set meaning that we're not a release build
        //  2. We don't have Google Play Services
        //  3. We're running in Test Lab which is what happens when Google tests app releases. The
        //    test for this is mentioned here:
        //    https://firebase.google.com/docs/test-lab/android/android-studio#modify_instrumented_test_behavior_for
        //
        val testLabSetting: String? =
            Settings.System.getString(contentResolver, "firebase.test.lab")
        Analytics.getInstance(
            BuildConfig.DUMMY_ANALYTICS ||
                    !hasPlayServices(this) ||
                    "true" == testLabSetting,
            context = applicationContext
        )


        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isFirstLaunch = sharedPreferences.getBoolean(FIRST_LAUNCH_KEY, true)

        // The splash sound is quite invasive. As a result, we want to limit how often we play it.
        // This code means that it will be played the first time any new release is installed.
        val splashPlayed = (sharedPreferences.getString(LAST_SPLASH_RELEASE_KEY, LAST_SPLASH_RELEASE_DEFAULT)
                == BuildConfig.VERSION_NAME.substringBeforeLast("."))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val timeNow = System.currentTimeMillis()
            installSplashScreen()

            var splashSoundFinished = false
            if (splashPlayed) {
                splashSoundFinished = true
            } else {
                // We have a splash sound, so play it and keep the splash screen visible until the
                // playback has finished
                val splashPlayer = android.media.MediaPlayer()
                try {
                    val afd = assets.openFd("DoubleTap/dt_soundscape.mp3")
                    splashPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    splashPlayer.prepare()
                    splashPlayer.setVolume(0.7f, 0.7f)
                    splashPlayer.start()
                    splashPlayer.setOnCompletionListener {
                        it.release()
                        splashSoundFinished = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to play splash sound: $e")
                    splashPlayer.release()
                    splashSoundFinished = true
                }
                sharedPreferences.edit(commit = true) {
                    putString(LAST_SPLASH_RELEASE_KEY, BuildConfig.VERSION_NAME.substringBeforeLast("."))
                }
            }

            // Keep the splash screen visible until the sound has finished playing,
            // with a minimum delay for attribution acknowledgements.
            val attributionDelay = 1500
            val content: View = findViewById(android.R.id.content)
            val context = this
            content.viewTreeObserver.addOnPreDrawListener(
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        val minDelayPassed =
                            (System.currentTimeMillis() - timeNow) > attributionDelay
                        return if (minDelayPassed && splashSoundFinished) {
                            content.viewTreeObserver.removeOnPreDrawListener(this)
                            if (isFirstLaunch) {
                                // On the first launch, we want to take the user through the OnboardingActivity so
                                // switch to it immediately.
                                val intent = Intent(context, OnboardingActivity::class.java)
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                startActivity(intent)
                                finish()
                            }
                            else
                                onSplashComplete()
                            true
                        } else {
                            false
                        }
                    }
                }
            )
        }

        // The remaining code in this function can be run whilst the splash screen is visible.
        // We delay starting the service until the splash screen is gone so that we don't have a
        // clash of audio with the splash screen sound.
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
        // We've deprecated "Online" as a mode. The only options are "Auto" and "Offline". Online
        // and Auto worked the same anyway...
        if (sharedPreferences.getString(GEOCODER_MODE_KEY, GEOCODER_MODE_DEFAULT) == "Online") {
            sharedPreferences.edit { putString(GEOCODER_MODE_KEY, GEOCODER_MODE_DEFAULT) }
        }

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
    }

    private fun onSplashComplete() {
        checkAndRequestNotificationPermissions()
        soundscapeServiceConnection.tryToBindToServiceIfRunning(applicationContext)

        val db = org.scottishtecharmy.soundscape.database.local.MarkersAndRoutesDatabase.getMarkersInstance(applicationContext)
        lifecycleScope.launch {
            db.routeDao().getAllRoutesFlow().collect { routes ->
                updateRouteShortcuts(routes)
            }
        }

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

    private fun updateRouteShortcuts(routes: List<RouteEntity>) {
        val currentIds = routes.map { "route_${it.routeId}" }.toSet()

        // Remove shortcuts for deleted routes
        val toRemove = ShortcutManagerCompat.getDynamicShortcuts(applicationContext)
            .map { it.id }
            .filter { it.startsWith("route_") && it !in currentIds }
        if (toRemove.isNotEmpty()) {
            ShortcutManagerCompat.removeDynamicShortcuts(applicationContext, toRemove)
        }

        // Add/update shortcuts for current routes
        for (route in routes) {
            val intent = Intent(this, MainActivity::class.java)
            intent.action = Intent.ACTION_VIEW
            intent.data = "soundscape://route/${route.name}".toUri()

            val shortcut = ShortcutInfoCompat.Builder(applicationContext, "route_${route.routeId}")
                .setShortLabel(route.name)
                .setLongLabel(route.name)
                .addCapabilityBinding(
                    "actions.intent.START_EXERCISE",
                    "exercise.name",
                    listOf(route.name)
                )
                .setIntent(intent)
                .build()

            ShortcutManagerCompat.pushDynamicShortcut(applicationContext, shortcut)
        }
    }

    private fun rateSoundscape() {
        if (!hasPlayServices(this)) {
            // No Play Services - open Play Store directly as fallback
            openPlayStoreListing()
            return
        }

        try {
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
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error with in-app review, falling back to Play Store", e)
            openPlayStoreListing()
        }
    }

    private fun openPlayStoreListing() {
        try {
            // Try to open in Play Store app
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
        } catch (e: Exception) {
            // Fall back to browser
            startActivity(Intent(Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$packageName".toUri()))
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
        bodyText.append(tableRow("Summary", subjectText))
        bodyText.append(tableRow("Product", product))
        bodyText.append(tableRow("Manufacturer", manufacturer))
        talkBackDescription(bodyText, applicationContext)

        bodyText.append(tableRow("AndroidGeocoder", AndroidGeocoder.enabled.toString()))

        val extractPath = sharedPreferences.getString(SELECTED_STORAGE_KEY, SELECTED_STORAGE_DEFAULT)!!
        val extractCollection = findExtracts(File(extractPath, Environment.DIRECTORY_DOWNLOADS).path)
        if(extractCollection != null) {
            for (extract in extractCollection.features) {
                bodyText.append(tableRow
                    (
                    "Offline extract",
                    "${extract.properties?.get("name")}, ${extract.properties?.get("filename")}"
                    )
                )
            }
        }
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
        const val LANGUAGE_SUPPORTED_PROMPTED_DEFAULT = false
        const val LANGUAGE_SUPPORTED_PROMPTED_KEY = "LanguageSupported"
        const val GEOCODER_MODE_DEFAULT = "Auto"
        const val GEOCODER_MODE_KEY = "GeocoderMode"
        const val LAST_SPLASH_RELEASE_DEFAULT = ""
        const val LAST_SPLASH_RELEASE_KEY = "LastNewRelease"

        const val FIRST_LAUNCH_KEY = "FirstLaunch"
        const val AUDIO_TOUR_SHOWN_KEY = "AudioTourShown"
    }
}