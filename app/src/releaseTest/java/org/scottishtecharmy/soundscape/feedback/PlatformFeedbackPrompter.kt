package org.scottishtecharmy.soundscape.feedback

import android.content.Context

fun createPlatformFeedbackPrompter(@Suppress("UNUSED_PARAMETER") context: Context): FeedbackPrompter =
    NoOpFeedbackPrompter()
