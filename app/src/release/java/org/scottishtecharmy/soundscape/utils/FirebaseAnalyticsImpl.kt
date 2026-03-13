package org.scottishtecharmy.soundscape.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics
import javax.inject.Inject

class FirebaseAnalyticsImpl @Inject constructor(context: Context) : Analytics {
    init {
        // We disabled auto initialization, so initialize now
        FirebaseApp.initializeApp(context)
    }
    override fun logEvent(name: String, params: Bundle?) {
        Firebase.analytics.logEvent(name, params)
    }
    override fun logCostlyEvent(name: String, params: Bundle?) {
        // We're going to drop events that are considered costly for now, purely to see how this
        // affects our monthly event count.
    }

    override fun crashSetCustomKey(key: String, value: String) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }
    override fun crashLogNotes(name: String) {
        Firebase.crashlytics.log(name)
    }

}