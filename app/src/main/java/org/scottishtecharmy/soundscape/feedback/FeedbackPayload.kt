package org.scottishtecharmy.soundscape.feedback

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeedbackPayload(
    val questionId: String,
    val response: String,
    val freeText: String?,
    val appVersion: String,
    val locale: String,
    val timestamp: String
)
