package org.scottishtecharmy.soundscape.activityrecognition

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.squareup.otto.Subscribe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.services.getOttoBus


// This is from the documentation here:
// https://developer.android.com/develop/sensors-and-location/location/transitions
// https://developers.google.com/android/reference/com/google/android/gms/location/ActivityRecognitionClient
/**
 * Class to detect if a device is entering/exiting a vehicle, or stopping moving. This is used to
 * adjust the contents of call outs and when they are made.
 */
class ActivityTransition {

    private val _transition = MutableStateFlow(ActivityTransitionEvent(DetectedActivity.UNKNOWN,
        ActivityTransition.ACTIVITY_TRANSITION_ENTER,
        0))
    val transition: StateFlow<ActivityTransitionEvent> = _transition

    private var pendingIntent: PendingIntent? = null

    /**
     * onActivityTransitionEvent is where ActivityTransitionEvent percolate up to after they have
     * been received by the ActivityTransitionReceiver.
     */
    @Subscribe
    fun onActivityTransitionEvent(event: ActivityTransitionEvent) {
        val uiTransition = mapActivityToString(event)
        val transition = "${mapTransitionToString(event)} $uiTransition"

        Log.e(TAG, "onActivityTransitionEvent: $transition")

        _transition.value = event
    }

    @SuppressWarnings("MissingPermission")
    fun startVehicleActivityTracking(
        context: Context,
        onSuccess: () -> Unit = { },
        onFailure: (String) -> Unit = { }
    ){
        // We've got an @Subscribe function, so we need to register our class
        getOttoBus().register(this)

        Log.d(TAG,"startVehicleTracking function")
        pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ActivityTransitionReceiver::class.java),
            // This has to be FLAG_MUTABLE as it is extended by the code that sends it to the
            // receiver to add in the activity transition details.
            PendingIntent.FLAG_MUTABLE
        )
        val request = ActivityTransitionRequest(getTransitions())
        Log.d(TAG,"startVehicleTracking function - permission check")
        if (permissionGranted(context)) {
            pendingIntent?.let { trackingIntent ->
                ActivityRecognition.getClient(context)
                    .requestActivityTransitionUpdates(request, trackingIntent)
                    .addOnSuccessListener {
                        Log.d(TAG,"Activity recognition started")
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        Log.d(TAG, "Activity recognition could not be started: ${exception.message}")
                        onFailure("Activity recognition could not be started: ${exception.message}")
                    }
            }
        }


    }

    @SuppressWarnings("MissingPermission")
    fun stopVehicleActivityTracking(context: Context){
        getOttoBus().unregister(this)
        if (permissionGranted(context)) {
            pendingIntent?.let { trackingIntent ->
                ActivityRecognition.getClient(context).removeActivityTransitionUpdates(trackingIntent)
                    .addOnSuccessListener {
                        trackingIntent.cancel()
                        Log.d(TAG, "Activity recognition canceled")
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Activity recognition could not be canceled: ${exception.message}")
                    }
            }
        }

    }

    private fun permissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun getTransitions() = listOf(
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.STILL)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.STILL)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.ON_FOOT)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.ON_FOOT)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.ON_BICYCLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.ON_BICYCLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.RUNNING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.RUNNING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build()
    )
    companion object {
        private const val TAG = "ActivityTransition"
    }
}

fun mapActivityToString(event: ActivityTransitionEvent) =
    when (event.activityType) {
        DetectedActivity.STILL -> "Still"
        DetectedActivity.ON_FOOT -> "OnFoot"
        DetectedActivity.ON_BICYCLE -> "OnBike"
        DetectedActivity.IN_VEHICLE -> "InVehicle"
        DetectedActivity.WALKING -> "Walking"
        DetectedActivity.RUNNING -> "Running"
        else -> "Unknown ${event.activityType}"
    }

fun mapTransitionToString(event: ActivityTransitionEvent) =
    when (event.transitionType) {
        ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "Enter"
        ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "Exit"
        else -> "Unknown"
    }

