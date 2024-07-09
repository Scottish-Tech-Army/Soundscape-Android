package com.kersnazzle.soundscapealpha.activityrecognition

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

    fun startVehicleActivityTracking(){

        pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ActivityTransitionReceiver::class.java),
            PendingIntent.FLAG_MUTABLE)
        val request = ActivityTransitionRequest(getTransitions())

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
}