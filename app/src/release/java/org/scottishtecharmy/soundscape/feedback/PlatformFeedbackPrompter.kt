package org.scottishtecharmy.soundscape.feedback

import android.content.Context

fun createPlatformFeedbackPrompter(context: Context): FeedbackPrompter =
    RealFeedbackPrompter(context)
