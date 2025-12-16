package org.scottishtecharmy.soundscape.utils

import android.os.Bundle
import javax.inject.Inject

class NoOpAnalytics @Inject constructor() : Analytics {
    override fun logEvent(name: String, params: Bundle?) {
        // Do nothing
    }
}