package org.scottishtecharmy.soundscape.utils
import android.content.Context

object AnalyticsProvider {
    @Volatile
    private var INSTANCE: Analytics? = null
    fun getInstance(dummy: Boolean? = null, context: Context? = null) : Analytics {
        synchronized(this) {
            var instance = INSTANCE
            if (instance == null) {
                instance = if(dummy == false)
                    createPlatformAnalytics(context!!)
                else
                    NoOpAnalytics()
                INSTANCE = instance
            }
            return instance
        }
    }
}
