package org.scottishtecharmy.soundscape.feedback

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.scottishtecharmy.soundscape.BuildConfig
import org.scottishtecharmy.soundscape.utils.Analytics

class RealFeedbackPrompter @Inject constructor(context: Context) : FeedbackPrompter {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _pendingQuestion = MutableStateFlow<FeedbackQuestion?>(null)
    override val pendingQuestion: StateFlow<FeedbackQuestion?> = _pendingQuestion

    override fun onRouteCompleted(routeName: String) {
        if (!frequencyCapAllowsPrompt()) return
        if (_pendingQuestion.value != null) return  // already showing one

        val question = pickQuestion() ?: return
        prefs.edit().putLong(KEY_LAST_PROMPTED_AT, System.currentTimeMillis()).apply()
        Analytics.getInstance().logEvent("feedback_prompt_shown", null)
        _pendingQuestion.value = question
    }

    override fun submitResponse(question: FeedbackQuestion, choice: String, freeText: String?) {
        Analytics.getInstance().logEvent(
            if (choice == CHOICE_SKIPPED) "feedback_skipped" else "feedback_submitted",
            null
        )
        post(question, choice, freeText)
        _pendingQuestion.value = null
    }

    override fun dismissCurrent() {
        val question = _pendingQuestion.value ?: return
        submitResponse(question, CHOICE_SKIPPED, null)
    }

    private fun frequencyCapAllowsPrompt(): Boolean {
        val last = prefs.getLong(KEY_LAST_PROMPTED_AT, 0L)
        if (last == 0L) return true
        return System.currentTimeMillis() - last >= FREQUENCY_CAP_MS
    }

    private fun pickQuestion(): FeedbackQuestion? {
        val pool = QUESTION_POOL
        if (pool.isEmpty()) return null
        val (id, resId) = pool.random()
        return FeedbackQuestion(id = id, text = appContext.getString(resId))
    }

    private fun post(question: FeedbackQuestion, choice: String, freeText: String?) {
        if (BuildConfig.FEEDBACK_PROVIDER_URL.isEmpty()) {
            Log.d(TAG, "FEEDBACK_PROVIDER_URL empty — skipping POST")
            return
        }
        val payload = FeedbackPayload(
            questionId = question.id,
            response = choice,
            freeText = freeText,
            appVersion = BuildConfig.VERSION_NAME,
            locale = Locale.getDefault().toLanguageTag(),
            timestamp = isoTimestamp()
        )
        scope.launch {
            try {
                val response = FeedbackApi.getInstance().submit(payload).execute()
                if (!response.isSuccessful) {
                    Log.w(TAG, "POST failed: HTTP ${response.code()}")
                }
            } catch (t: Throwable) {
                Log.w(TAG, "POST threw", t)
            }
        }
    }

    private fun isoTimestamp(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }

    companion object {
        private const val TAG = "FeedbackPrompter"
        private const val PREFS_NAME = "feedback_prompter"
        private const val KEY_LAST_PROMPTED_AT = "last_prompted_at"
        private const val FREQUENCY_CAP_MS = 7L * 24 * 60 * 60 * 1000

        const val CHOICE_SKIPPED = "skipped"

        // Stable IDs sent over the wire — never change without coordinating with SA-315.
        private val QUESTION_POOL = listOf(
            "route_followability_v1" to org.scottishtecharmy.soundscape.R.string.feedback_question_route_followability,
            "audio_balance_v1" to org.scottishtecharmy.soundscape.R.string.feedback_question_audio_balance,
            "confusion_v1" to org.scottishtecharmy.soundscape.R.string.feedback_question_confusion,
        )
    }
}
