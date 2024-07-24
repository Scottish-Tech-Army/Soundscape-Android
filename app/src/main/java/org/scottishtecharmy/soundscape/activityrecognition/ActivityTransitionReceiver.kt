package org.scottishtecharmy.soundscape.activityrecognition

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import org.scottishtecharmy.soundscape.services.SoundscapeService

class ActivityTransitionReceiver: BroadcastReceiver() {

    private lateinit var actionTransition: ActivityTransition
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive")
        intent?.let { atIntent ->
            if (ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(atIntent)
                if (result != null && context != null) {
                    processTransitionResults(context, result.transitionEvents)
                }
            }
        }
    }

    private fun processTransitionResults(context: Context, transitionEvents: List<ActivityTransitionEvent>) {
        //if driving, start location service
        for (event in transitionEvents) {
            val uiTransition = mapActivityToString(event)
            val transition = "${mapTransitionToString(event)} $uiTransition"
            Log.d(TAG, "Process Transition Results: $transition")
            //actionTransition.onDetectedTransitionEvent(uiTransition)

            /*if (event.activityType == DetectedActivity.IN_VEHICLE) {
                val intent = Intent(context, SoundscapeService::class.java)
                when (event.transitionType) {
                    ActivityTransition.ACTIVITY_TRANSITION_ENTER -> {
                        intent.putExtra("IN_VEHICLE", true)
                    }
                    ActivityTransition.ACTIVITY_TRANSITION_EXIT -> {
                        intent.putExtra("IN_VEHICLE", false)
                    }
                }

            }*/
        }
    }

    private fun mapActivityToString(event: ActivityTransitionEvent) =
        when (event.activityType) {
            DetectedActivity.STILL -> "STILL"
            DetectedActivity.ON_FOOT -> "STAND"
            DetectedActivity.WALKING -> "WALK"
            DetectedActivity.RUNNING -> "RUN"
            DetectedActivity.ON_BICYCLE -> "BIKE"
            DetectedActivity.IN_VEHICLE -> "DRIVE"
            else -> "UNKNOWN"
        }

    private fun mapTransitionToString(event: ActivityTransitionEvent) =
        when (event.transitionType) {
            ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
            else -> "UNKNOWN"
        }

    companion object {
        private const val TAG = "ActivityTransitionReceiver"
    }
}