package org.scottishtecharmy.soundscape.utils
import android.os.Bundle

interface Analytics {
    fun logEvent(name: String, params: Bundle? = null)
    fun logCostlyEvent(name: String, params: Bundle? = null)

    companion object {
        @Volatile
        private var INSTANCE: Analytics? = null
        fun getInstance(dummy: Boolean? = null) : Analytics {
            synchronized(this) {
                var instance = INSTANCE

                // Check that we've initialized the instance
                if(dummy == null) assert(instance != null)

                if (instance == null) {
                    instance = if(dummy == false)
                        FirebaseAnalyticsImpl()
                    else
                        NoOpAnalytics()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}