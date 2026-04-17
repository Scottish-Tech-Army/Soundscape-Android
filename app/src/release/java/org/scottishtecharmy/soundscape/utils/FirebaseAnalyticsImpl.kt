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

    private fun Map<String, Any?>?.toBundle(): Bundle? {
        if (this == null) return null
        return Bundle().apply {
            for ((key, value) in this@toBundle) {
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
    }

    override fun logEvent(name: String, params: Map<String, Any?>?) {
        Firebase.analytics.logEvent(name, params.toBundle())
    }
    override fun logCostlyEvent(name: String, params: Map<String, Any?>?) {
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
