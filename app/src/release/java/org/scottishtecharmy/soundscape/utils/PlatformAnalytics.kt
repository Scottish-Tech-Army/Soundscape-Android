package org.scottishtecharmy.soundscape.utils

import android.content.Context

fun createPlatformAnalytics(context: Context): Analytics = FirebaseAnalyticsImpl(context)
