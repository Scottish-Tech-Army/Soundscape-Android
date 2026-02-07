package org.scottishtecharmy.soundscape.utils
import android.os.Bundle

interface Analytics {
    fun logEvent(name: String, params: Bundle? = null)
    fun logCostlyEvent(name: String, params: Bundle? = null)

    fun crashSetCustomKey(key: String, value: String)
    fun crashLogNotes(name: String)

    companion object {
        @Volatile
        private var INSTANCE: Analytics? = null
        fun getInstance(dummy: Boolean? = null) : Analytics {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = if(dummy == false)
                        createPlatformAnalytics()
                    else
                        NoOpAnalytics()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}