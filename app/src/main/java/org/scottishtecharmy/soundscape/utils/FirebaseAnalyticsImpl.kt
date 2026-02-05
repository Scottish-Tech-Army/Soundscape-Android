package org.scottishtecharmy.soundscape.utils

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import javax.inject.Inject

class FirebaseAnalyticsImpl @Inject constructor() : Analytics {
    override fun logEvent(name: String, params: Bundle?) {
        Firebase.analytics.logEvent(name, params)
    }
    override fun logCostlyEvent(name: String, params: Bundle?) {
        // We're going to drop events that are considered costly for now, purely to see how this
        // affects our monthly event count.
    }
}