package org.scottishtecharmy.soundscape.feedback

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface FeedbackPrompter {
    val pendingQuestion: StateFlow<FeedbackQuestion?>

    fun onRouteCompleted(routeName: String)
    fun submitResponse(question: FeedbackQuestion, choice: String, freeText: String?)
    fun dismissCurrent()

    companion object {
        @Volatile
        private var INSTANCE: FeedbackPrompter? = null

        fun getInstance(dummy: Boolean? = null, context: Context? = null): FeedbackPrompter {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = if (dummy == false)
                        createPlatformFeedbackPrompter(context!!)
                    else
                        NoOpFeedbackPrompter()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}
