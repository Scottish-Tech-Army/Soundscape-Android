package com.kersnazzle.soundscapealpha.activityrecognition

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
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// This is from the documentation here:
// https://developer.android.com/develop/sensors-and-location/location/transitions
// https://developers.google.com/android/reference/com/google/android/gms/location/ActivityRecognitionClient
/**
* Class to detect if a device is entering/exiting a vehicle. We need this information
 * so we can increase/decrease the frequency of GeoJSON tile requests. The tile requests will need
 * to be more frequent if the device is moving quickly.
*/
class ActivityTransition(val context: Context) {

    private val _transition = MutableStateFlow("UNKNOWN")
    val transition: StateFlow<String> = _transition

    private var pendingIntent: PendingIntent? = null

    @SuppressWarnings("MissingPermission")
    fun startVehicleActivityTracking(
        onSuccess: () -> Unit = { },
        onFailure: (String) -> Unit = { }
    ){

        pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ActivityTransitionReceiver::class.java),
            // not sure if this should be FLAG_MUTABLE or FLAG_IMMUTABLE
            PendingIntent.FLAG_MUTABLE
        )
        val request = ActivityTransitionRequest(getTransitions())

        if (permissionGranted()) {
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
    fun stopVehicleActivityTracking(){
        if (permissionGranted()) {
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

    private fun permissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // TODO Remove the other DetectedActivity once I've figured out how to do this as I'm only
    //  interested in the DetectedActivity.IN_VEHICLE
    //  and the two ActivityTransitions: ACTIVITY_TRANSITION_ENTER and ACTIVITY_TRANSITION_EXIT
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
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
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
            .setActivityType(DetectedActivity.RUNNING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.RUNNING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build(),
    )
    companion object {
        private const val TAG = "ActivityTransition"
    }
}

