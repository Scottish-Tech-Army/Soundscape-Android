package org.scottishtecharmy.soundscape.feedback

import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NoOpFeedbackPrompter @Inject constructor() : FeedbackPrompter {
    private val _pendingQuestion = MutableStateFlow<FeedbackQuestion?>(null)
    override val pendingQuestion: StateFlow<FeedbackQuestion?> = _pendingQuestion

    override fun onRouteCompleted(routeName: String) {
        // Do nothing
    }

    override fun submitResponse(question: FeedbackQuestion, choice: String, freeText: String?) {
        // Do nothing
    }

    override fun dismissCurrent() {
        // Do nothing
    }
}
