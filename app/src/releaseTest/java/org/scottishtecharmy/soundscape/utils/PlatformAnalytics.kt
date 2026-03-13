package org.scottishtecharmy.soundscape.utils

import android.content.Context

fun createPlatformAnalytics(@Suppress("UNUSED_PARAMETER") context: Context): Analytics = NoOpAnalytics()
