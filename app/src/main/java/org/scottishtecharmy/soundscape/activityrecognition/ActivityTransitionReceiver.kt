package org.scottishtecharmy.soundscape.activityrecognition

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import org.scottishtecharmy.soundscape.services.getOttoBus

/**
 * There are several limitations on receiving activity transitions. The Intent passed in has to be
 * mutable because it's altered by the Activity Recognition code to add in the transition events.
 * However, to be mutable means that it has to be an explicit Intent which in turn means that it
 * must be registered in the Android manifest and can't be registered dynamically using
 * registerReceiver. In turn that means that ActivityTransitionReceiver can't take any extra
 * arguments, so to get data out of here we use a singleton Otto bus.
 */
class ActivityTransitionReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let { atIntent ->
            if (ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(atIntent)
                if (result != null && context != null) {
                    processTransitionResults(result.transitionEvents)
                }
            }
        }
    }

    private fun processTransitionResults(transitionEvents: List<ActivityTransitionEvent>) {
        for (event in transitionEvents) {
            getOttoBus().post(event)
        }
    }
}